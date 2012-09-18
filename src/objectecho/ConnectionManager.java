package objectecho;

import inescid.gsd.rollerchain.interfaces.EventReceiver;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

public class ConnectionManager extends SimpleChannelUpstreamHandler {
	private final HashMap<Endpoint, Connection> connections;
	private final int connectionTimeout;
	private final ExecutorService bossThreadPool;
	private final ExecutorService workerThreadPool;
	private final EventReceiver toDeliver;
	private final Endpoint selfEndpoint;
	private final ServerBootstrap serverBootstrap;
	private final Thread gcThread;

	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());

	ConnectionManager(EventReceiver toDeliver, Endpoint selfEndpoint) {
		this.connections = new HashMap<Endpoint, Connection>();
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
						ConnectionManager.this);
			}
		});

		// Bind and start to accept incoming connections.
		this.serverBootstrap.bind(new InetSocketAddress(selfEndpoint.port));

		this.gcThread = new Thread(new Runnable() {
			@Override
			public void run() {
				ConnectionManager.this.checkConnections();
			}
		});
	}

	@Override
	public void channelConnected(
			ChannelHandlerContext ctx, ChannelStateEvent e) {
		// TODO: create new connection for this channel
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		ConnectionManager.logger.log(
				Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		e.getChannel().close();
	}

	private synchronized void checkConnections() {
		for (Connection it : this.connections.values()) {
			if (it.isDirty()) {
				it.close();
			}
		}
		for (Connection it : this.connections.values()) {
			it.setDirty();
		}

		try {
			Thread.sleep(this.connectionTimeout);
		} catch (InterruptedException e) {
			Thread.dumpStack();
			System.exit(-1);
		}
	}

	public synchronized Connection getConnection(Endpoint e) {
		Connection temp = this.connections.get(e);
		if (temp == null) {
			temp = this.createNewConnection(e);
		}
		temp.clean();

		return temp;
	}

	private Connection createNewConnection(Endpoint e) {
		Connection c = new Connection(this, e);
		return c;
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
}
