package inescid.gsd.transport;

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

public class Connection extends SimpleChannelUpstreamHandler {
	boolean dirty;

	private final Endpoint otherEndpoint;

	private final ConnectionManager connectionManager;
	private Channel channel;
	private ClientBootstrap bootstrap;

	private final Queue<Object> outgoing = new LinkedList<Object>();;

	private boolean closed = false;

	private IncomingChannelHandler incomingChannel;

	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());

	public Connection(ConnectionManager connectionManager, Endpoint endpoint) {
		this.connectionManager = connectionManager;
		this.otherEndpoint = endpoint;
		this.channel = null;
	}

	public Connection(ConnectionManager connectionManager, Endpoint endpoint,
			IncomingChannelHandler incomingChannel) {
		this.connectionManager = connectionManager;
		this.otherEndpoint = endpoint;
		this.channel = incomingChannel.getChannel();
		this.incomingChannel = incomingChannel;
	}

	public void init() {
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
		Connection.logger.log(Level.INFO, "Connecting to: " + this.otherEndpoint);
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
			Connection.logger.log(Level.INFO, this.connectionManager.getSelfEndpoint()
					+ " connected to:" + this.otherEndpoint);
			synchronized (this) {
				this.channel = future.getChannel();

				if (this.closed) {
					this.cleanup();
				} else {
					this.sendMessage(new EndpointInfo(this.connectionManager.getSelfEndpoint()));

					while (this.outgoing.size() > 0) {
						this.channel.write(this.outgoing.poll());
					}
				}
			}
		} else {
			Connection.logger.log(Level.WARNING, this.connectionManager.getSelfEndpoint()
					+ " could not create a connection to: " + this.otherEndpoint);
			future.getCause().printStackTrace(); // TODO: update me with
													// correct
													// error treatment
			this.cleanup();
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
		this.incomingMessage(this.channel, e);
	}

	public void incomingMessage(Channel c, MessageEvent e) {
		assert (c == this.channel && this.incomingChannel == null || c == this.incomingChannel
				.getChannel());
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
		this.cleanup();
	}

	@Override
	public void channelClosed(
			ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		this.connectionManager.removeConnection(this);
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		Connection.logger.log(
				Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		this.cleanup();
	}

	private void cleanup() {
		synchronized (this) {
			if (this.channel != null) {
				this.channel.close();
				this.channel = null;
			}
			if (this.incomingChannel != null) {
				this.incomingChannel.close();
				this.incomingChannel = null;
			}
			this.outgoing.clear();
			this.bootstrap.releaseExternalResources();
			this.closed = true;
		}
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

	public Endpoint getOtherEndpoint() {
		return this.otherEndpoint;
	}

	public void addIncomingChannel(IncomingChannelHandler incomingChannel) {
		if (this.incomingChannel != null) throw new RuntimeException("OOPS, SHOULD NEVER HAPPEN");
		synchronized (this) {
			this.incomingChannel = incomingChannel;
		}
	}

	@Override
	public String toString() {
		return "C:" + this.otherEndpoint + (this.incomingChannel != null ? "/in" : "/out");
	}
}
