package inescid.gsd.test;

import inescid.gsd.common.EventReceiver;
import inescid.gsd.transport.Connection;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class TransportTest implements EventReceiver {
	private static final Logger logger = Logger.getLogger(
			Connection.class.getName());
	private static final int INSTANCES = 3;
	private static final String HOSTNAME = "localhost";
	private static final int PORT = 8071;

	private final int id;
	private final Endpoint endpoint;
	private ConnectionManager connectionManager;

	private final Endpoint contact;

	private final ConcurrentSkipListSet<Endpoint> knownEndpoints = new ConcurrentSkipListSet<Endpoint>();
	private int currentRound = 0;
	private boolean stopped = false;
	private ScheduledExecutorService tp;

	public TransportTest(int id, int otherId) {
		this.id = id;
		contact = new Endpoint(TransportTest.HOSTNAME, TransportTest.PORT + otherId);
		endpoint = new Endpoint(TransportTest.HOSTNAME, TransportTest.PORT + this.id);
		knownEndpoints.add(endpoint);
		tp = Executors.newScheduledThreadPool(1);
		tp.submit(new Runnable() {
			@Override
			public void run() {
				TransportTest.this.init();
			}
		});
	}

	public void init() {
		connectionManager = new ConnectionManager(this, endpoint);

		tp.scheduleAtFixedRate(new Runnable() {
			public void run() {
				TransportTest.this.round();
			}
		}, 2, 2, TimeUnit.SECONDS);
	}

	public void round() {
		if (knownEndpoints.size() == TransportTest.INSTANCES) {
			System.out.println(id + " is finished: " + knownEndpoints);
			System.out.println(connectionManager);
		}
		Endpoint temp = Utils.getRandomEl(knownEndpoints);

		if (currentRound <= (TransportTest.INSTANCES + 3)) {
			Connection connection = connectionManager.getConnection(temp);
			connection.sendMessage(new Message(knownEndpoints));
		}
		if (currentRound > (TransportTest.INSTANCES + 4)) shutdown();

		currentRound++;
	}

	@Override
	public void processEvent(Endpoint source, Object myobj) {
		tp.submit(new EventToProcess(myobj));
	}

	class EventToProcess implements Runnable {
		private final Object obj;

		@Override
		public void run() {
			internalProcessEvent(obj);
		}

		EventToProcess(Object obj) {
			this.obj = obj;
		}
	}

	public void internalProcessEvent(Object myobj) {
		if (myobj instanceof Message)
			knownEndpoints.addAll(((Message) myobj).knownEndpoints);
		else
			throw new RuntimeException("oops");
	}

	public void shutdown() {
		if (stopped)
			return;
		connectionManager.shutdown();
		stopped = true;
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

		for (int i = 0; i < TransportTest.INSTANCES; i++)
			tests.add(new TransportTest(i, (i + 1) % TransportTest.INSTANCES));
		System.err.println("init done");

		while (true) {
			int notStopped = 0;
			for (TransportTest it : tests) {
				if (it.stopped && (it.tp != null)) {
					it.tp.shutdownNow();
					it.tp = null;
				}
				if (!it.stopped) notStopped++;
			}
			if (notStopped == 0) break;
		}
		System.err.println("Should quit now");
	}
}

class Message implements Serializable {
	private static final long serialVersionUID = 803543778717265759L;

	Set<Endpoint> knownEndpoints;

	public Message(Set<Endpoint> knownEndpoints) {
		this.knownEndpoints = new TreeSet<Endpoint>(knownEndpoints);
	}
}
