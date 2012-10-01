package inescid.gsd.transport.test;

import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.transport.interfaces.EventReceiver;
import inescid.gsd.utils.Utils;

import java.io.Serializable;
import java.math.BigInteger;

public class LargeDataClientTest implements EventReceiver {
	ConnectionManager manager;
	private final Endpoint serverEndpoint;

	LargeDataClientTest(Endpoint endpoint, Endpoint serverEndpoint) {
		manager = new ConnectionManager(this, endpoint);
		this.serverEndpoint = serverEndpoint;
	}

	public static void main(String[] args) {
		Endpoint serverEndpoint = new Endpoint("localhost", 8090);
		LargeDataClientTest obj = new LargeDataClientTest(new Endpoint("localhost", 8091), serverEndpoint);
		try {
			Thread.sleep(2 * 1000); // give some time for initialization
		} catch (InterruptedException e) {
		}
		obj.sendMessage(new LargeDataMessage());
	}

	private void sendMessage(LargeDataMessage message) {
		System.err.println("CLIENT: Sending message");
		manager.getConnection(serverEndpoint).sendMessage(message);
		System.err.println("CLIENT: Sent message");
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		if (message instanceof DeathNotification) {
			System.out.println(source + " is unreachable. Quitting");
			System.exit(-1);
		}

		LargeDataMessage msg = ((LargeDataMessage) message);
		System.out.println(message);
		if (msg.getValue() < 20)
			sendMessage(msg);
		else
			System.exit(-1);
	}
}

class LargeDataMessage implements Serializable {
	private static final long serialVersionUID = 4191571455278808818L;

	BigInteger counters[] = new BigInteger[10000];

	public LargeDataMessage() {
		for (int it : Utils.range(counters.length))
			if(it == 0)
				counters[it] = BigInteger.ZERO;
			else
				counters[it] = counters[it-1].add(BigInteger.ONE);
	}

	@Override
	public String toString() {
		return "hello world " + counters[0];
	}

	public int getValue() {
		return counters[0].intValue();
	}

	public LargeDataMessage incr() {
		for (int it : Utils.range(counters.length))
			counters[it] = counters[it].add(BigInteger.ONE);
		return this;
	}
}
