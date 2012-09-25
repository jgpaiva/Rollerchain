package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.centralizedrollerchain.interfaces.InternalEvent;
import inescid.gsd.centralizedrollerchain.utils.PriorityPair;
import inescid.gsd.transport.Connection;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.transport.interfaces.EventReceiver;

import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Node implements EventReceiver, Runnable {
	// Transport layer
	protected final ConnectionManager connectionManager;

	// This node's endpoint
	protected final Endpoint endpoint;

	protected final ScheduledExecutorService executor;

	class MyThreadFactory implements ThreadFactory {
		private final String threadPre;
		private final ThreadFactory realFactory = Executors.defaultThreadFactory();

		public MyThreadFactory(String string) {
			threadPre = string;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread temp = realFactory.newThread(r);
			temp.setName(threadPre + temp.getName());
			return temp;
		}
	}

	// Queue for the objects received from TCP
	private final PriorityBlockingQueue<PriorityPair<Endpoint, Object>> queue = new PriorityBlockingQueue<PriorityPair<Endpoint, Object>>();

	public static final Logger logger = Logger.getLogger(Node.class.getName());

	public Node(Endpoint endpoint) {
		this.endpoint = endpoint;
		connectionManager = new ConnectionManager(this, endpoint);
		// initialize the single thread of this node
		executor = Executors.newScheduledThreadPool(1, new MyThreadFactory("NodeThreadPool_"));
		Node.logger.log(Level.INFO, "Created new Node");
	}

	public void run() {
	}

	protected abstract void processEventInternal(Endpoint fst, Object snd);

	@Override
	public void processEvent(Endpoint source, Object message) {
		Node.logger.log(Level.FINEST, "Queuing event from: " + source + " / " + message);
		if (message instanceof Event)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 0));
		else if (message instanceof DeathNotification)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 1));
		else if (message instanceof InternalEvent)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 2));
		else
			Node.logger.log(Level.SEVERE, "Received unknown event: " + message);
		Node.logger.log(Level.FINEST, "Queued event from: " + source + " / " + message);
		executor.submit(new Runnable() {
			@Override
			public void run() {
				takeFromQueue();
			}
		});
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

	protected void sendMessage(Endpoint endpoint, Event message) {
		Node.logger.log(Level.FINE, this.endpoint + " S: " + endpoint + " / " + message);
		Connection temp = connectionManager.getConnection(endpoint);
		Node.logger.log(Level.FINEST, "got connection to send message: " + endpoint + " / "
				+ message);
		temp.sendMessage(message);
		Node.logger.log(Level.FINEST, "sent message: " + endpoint + " / " + message);
	}

	public void die() {
		connectionManager.shutdown();
		queue.clear();
		queue.add(new PriorityPair<Endpoint, Object>(endpoint, new DieNow(), 4));
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}
}

class DieNow extends Event {
}