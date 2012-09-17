package objectecho;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

public class ConnectionManager implements Runnable {
	private final HashMap<Endpoint, Connection> connections;
	private final Endpoint self;
	private final int connectionTimeout;
	private final ExecutorService bossThreadPool;
	private final ExecutorService workerThreadPool;

	ConnectionManager(Endpoint self) {
		this.connections = new HashMap<Endpoint, Connection>();
		// TODO: insert myself in connections with dummy connection

		new Thread(this).run();
		this.connectionTimeout = Configuration.getConnectionTimeout();

		this.bossThreadPool = Executors.newCachedThreadPool();
		this.workerThreadPool = Executors.newCachedThreadPool();

		this.self = self;
	}

	@Override
	public void run() {
		this.checkConnections();
		try {
			Thread.sleep(this.connectionTimeout);
		} catch (InterruptedException e) {
			Thread.dumpStack();
			System.exit(-1);
		}
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
		// Configure the client.
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(this.bossThreadPool, this.workerThreadPool));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
						new ObjectEncoder(),
						new ObjectDecoder(
								ClassResolvers.cacheDisabled(this.getClass().getClassLoader())),
						new ObjectClientHandler(ObjectEchoClient.this.id));
			}
		});

		// Start the connection attempt.
		bootstrap.connect(new InetSocketAddress(this.host, this.port));

		Connection c = new Connection(this, this.self, e);
	}
}
