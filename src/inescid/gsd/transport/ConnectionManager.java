package inescid.gsd.transport;

import inescid.gsd.common.EventReceiver;
import inescid.gsd.utils.Configuration;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

public class ConnectionManager {
	// active connections and respective endoints. possibly could be replaced by
	// a channelgroup
	private final Map<Endpoint, Connection> connections;
	// lock for manipulating connections
	private final ReentrantReadWriteLock connectionsLock;
	// time between checking active connections
	private final int connectionTimeout;

	// all channels managed by this connection manager
	private final ChannelGroup allChannels = new DefaultChannelGroup();

	// executors for connections spawned by the server
	private final ExecutorService bossThreadPool;
	private final ExecutorService workerThreadPool;
	// bootstrap for tcp server
	private final ServerBootstrap serverBootstrap;

	// owner of this ConnectionManager. will handled incoming events
	private final EventReceiver toDeliver;
	// endpoint of owner
	private final Endpoint selfEndpoint;

	private boolean running = false;

	// executor service for cleanup tasks
	private final ScheduledExecutorService cleanupThread;

	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());

	/**
	 * builds a new connection manager. will create at most one outgoing TCP
	 * connection per remote endpoint.
	 * 
	 * @param toDeliver
	 * @param selfEndpoint
	 */
	public ConnectionManager(EventReceiver toDeliver, Endpoint selfEndpoint) {
		connections = new TreeMap<Endpoint, Connection>();
		connectionsLock = new ReentrantReadWriteLock();

		connectionTimeout = Configuration.getConnectionTimeout();

		bossThreadPool = Executors.newCachedThreadPool();
		workerThreadPool = Executors.newCachedThreadPool();

		this.toDeliver = toDeliver;
		this.selfEndpoint = selfEndpoint;

		// Configure the server.
		serverBootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
						new ObjectEncoder(),
						new ObjectDecoder(
								ClassResolvers.cacheDisabled(this.getClass().getClassLoader())),
						new IncomingChannelHandler(ConnectionManager.this));
			}
		});
		serverBootstrap.setOption("child.tcpnodelay", true);
		serverBootstrap.setOption("child.keepAlive", true);

		// Bind and start to accept incoming connections.
		Channel retVal = serverBootstrap.bind(new InetSocketAddress(selfEndpoint.port));
		ConnectionManager.logger.log(Level.INFO, "starting server at localhost:"
				+ selfEndpoint.port + " channel:" + retVal);
		allChannels.add(retVal);

		cleanupThread = Executors.newScheduledThreadPool(2);
		cleanupThread.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				ConnectionManager.this.checkConnections();
			}
		}, 0, connectionTimeout, TimeUnit.SECONDS);

		running = true;
	}

	/**
	 * garbage collecting of open connections.
	 */
	private synchronized void checkConnections() {
		try {
			connectionsLock.readLock().lock();
			for (Connection it : connections.values()) {
				if (it.isDirty()) removeConnection(it);
				it.setDirty();
			}
		} finally {
			connectionsLock.readLock().unlock();
		}
	}

	/**
	 * Get a connection to endpoint. Will create a new one if there is none.
	 * 
	 * @param e
	 * @return
	 */
	public Connection getConnection(Endpoint e) {
		Connection temp;
		try {
			connectionsLock.readLock().lock();
			if (!running)
				return null;
			temp = connections.get(e);
		} finally {
			connectionsLock.readLock().unlock();
		}
		if (temp == null) {
			ConnectionManager.logger.log(Level.INFO, "connection to: " + e
					+ " not found, creating new connection.");
			temp = createNewConnection(e);
		}
		temp.clean();

		return temp;
	}

	/**
	 * opens a new outgoing connection to the endpoint.
	 * 
	 * @param e
	 *            endpoint to connect
	 * @return new connection to endpoint
	 */
	private Connection createNewConnection(Endpoint e) {
		Connection temp;
		try {
			connectionsLock.writeLock().lock();
			temp = connections.get(e);
			if (temp == null) {
				temp = new Connection(this, e);
				temp.init(bossThreadPool, workerThreadPool);
				connections.put(e, temp);
			}
		} finally {
			connectionsLock.writeLock().unlock();
		}
		return temp;
	}

	/**
	 * Creates a new connection object using as basis an incomming TCP
	 * connection.
	 * 
	 * @param incomingChannel
	 * @return
	 */
	Connection createConnection(IncomingChannelHandler incomingChannel) {
		Connection temp;
		try {
			connectionsLock.writeLock().lock();
			Endpoint endpoint = incomingChannel.getEndpoint();

			temp = connections.get(endpoint);
			if (temp == null) {
				temp = new Connection(this, endpoint, incomingChannel);
				connections.put(endpoint, temp);
			} else
				temp.addIncomingChannel(incomingChannel);
		} finally {
			connectionsLock.writeLock().unlock();
		}
		return temp;
	}

	/**
	 * Schedule a connection to be destroyed
	 * 
	 * @param it
	 */
	void removeConnection(Connection it) {
		ConnectionManager.logger.log(Level.INFO, selfEndpoint + " requesting remove channel with "
				+ it.otherEndpoint);
		class RemoveConnection implements Runnable {
			private final Connection conn;

			@Override
			public void run() {
				internalRemoveConnection(conn);
			}

			RemoveConnection(Connection conn) {
				this.conn = conn;
			}
		}
		cleanupThread.submit(new RemoveConnection(it));
	}

	/**
	 * Remove and destroy a connection
	 * 
	 * @param it
	 */
	private void internalRemoveConnection(Connection it) {
		try {
			connectionsLock.writeLock().lock();
			if (!running)
				return;
			connections.remove(it.getOtherEndpoint());
			it.close();
		} finally {
			connectionsLock.writeLock().unlock();
		}
	}

	/**
	 * Shutdown this Manager and release all resources. This method may take a
	 * while to finish.
	 */
	public void shutdown() {
		try {
			connectionsLock.writeLock().lock();
			if (!running)
				return;

			ConnectionManager.logger.log(Level.INFO, "Shutting down ConnectionManager");
			for (Connection it : connections.values())
				it.close();
			allChannels.close().awaitUninterruptibly();
			ConnectionManager.logger.log(Level.INFO, "Closed all channels. Releasing resources");
			serverBootstrap.releaseExternalResources();
			ConnectionManager.logger.log(Level.INFO, "Shutting down ThreadPools");
			bossThreadPool.shutdownNow();
			workerThreadPool.shutdownNow();
			cleanupThread.shutdownNow();
			connections.clear();
			ConnectionManager.logger.log(Level.INFO, "Shutdown complete");

			running = false;
		} finally {
			connectionsLock.writeLock().unlock();
		}
	}

	void deliverEvent(Endpoint source, Object event) {
		toDeliver.processEvent(source, event);
	}

	void addChannel(Endpoint otherEndpoint, Channel c) {
		ConnectionManager.logger.log(Level.INFO, selfEndpoint + " estabilished channel with "
				+ otherEndpoint);
		allChannels.add(c);
	}

	public Endpoint getSelfEndpoint() {
		return selfEndpoint;
	}

	@Override
	public String toString() {
		return "ConnectionManager at " + selfEndpoint + " connections: "
				+ connections.values();
	}
}
