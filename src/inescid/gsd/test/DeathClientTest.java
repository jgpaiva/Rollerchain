package inescid.gsd.test;

import inescid.gsd.common.EventReceiver;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;

import java.io.Serializable;

public class DeathClientTest implements EventReceiver {
	ConnectionManager manager;
	private final Endpoint serverEndpoint;

	DeathClientTest(Endpoint endpoint, Endpoint serverEndpoint) {
		manager = new ConnectionManager(this, endpoint);
		this.serverEndpoint = serverEndpoint;
	}

	public static void main(String[] args) {
		Endpoint serverEndpoint = new Endpoint("localhost", 8090);
		DeathClientTest obj = new DeathClientTest(new Endpoint("localhost", 8091), serverEndpoint);
		try {
			Thread.sleep(2 * 1000); // give some time for initialization
		} catch (InterruptedException e) {
		}
		obj.sendMessage();
	}

	private void sendMessage() {
		manager.getConnection(serverEndpoint).sendMessage(new PingPongMessage());
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		System.out.println(message);
		System.exit(-1);
	}
}

class PingPongMessage implements Serializable {
	private static final long serialVersionUID = 4191571455278808818L;

	int counter = 0;

	@Override
	public String toString() {
		return "hello world " + counter;
	}

	public PingPongMessage incr() {
		counter++;
		return this;
	}
}
