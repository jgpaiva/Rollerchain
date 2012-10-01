package inescid.gsd.transport.test;

import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.transport.interfaces.EventReceiver;

public class LargeDataServerTest implements EventReceiver {
	ConnectionManager manager;

	LargeDataServerTest(Endpoint endpoint) {
		manager = new ConnectionManager(this, endpoint);
	}

	public static void main(String[] args) {
		LargeDataServerTest obj = new LargeDataServerTest(new Endpoint("localhost", 8090));
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		if (message instanceof DeathNotification) {
			System.out.println(source + " is unreachable. Quitting");
			System.exit(-1);
		} else {
			System.out.println("received message: " + message);
			manager.getConnection(source).sendMessage(((LargeDataMessage) message).incr());
		}
	}
}
