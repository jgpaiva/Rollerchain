package inescid.gsd.transport;

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
		this.channel = null;
		IncomingChannelHandler.logger.log(Level.INFO, "init");
	}

	@Override
	public void channelConnected(
			ChannelHandlerContext ctx, ChannelStateEvent e) {
		InetSocketAddress addr = (InetSocketAddress) e.getChannel().getRemoteAddress();
		IncomingChannelHandler.logger.log(Level.INFO, this.connectionManager.getSelfEndpoint()
				+ " has incoming connection from: " + new Endpoint(addr));
		this.channel = e.getChannel();
	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		IncomingChannelHandler.logger.log(
				Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		e.getChannel().close();
		// TODO: handle cleanup
	}

	public Endpoint getEndpoint() {
		return this.otherEndpoint;
	}

	public Channel getChannel() {
		return this.channel;
	}

	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (e.getMessage() instanceof EndpointInfo) {
			EndpointInfo message = (EndpointInfo) e.getMessage();
			this.otherEndpoint = message.endpoint;
			this.connection = this.connectionManager.createConnection(this);
		} else {
			if (this.connection == null) {
				IncomingChannelHandler.logger.log(Level.SEVERE, "received: " + e.getMessage()
						+ " and connection was null");
			}
			this.connection.incomingMessage(this.channel, e);
		}
	}

	public void close() {
		this.channel.close();
	}
}
