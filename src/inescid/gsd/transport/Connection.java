package inescid.gsd.transport;

import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.transport.events.EndpointInfo;
import inescid.gsd.transport.exception.TransportException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class Connection extends SimpleChannelUpstreamHandler {
	public final Endpoint otherEndpoint;

	private final ConnectionManager connectionManager;
	private Channel channel;

	private final Queue<Object> outgoing = new LinkedList<Object>();;

	private boolean shouldClose = false;
	private boolean closed = false;

	private IncomingChannelHandler incomingChannel;

	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());

	public Connection(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		channel = null;
		otherEndpoint = null;// fix me
	}

	public Connection(ConnectionManager connectionManager, Endpoint endpoint) {
		this.connectionManager = connectionManager;
		otherEndpoint = endpoint;
		channel = null;
		incomingChannel = null;
	}

	public Connection(ConnectionManager connectionManager, Endpoint endpoint,
			IncomingChannelHandler incomingChannel) {
		this.connectionManager = connectionManager;
		otherEndpoint = endpoint;
		channel = incomingChannel.getChannel();
		this.incomingChannel = incomingChannel;
	}

	void channelConnectedCallback(ChannelFuture future) {
		shouldClose = false;
		assert (channel == null);
		if (future.getChannel() != null)
			connectionManager.addChannel(future.getChannel());
		if (future.isSuccess()) {
			Connection.logger.log(Level.FINE, "successfully connected");
			synchronized (this) {
				channel = future.getChannel();

				if (closed)
					cleanDie();
				else {
					sendMessage(new EndpointInfo(connectionManager.getSelfEndpoint()));
					Connection.logger.log(Level.FINE, "sent endpoint info");

					while (outgoing.size() > 0)
						channel.write(outgoing.poll());
				}
			}
		} else {
			Connection.logger.log(Level.FINE, "unsuccessful connection");
			die(future.getCause());
		}
	}

	/**
	 * Send message to other endpoint of this connection. Message is sent
	 * assynchronously.
	 * 
	 * @param message
	 */
	public void sendMessage(Object message) {
		shouldClose = false;
		synchronized (this) {
			if (channel == null)
				outgoing.add(message);
			else
				try {
					Connection.logger.log(Level.FINEST, "writing to channel");
					channel.write(message);
					Connection.logger.log(Level.FINEST, "wrote to channel");
				} catch (Throwable e) {
					Connection.logger.log(Level.SEVERE, "caught exception: " + e);
				}
		}
	}

	/**
	 * Invoked when a message object was received from a remote peer.
	 */
	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		shouldClose = false;
		incomingMessage(channel, e);
	}

	public void incomingMessage(Channel c, MessageEvent e) {
		assert (((c == channel) && (incomingChannel == null)) || (c == incomingChannel
				.getChannel()));
		connectionManager.deliverEvent(otherEndpoint, e.getMessage());
	}

	@Override
	public void handleUpstream(
			ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent) Connection.logger.log(Level.FINER, e.toString());
		super.handleUpstream(ctx, e);
	}

	@Override
	public void channelClosed(
			ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		die(new TransportException("Channel was closed"));
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		Connection.logger.log(Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		die(e.getCause());
	}

	public Endpoint getOtherEndpoint() {
		return otherEndpoint;
	}

	public void addIncomingChannel(IncomingChannelHandler incomingChannel) {
		if (this.incomingChannel != null)
			ConnectionManager.die(this.getClass().getName()
					+ "trying to add different incoming channel handler for connection!");
		synchronized (this) {
			this.incomingChannel = incomingChannel;
		}
	}

	/**
	 * This method kills this connection, cleans it and removes it from
	 * ConnectionManager
	 */
	public void die(Throwable t) {
		cleanAndClose();
		connectionManager.removeConnection(this);
		connectionManager.deliverEvent(otherEndpoint, new DeathNotification(t));
	}

	private void cleanDie() {
		cleanAndClose();
		connectionManager.removeConnection(this);
	}

	public void cleanAndClose() {
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
			closed = true;
		}
	}

	public void closeIncoming(IncomingChannelHandler incomingChannelHandler) {
		synchronized (this) {
			if (incomingChannel != incomingChannelHandler)
				ConnectionManager
				.die(this.getClass().getName()
						+ "trying to close different incoming channel handler for connection! "
								+ incomingChannel + "!=" + incomingChannelHandler + " for endpoint: "
								+ otherEndpoint);
			if (incomingChannel.getChannel() == channel) cleanDie();
		}
	}

	@Override
	public String toString() {
		return "Conn:" + otherEndpoint + (incomingChannel != null ? "/in" : "/out");
	}

	public void checkAlive() {
		synchronized (this) {
			if (shouldClose) {
				; // no contacts for some time
				if (incomingChannel.getChannel() == channel) {
					// don't close, it's the other endpoint's responsibility
				} else {
					channel.close();
					if (incomingChannel != null)
						channel = incomingChannel.getChannel();
				}
			}
		}
		shouldClose = true;
	}
}
