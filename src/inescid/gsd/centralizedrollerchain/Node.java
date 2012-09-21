package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.centralizedrollerchain.interfaces.InternalEvent;
import inescid.gsd.centralizedrollerchain.utils.PriorityPair;
import inescid.gsd.common.EventReceiver;
import inescid.gsd.transport.Connection;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Node implements EventReceiver {
	// Transport layer
	protected final ConnectionManager connectionManager;

	// This node's endpoint
	protected final Endpoint endpoint;

	// Queue for the objects received from TCP
	private final PriorityBlockingQueue<PriorityPair<Endpoint, Object>> queue = new PriorityBlockingQueue<PriorityPair<Endpoint, Object>>();

	public static final Logger logger = Logger.getLogger(Node.class.getName());

	public Node(Endpoint endpoint) {
		this.endpoint = endpoint;
		connectionManager = new ConnectionManager(this, endpoint);
	}

	public void start() {
		while (true)
			try {
				PriorityPair<Endpoint, Object> res = queue.take();
				if (res.getSnd() instanceof DieNow)
					break;
				processEventInternal(res.getFst(), res.getSnd());
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
	}

	protected abstract void processEventInternal(Endpoint fst, Object snd);

	@Override
	public void processEvent(Endpoint source, Object message) {
		if (message instanceof Event)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 0));
		else if (message instanceof DeathNotification)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 1));
		else if (message instanceof InternalEvent)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 2));
		else
			Node.logger.log(Level.SEVERE, "Received unknown event: " + message);
	}

	protected void sendMessage(Endpoint endpoint, Event message) {
		Node.logger.log(Level.FINE, this.endpoint + " S: " + endpoint + " / " + message);
		Connection temp = connectionManager.getConnection(endpoint);
		temp.sendMessage(message);
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