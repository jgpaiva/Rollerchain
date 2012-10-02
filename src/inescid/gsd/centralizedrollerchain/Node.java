package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.events.InstantDeath;
import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.centralizedrollerchain.interfaces.InternalEvent;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;
import inescid.gsd.centralizedrollerchain.utils.NamedThreadFactory;
import inescid.gsd.centralizedrollerchain.utils.PriorityPair;
import inescid.gsd.transport.Connection;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.transport.interfaces.EventReceiver;

import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Node implements EventReceiver {
	// Transport layer
	protected final ConnectionManager connectionManager;

	// This node's endpoint
	protected final Endpoint endpoint;

	protected ScheduledExecutorService executor;

	// Queue for the objects received from TCP
	private final PriorityBlockingQueue<PriorityPair<Endpoint, Object>> queue = new PriorityBlockingQueue<PriorityPair<Endpoint, Object>>();

	static final Logger logger = Logger.getLogger(Node.class.getName());

	public Node(Endpoint endpoint) {
		this.endpoint = endpoint;
		connectionManager = new ConnectionManager(this, endpoint);
		// initialize the single thread of this node
		Node.logger.log(Level.INFO, "Created new Node");
	}

	/**
	 * Method used to initialize the node. If it needs to do something when
	 * booted, this is where it should be done.
	 */
	public void init() {
		executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("NodeThreadPool_"));
	}

	protected abstract void processEventInternal(Endpoint fst, Object snd);

	@Override
	public void processEvent(Endpoint source, Object message) {
		if (executor == null)
			Node.die("Node not initialized!");

		if (message instanceof InstantDeath) {
			Node.logger.log(Level.SEVERE, "Now dying. Received " + message + " from " + source);
			System.exit(-1);
		}

		Node.logger.log(Level.FINEST, "Queuing event from: " + source + " / " + message);
		if (message instanceof Event)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 0));
		else if (message instanceof UpperLayerMessage)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 0));
		else if (message instanceof DeathNotification)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 1));
		else if (message instanceof InternalEvent)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 2));
		else
			Node.logger.log(Level.SEVERE, "Received unknown event: " + message);
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					takeFromQueue();
				} catch (Throwable t) {
					Node.die(t);
				}
			}
		});
		Node.logger.log(Level.FINEST, "Queued event from: " + source + " / " + message);
	}

	private void takeFromQueue() {
		PriorityPair<Endpoint, Object> el = null;
		if (queue.size() == 0) {
			Node.logger.log(Level.SEVERE, "queue was empty. Quitting");
			System.exit(-1);
		}
		try {
			el = queue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			Node.logger.log(Level.SEVERE, "error while taking from the queue. Quitting");
			System.exit(-1);
		}
		processEventInternal(el.getFst(), el.getSnd());
	}

	public void sendMessage(Endpoint dest, Event message) {
		if (dest == null)
			Node.die("Endpoint was null");

		Node.logger.log(Level.FINE, endpoint + " S: " + dest + " / " + message);
		Connection temp = connectionManager.getConnection(dest);
		Node.logger.log(Level.FINEST, "got connection to send message: " + dest + " / "
				+ message);
		temp.sendMessage(message);
		Node.logger.log(Level.FINEST, "sent message: " + dest + " / " + message);
	}

	public void kill(Endpoint source) {
		connectionManager.shutdown();
		queue.clear();
		executor.shutdownNow();
		Node.logger.log(Level.SEVERE, "node " + endpoint + " was killed by " + source);
	}

	public static void die(String string) {
		Thread.dumpStack();
		System.out.println("SEVERE ERROR: " + string);
		System.err.println("SEVERE ERROR: " + string);
		Node.logger.log(Level.SEVERE, string);
		System.exit(-29);
	}

	protected static void die(Throwable t) {
		t.printStackTrace();
		Node.die("Exception found when processing job: " + t);
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public ScheduledExecutorService getExecutor() {
		return executor;
	}
}