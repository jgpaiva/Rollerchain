package inescid.gsd.test;

import inescid.gsd.rollerchain.interfaces.EventReceiver;
import inescid.gsd.transport.Connection;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransportTest implements Runnable, EventReceiver {
	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());
	private static final int INSTANCES = 3;
	private static final String HOSTNAME = "localhost";
	private static final int PORT = 8071;

	private final int id;
	private final Endpoint endpoint;
	private ConnectionManager connectionManager;

	private final Endpoint contact;

	private final Set<Endpoint> knownEndpoints = new TreeSet<Endpoint>();
	private int currentRound = 0;
	private boolean stopped = false;

	public TransportTest(int id, int otherId) {
		this.id = id;
		this.contact = new Endpoint(TransportTest.HOSTNAME, TransportTest.PORT + otherId);
		this.endpoint = new Endpoint(TransportTest.HOSTNAME, TransportTest.PORT + id);
		this.knownEndpoints.add(this.endpoint);
	}

	@Override
	public void run() {
		this.connectionManager = new ConnectionManager(this, this.endpoint);

		this.sleep(2000);

		this.sendMessage(this.contact);
	}

	@Override
	public void processEvent(Object myobj) {
		if (myobj instanceof Message) {
			this.knownEndpoints.addAll(((Message) myobj).knownEndpoints);
			if (this.knownEndpoints.size() == TransportTest.INSTANCES) {
				System.out.println(this.id + " is finished: " + this.knownEndpoints);
				System.out.println(this.connectionManager);
			}
			Endpoint temp = Utils.getRandomEl(this.knownEndpoints);

			this.sleep(2000);

			this.sendMessage(temp);
		} else
			throw new RuntimeException("oops");
	}

	private void sendMessage(Endpoint e) {
		Connection connection = this.connectionManager.getConnection(e);
		if (this.currentRound < TransportTest.INSTANCES) {
			connection.sendMessage(new Message(this.knownEndpoints));
		}
		this.currentRound++;
	}

	private void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} // wait for a bit
	}

	public static void main(String[] args) {
		ArrayList<TransportTest> tests = new ArrayList<TransportTest>();

		for (int i = 0; i < TransportTest.INSTANCES; i++) {
			tests.add(new TransportTest(i, (i + 1) % TransportTest.INSTANCES));
		}
		System.err.println("init done");

		ArrayList<Thread> threads = new ArrayList<Thread>();
		for (TransportTest it : tests) {
			threads.add(new Thread(it));
		}
		for (Thread it : threads) {
			it.start();
		}
		System.err.println("launched all threads");
		for (Thread it : threads) {
			try {
				it.join();
			} catch (InterruptedException e) {
			}
		}
		System.err.println("joined all threads");

		while (true) {
			int notStopped = 0;
			for (TransportTest it : tests) {
				if (it.currentRound > TransportTest.INSTANCES && !it.stopped) {
					TransportTest.logger.log(Level.INFO, "Stopping thread " + it.id);
					it.connectionManager.shutdown();
					it.stopped = true;
				}
				if (!it.stopped) {
					notStopped++;
				}
			}
			if (notStopped == 0) {
				break;
			}
		}
	}
}

class Message implements Serializable {
	private static final long serialVersionUID = 803543778717265759L;

	Set<Endpoint> knownEndpoints;

	public Message(Set<Endpoint> knownEndpoints) {
		this.knownEndpoints = knownEndpoints;
	}
}
