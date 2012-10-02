package inescid.gsd.transport;

import inescid.gsd.transport.events.EndpointInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class IncomingChannelHandler extends SimpleChannelUpstreamHandler {
	private final ConnectionManager connectionManager;
	private Connection connection;
	private Channel channel;
	private boolean closed;

	private static final Logger logger = Logger.getLogger(
			IncomingChannelHandler.class.getName());

	public IncomingChannelHandler(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		channel = null;
		closed = false;
		IncomingChannelHandler.logger.log(Level.FINER, "New incoming channel created");
	}

	@Override
	public void channelConnected(
			ChannelHandlerContext ctx, ChannelStateEvent e) {
		channel = e.getChannel();
		connectionManager.addChannel(channel);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		// empty
		IncomingChannelHandler.logger.log(Level.FINE, "Incomming channel closed");
		synchronized (this) {
			if (closed)
				return;
			closed = true;

			if (connection == null)
				close();
			else
				connection.closeIncoming(this);
		}
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		IncomingChannelHandler.logger.log(Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		synchronized (this) {
			if (closed)
				return;

			if (connection == null)
				channel.close();
			else
				connection.die(e.getCause());
		}
	}

	public Channel getChannel() {
		return channel;
	}

	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		synchronized (this) {
			if (closed)
				return;

			if (e.getMessage() instanceof EndpointInfo) {
				EndpointInfo message = (EndpointInfo) e.getMessage();
				connection = connectionManager.createConnection(this, message.endpoint);
			} else {
				if (connection == null)
					ConnectionManager.die(this.getClass().getName() + " Received: " + e.getMessage()
							+ " and connection was null.");
				connection.incomingMessage(channel, e);
			}
		}
	}

	public void close() {
		synchronized (this) {
			if (closed)
				return;

			channel.close();
			closed = true;
		}
	}
}
