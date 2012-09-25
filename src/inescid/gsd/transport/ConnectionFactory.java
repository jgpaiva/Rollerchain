package inescid.gsd.transport;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

public class ConnectionFactory implements ChannelPipelineFactory {
	private final ConnectionManager connectionManager;
	private final Endpoint endpoint;
	private final Connection connection;

	private static final Logger logger = Logger.getLogger(
			ConnectionFactory.class.getName());

	ConnectionFactory(ConnectionManager connectionManager, Endpoint e) {
		this.connectionManager = connectionManager;
		endpoint = e;
		connection = new Connection(this.connectionManager, endpoint);
	}

	@Override
	public synchronized ChannelPipeline getPipeline() throws Exception {
		ConnectionFactory.logger.log(Level.FINE, "created a pipeline for " + connection);
		return Channels.pipeline(
				new ObjectEncoder(),
				new ObjectDecoder(
						ClassResolvers.cacheDisabled(this.getClass().getClassLoader())),
				connection
				);
	}

	public Connection getConnection() {
		return connection;
	}

	public void handleConnectionFuture(ChannelFuture future) {
		future.addListener(new ChannelFutureListener()
		{
			@Override
			public void operationComplete(ChannelFuture future) throws Exception
			{
				connection.channelConnectedCallback(future);
			}
		});
		ConnectionFactory.logger.log(Level.FINE, "registered future with connection:" + connection);
	}

	public static ChannelPipelineFactory getDummyPipeline() {
		return new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ConnectionFactory.logger.log(Level.SEVERE, "this should never happen!");
				throw new RuntimeException("this should never be used!");
			}
		};
	}
}
