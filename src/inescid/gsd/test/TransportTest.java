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
			TransportTest.class.getName());
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
		this.contact = new Endpoint(TransportTest.HOSTNAME, TransportTest.PORT + otherId);
		this.endpoint = new Endpoint(TransportTest.HOSTNAME, TransportTest.PORT + this.id);
		this.knownEndpoints.add(this.endpoint);
		this.tp = Executors.newScheduledThreadPool(1);
		this.tp.submit(new Runnable() {
			@Override
			public void run() {
				TransportTest.this.init();
			}
		});
	}

	public void init() {
		this.connectionManager = new ConnectionManager(this, this.endpoint);

		this.tp.scheduleAtFixedRate(new Runnable() {
			public void run() {
				TransportTest.this.round();
			}
		}, 2, 2, TimeUnit.SECONDS);
	}

	public void round() {
		if (this.knownEndpoints.size() == TransportTest.INSTANCES) {
			System.out.println(this.id + " is finished: " + this.knownEndpoints);
			System.out.println(this.connectionManager);
		}
		Endpoint temp = Utils.getRandomEl(this.knownEndpoints);

		if (this.currentRound <= TransportTest.INSTANCES + 3) {
			Connection connection = this.connectionManager.getConnection(temp);
			connection.sendMessage(new Message(this.knownEndpoints));
		}
		if (this.currentRound > TransportTest.INSTANCES + 4) {
			this.shutdown();
		}

		this.currentRound++;
	}

	@Override
	public void processEvent(Endpoint source, Object myobj) {
		this.tp.submit(new EventToProcess(myobj));
	}

	class EventToProcess implements Runnable {
		private final Object obj;

		@Override
		public void run() {
			TransportTest.this.internalProcessEvent(this.obj);
		}

		EventToProcess(Object obj) {
			this.obj = obj;
		}
	}

	public void internalProcessEvent(Object myobj) {
		if (myobj instanceof Message) {
			this.knownEndpoints.addAll(((Message) myobj).knownEndpoints);
		} else
			throw new RuntimeException("oops");
	}

	public void shutdown() {
		if (this.stopped)
			return;
		this.connectionManager.shutdown();
		this.stopped = true;
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

		while (true) {
			int notStopped = 0;
			for (TransportTest it : tests) {
				if (it.stopped && it.tp != null) {
					it.tp.shutdownNow();
					it.tp = null;
				}
				if (!it.stopped) {
					notStopped++;
				}
			}
			if (notStopped == 0) {
				break;
			}
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
