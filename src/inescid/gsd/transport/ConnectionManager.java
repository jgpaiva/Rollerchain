package inescid.gsd.transport;

import inescid.gsd.rollerchain.interfaces.EventReceiver;
import inescid.gsd.utils.Configuration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

public class ConnectionManager {
	private final Map<Endpoint, Connection> connections;
	private final ReentrantReadWriteLock connectionsLock;
	private final int connectionTimeout;
	private final ExecutorService bossThreadPool;
	private final ExecutorService workerThreadPool;
	private final EventReceiver toDeliver;
	private final Endpoint selfEndpoint;
	private final ServerBootstrap serverBootstrap;
	private final Thread gcThread;

	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());

	public ConnectionManager(EventReceiver toDeliver, Endpoint selfEndpoint) {
		this.connections = new TreeMap<Endpoint, Connection>();
		this.connectionsLock = new ReentrantReadWriteLock();
		// TODO: insert myself in connections with dummy connection

		this.connectionTimeout = Configuration.getConnectionTimeout();

		this.bossThreadPool = Executors.newCachedThreadPool();
		this.workerThreadPool = Executors.newCachedThreadPool();

		this.toDeliver = toDeliver;
		this.selfEndpoint = selfEndpoint;

		// Configure the server.
		this.serverBootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		this.serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
						new ObjectEncoder(),
						new ObjectDecoder(
								ClassResolvers.cacheDisabled(this.getClass().getClassLoader())),
						new IncomingChannelHandler(ConnectionManager.this));
			}
		});

		// Bind and start to accept incoming connections.
		Channel retVal = this.serverBootstrap.bind(new InetSocketAddress(selfEndpoint.port));
		ConnectionManager.logger.log(Level.INFO, "starting server at localhost:"
				+ selfEndpoint.port + " channel:" + retVal);

		this.gcThread = new Thread(new Runnable() {
			@Override
			public void run() {
				ConnectionManager.this.checkConnections();
			}
		});
		this.gcThread.start();
	}

	private synchronized void checkConnections() {
		ArrayList<Connection> toRemove = new ArrayList<Connection>();
		try {
			this.connectionsLock.readLock().lock();
			for (Connection it : this.connections.values()) {
				if (it.isDirty()) {
					it.close();
					toRemove.add(it);
				}
			}

			for (Connection it : toRemove) {
				this.removeConnection(it);
			}

			for (Connection it : this.connections.values()) {
				it.setDirty();
			}
		} finally {
			this.connectionsLock.readLock().unlock();
		}

		try {
			Thread.sleep(this.connectionTimeout);
		} catch (InterruptedException e) {
			Thread.dumpStack();
			System.exit(-1);
		}
	}

	public Connection getConnection(Endpoint e) {
		Connection temp;
		try {
			this.connectionsLock.readLock().lock();
			temp = this.connections.get(e);
		} finally {
			this.connectionsLock.readLock().unlock();
		}
		if (temp == null) {
			ConnectionManager.logger.log(Level.INFO, "connection to: " + e
					+ " not found, creating new connection.");
			temp = this.createNewConnection(e);
		}
		temp.clean();

		return temp;
	}

	private Connection createNewConnection(Endpoint e) {
		Connection temp;
		try {
			this.connectionsLock.writeLock().lock();
			temp = this.connections.get(e);
			if (temp == null) {
				temp = new Connection(this, e);
				temp.init();
				this.connections.put(e, temp);
			}
		} finally {
			this.connectionsLock.writeLock().unlock();
		}
		return temp;
	}

	protected Connection createConnection(IncomingChannelHandler incomingChannel) {
		Connection temp;
		try {
			this.connectionsLock.writeLock().lock();
			Endpoint endpoint = incomingChannel.getEndpoint();

			temp = this.connections.get(endpoint);
			if (temp == null) {
				temp = new Connection(this, endpoint, incomingChannel);
				this.connections.put(endpoint, temp);
			} else {
				temp.addIncomingChannel(incomingChannel);
			}
		} finally {
			this.connectionsLock.writeLock().unlock();
		}
		return temp;
	}

	public void removeConnection(Connection it) {
		try {
			this.connectionsLock.writeLock().lock();
			this.connections.remove(it.getOtherEndpoint());
			it.close();
		} finally {
			this.connectionsLock.writeLock().unlock();
		}
	}

	public Executor getBossThreadPool() {
		return this.bossThreadPool;
	}

	public Executor getWorkerThreadPool() {
		return this.workerThreadPool;
	}

	public EventReceiver getToDeliver() {
		return this.toDeliver;
	}

	public Endpoint getSelfEndpoint() {
		return this.selfEndpoint;
	}

	@Override
	public String toString() {
		return "ConnectionManager at " + this.selfEndpoint + " connections: "
				+ this.connections.values();
	}

	public void shutdown() {
		for (Connection it : this.connections.values()) {
			it.close();
		}
		this.serverBootstrap.releaseExternalResources();
		this.connections.clear();
	}
}
