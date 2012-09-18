package objectecho;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

public class Connection extends SimpleChannelUpstreamHandler implements Runnable {
	boolean dirty;

	private final Endpoint otherEndpoint;

	private final ConnectionManager connectionManager;
	private Channel channel;
	private ClientBootstrap bootstrap;

	private final Queue<Object> outgoing;

	private boolean closed;

	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());

	public Connection(ConnectionManager connectionManager, Endpoint e) {
		this.connectionManager = connectionManager;
		this.otherEndpoint = e;
		this.channel = null;
		this.outgoing = new LinkedList<Object>();
		this.closed = false;
	}

	@Override
	public void run() {
		// Configure the client.
		this.bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(this.connectionManager.getBossThreadPool(),
						this.connectionManager.getWorkerThreadPool()));

		// Set up the pipeline factory.
		this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
						new ObjectEncoder(),
						new ObjectDecoder(
								ClassResolvers.cacheDisabled(this.getClass().getClassLoader())),
						Connection.this);
			}
		});

		this.bootstrap.setOption("tcpnodelay", true);
		this.bootstrap.setOption("keepAlive", true);

		// Start the connection attempt.
		ChannelFuture future = this.bootstrap.connect(this.otherEndpoint.getInetAddress());

		// Wait until the connection attempt succeeds or fails.
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future2) throws Exception {
				Connection.this.channelConnectedCallback(future2);
			}
		});
	}

	void channelConnectedCallback(ChannelFuture future) {
		assert (this.channel == null);
		if (future.isSuccess()) {
			synchronized (this) {
				this.channel = future.getChannel();
				while (this.outgoing.size() > 0) {
					this.channel.write(this.outgoing.poll());
				}
				if (this.closed) {
					this.cleanup();
				}
			}
		} else {
			synchronized (this) {
				future.getCause().printStackTrace(); // TODO: update me with
														// correct
														// error treatment
				this.cleanup();
			}
		}
	}

	/**
	 * Send message to other endpoint of this connection. Message is sent
	 * assynchronously.
	 * 
	 * @param message
	 */
	public void sendMessage(Object message) {
		synchronized (this) {
			if (this.channel == null) {
				this.outgoing.add(message);
			} else {
				this.channel.write(message);
			}
		}
	}

	/**
	 * Invoked when a message object was received from a remote peer.
	 */
	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		this.connectionManager.getToDeliver().processEvent(e.getMessage());
	}

	@Override
	public void handleUpstream(
			ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) {
			Connection.logger.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	public void close() {
		synchronized (this) {
			if (this.channel != null) {
				this.cleanup();
			}
			this.closed = true;
		}
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		Connection.logger.log(
				Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		synchronized (this) {
			this.cleanup();
		}
	}

	private void cleanup() {
		if (this.channel != null) {
			this.channel.close();
		}
		this.outgoing.clear();
		this.bootstrap.releaseExternalResources();
	}

	public boolean isDirty() {
		return this.dirty;
	}

	public void setDirty() {
		this.dirty = true;
	}

	public void clean() {
		this.dirty = false;
	}
}
