package inescid.gsd.transport;

import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.transport.events.EndpointInfo;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
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

	public final Endpoint otherEndpoint;

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
		otherEndpoint = endpoint;
		channel = null;
	}

	public Connection(ConnectionManager connectionManager, Endpoint endpoint,
			IncomingChannelHandler incomingChannel) {
		this.connectionManager = connectionManager;
		otherEndpoint = endpoint;
		channel = incomingChannel.getChannel();
		this.incomingChannel = incomingChannel;
	}

	public void init(ExecutorService bossThreadPool, ExecutorService workerThreadPool) {
		// Configure the client.
		bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(bossThreadPool, workerThreadPool));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
						new ObjectEncoder(),
						new ObjectDecoder(
								ClassResolvers.cacheDisabled(this.getClass().getClassLoader())),
						Connection.this);
			}
		});

		bootstrap.setOption("tcpnodelay", true);
		bootstrap.setOption("keepAlive", true);

		// Start the connection attempt.
		Connection.logger.log(Level.FINE, "Connecting to: " + otherEndpoint);
		ChannelFuture future = bootstrap.connect(otherEndpoint.getInetAddress());

		// Wait until the connection attempt succeeds or fails.
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future2) throws Exception {
				Connection.this.channelConnectedCallback(future2);
			}
		});
	}

	void channelConnectedCallback(ChannelFuture future) {
		assert (channel == null);
		if (future.getChannel() != null)
			connectionManager.addChannel(future.getChannel());
		if (future.isSuccess())
			synchronized (this) {
				channel = future.getChannel();

				if (closed)
					cleanup();
				else {
					sendMessage(new EndpointInfo(connectionManager.getSelfEndpoint()));

					while (outgoing.size() > 0)
						channel.write(outgoing.poll());
				}
			}
		else {
			future.getCause().printStackTrace();
			connectionManager.deliverEvent(otherEndpoint, new DeathNotification(future.getCause()));
			connectionManager.removeConnection(this);
		}
	}

	/**
	 * Send message to other endpoint of this connection. Message is sent
	 * assynchronously.
	 * 
	 * @param message
	 */
	public void sendMessage(Object message) {
		clean();
		synchronized (this) {
			if (channel == null)
				outgoing.add(message);
			else
				channel.write(message);
		}
	}

	/**
	 * Invoked when a message object was received from a remote peer.
	 */
	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		incomingMessage(channel, e, otherEndpoint);
	}

	public void incomingMessage(Channel c, MessageEvent e, Endpoint source) {
		clean();
		assert (((c == channel) && (incomingChannel == null)) || (c == incomingChannel
				.getChannel()));
		connectionManager.deliverEvent(source, e.getMessage());
	}

	@Override
	public void handleUpstream(
			ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) Connection.logger.log(Level.FINER, e.toString());
		super.handleUpstream(ctx, e);
	}

	public void close() {
		cleanup();
	}

	@Override
	public void channelClosed(
			ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		connectionManager.removeConnection(this);
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		Connection.logger.log(
				Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		connectionManager.deliverEvent(otherEndpoint, new DeathNotification(e.getCause()));
		connectionManager.removeConnection(this);
	}

	private void cleanup() {
		synchronized (this) {
			boolean hadChannel = false;
			if (channel != null) {
				channel.close();
				channel = null;
				hadChannel = true;
				Connection.logger.log(Level.FINER, "Closed outgoing channel");
			}
			boolean hadIncommingChannel = false;
			if (incomingChannel != null) {
				incomingChannel.close();
				incomingChannel = null;
				hadIncommingChannel = true;
				Connection.logger.log(Level.FINER, "Closed incoming channel");
			}
			outgoing.clear();
			if (bootstrap != null) {
				Connection.logger.log(Level.FINE, "Releasing external resources");
				bootstrap.releaseExternalResources();
				Connection.logger.log(Level.FINE, "Released external resources");
			} else if (!hadIncommingChannel)
				Connection.logger.log(Level.SEVERE, "had no bootstrap not incoming channel!");
			closed = true;
		}
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty() {
		dirty = true;
	}

	public void clean() {
		dirty = false;
	}

	public Endpoint getOtherEndpoint() {
		return otherEndpoint;
	}

	public void addIncomingChannel(IncomingChannelHandler incomingChannel) {
		if (this.incomingChannel != null) throw new RuntimeException("OOPS, SHOULD NEVER HAPPEN");
		clean();
		synchronized (this) {
			this.incomingChannel = incomingChannel;
		}
	}

	@Override
	public String toString() {
		return "Conn:" + otherEndpoint + (incomingChannel != null ? "/in" : "/out");
	}
}
