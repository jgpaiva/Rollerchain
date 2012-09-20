package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.events.SetNeighbours;
import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.centralizedrollerchain.utils.Pair;
import inescid.gsd.common.EventReceiver;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;

import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkerNode implements EventReceiver {
	private final ConnectionManager connectionManager;
	private final Endpoint endpoint;
	private final Endpoint masterEndpoint;

	private final WorkerNodeInternalState s = new WorkerNodeInternalState();

	PriorityBlockingQueue<Pair<Endpoint, Event>> queue = new PriorityBlockingQueue<Pair<Endpoint, Event>>();

	private static final Logger logger = Logger.getLogger(
			WorkerNode.class.getName());

	public WorkerNode(Endpoint endpoint, Endpoint masterEndpoint) {
		this.endpoint = endpoint;
		this.masterEndpoint = masterEndpoint;
		connectionManager = new ConnectionManager(this, endpoint);
	}

	public void start() {
		while (true)
			try {
				Pair<Endpoint, Event> res = queue.take();
				processEventInternal(res.getFst(), res.getSnd());
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		if (message instanceof Event)
			queue.add(new Pair<Endpoint, Event>(source, (Event) message));
		else
			WorkerNode.logger.log(Level.SEVERE, "Received unknown event: " + message);
	}

	public void processEventInternal(Endpoint source, Event message) {
		if (message instanceof SetNeighbours) processSetNeighbours((SetNeighbours) message);
	}

	private void processSetNeighbours(SetNeighbours e) {
		s.setGroup(e.getGroup());
		s.setSuccessorGroup(e.getSuccessorGroup());
		s.setPredecessorGroup(e.getPredecessorGroup());
	}
}

class WorkerNodeInternalState {
	private Set<Endpoint> group;
	private Set<Endpoint> successorGroup;
	private Set<Endpoint> predecessorGroup;

	public Set<Endpoint> getGroup() {
		return group;
	}

	public void setGroup(Set<Endpoint> group) {
		this.group = group;
	}

	public Set<Endpoint> getSuccessorGroup() {
		return successorGroup;
	}

	public void setSuccessorGroup(Set<Endpoint> successorGroup) {
		this.successorGroup = successorGroup;
	}

	public Set<Endpoint> getPredecessorGroup() {
		return predecessorGroup;
	}

	public void setPredecessorGroup(Set<Endpoint> predecessorGroup) {
		this.predecessorGroup = predecessorGroup;
	}
}
