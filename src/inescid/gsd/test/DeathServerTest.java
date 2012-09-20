package inescid.gsd.test;

import inescid.gsd.common.EventReceiver;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;

public class DeathServerTest implements EventReceiver {
	ConnectionManager manager;

	DeathServerTest(Endpoint endpoint) {
		manager = new ConnectionManager(this, endpoint);
	}

	public static void main(String[] args) {
		DeathServerTest obj = new DeathServerTest(new Endpoint("localhost", 8090));
		try {
			Thread.sleep(20 * 1000);
		} catch (InterruptedException e) {
		}
		obj.shutdown();
	}

	private void shutdown() {
		manager.shutdown();
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		if (message instanceof DeathNotification)
			System.out.println(source + " is unreachable");
		else {
			System.out.println("received message: " + message);

			manager.getConnection(source).sendMessage(((PingPongMessage) message).incr());
			try {
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("sending second message");
			manager.getConnection(source).sendMessage(((PingPongMessage) message).incr());
		}
	}
}
