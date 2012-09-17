package objectecho;

import inescid.gsd.rollerchain.interfaces.EventReceiver;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class Connection extends SimpleChannelHandler {
	boolean dirty;

	private Endpoint otherEndpoint;
	private Endpoint myEndpoint;

	private ConnectionManager connectionManager;

	private EventReceiver self;

	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());

	Connection(ConnectionManager connectionManager, Endpoint myEndpoint, Endpoint otherEndpoint,
			EventReceiver self) {
		this.connectionManager = connectionManager;
		this.myEndpoint = myEndpoint;
		this.otherEndpoint = otherEndpoint;
		this.self = self;
	}

	public boolean isDirty() {
		return this.dirty;
	}

	public void setDirty() {
		this.dirty = true;
	}

	public void close() {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not yet implemented");
	}

	public void clean() {
		this.dirty = false;

	}

	// TODO: understand what this does
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof ChannelStateEvent &&
				((ChannelStateEvent) e).getState() != ChannelState.INTEREST_OPS) {
			Connection.logger.info(e.toString());
		}
		super.handleUpstream(ctx, e);
	}

	// public void channelConnected(
	// ChannelHandlerContext ctx, ChannelStateEvent e) {
	// }

	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) {
		MyObject myobj = (MyObject) e.getMessage();

		this.clean();

		this.self.processEvent(myobj);
	}

	public void sendMessage(Object message) {

	}

	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		Connection.logger.log(
				Level.WARNING,
				"Unexpected exception from downstream.",
				e.getCause());
		e.getChannel().close();
	}
}
