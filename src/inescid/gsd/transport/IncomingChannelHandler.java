package inescid.gsd.transport;

import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.transport.events.EndpointInfo;

import java.net.InetSocketAddress;
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
	private Endpoint otherEndpoint;
	private Connection connection;
	private Channel channel;

	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());

	public IncomingChannelHandler(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
		channel = null;
		IncomingChannelHandler.logger.log(Level.INFO, "init");
	}

	@Override
	public void channelConnected(
			ChannelHandlerContext ctx, ChannelStateEvent e) {
		InetSocketAddress addr = (InetSocketAddress) e.getChannel().getRemoteAddress();
		channel = e.getChannel();
		connectionManager.addChannel(otherEndpoint, channel);
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		IncomingChannelHandler.logger.log(
				Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		connectionManager.deliverEvent(otherEndpoint, new DeathNotification(e.getCause()));
		connectionManager.removeConnection(connection);
	}

	public Endpoint getEndpoint() {
		return otherEndpoint;
	}

	public Channel getChannel() {
		return channel;
	}

	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (e.getMessage() instanceof EndpointInfo) {
			EndpointInfo message = (EndpointInfo) e.getMessage();
			otherEndpoint = message.endpoint;
			connection = connectionManager.createConnection(this);
		} else {
			if (connection == null)
				IncomingChannelHandler.logger.log(Level.SEVERE, "received: " + e.getMessage()
						+ " and connection was null");
			connection.incomingMessage(channel, e, otherEndpoint);
		}
	}

	public void close() {
		channel.close();
	}
}
