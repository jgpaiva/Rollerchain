package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.events.Divide;
import inescid.gsd.centralizedrollerchain.events.KeepAlive;
import inescid.gsd.centralizedrollerchain.events.Merge;
import inescid.gsd.centralizedrollerchain.events.SetNeighbours;
import inescid.gsd.centralizedrollerchain.events.WorkerInit;
import inescid.gsd.centralizedrollerchain.internalevents.DieEvent;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.interfaces.EventReceiver;

import java.util.Set;
import java.util.logging.Level;

public class WorkerNode extends Node {
	private final Endpoint masterEndpoint;

	private final WorkerNodeInternalState s = new WorkerNodeInternalState();

	private final EventReceiver upperLayer;

	public WorkerNode(Endpoint endpoint, Endpoint masterEndpoint) {
		super(endpoint);
		this.masterEndpoint = masterEndpoint;
		upperLayer = new EventReceiver() {
			@Override
			public void processEvent(Endpoint source, Object message) {
				// discard event
			}
		};
	}

	public WorkerNode(Endpoint endpoint, Endpoint masterEndpoint, EventReceiver upperLayer) {
		super(endpoint);
		this.masterEndpoint = masterEndpoint;
		this.upperLayer = upperLayer;
	}

	@Override
	public void processEventInternal(Endpoint source, Object message) {
		Node.logger.log(Level.FINE, "worker " + endpoint + " R: " + source + " / " + message);
		if (message instanceof SetNeighbours)
			processSetNeighbours(source, (SetNeighbours) message);
		else if (message instanceof Divide)
			processDivide(source, (Divide) message);
		else if (message instanceof Merge)
			processMerge(source, (Merge) message);
		else if (message instanceof DieEvent)
			processDieEvent(source, (DieEvent) message);
		else if (message instanceof KeepAlive)
			; // drop
		else
			Node.logger.log(Level.SEVERE, "Received unknown event: " + message);
	}

	@Override
	public void run() {
		sendMessage(masterEndpoint, new WorkerInit());
		super.run();
	}

	private void processSetNeighbours(Endpoint source, SetNeighbours setNeighbours) {
		s.setGroup(setNeighbours.getGroup());
		s.setSuccessorGroup(setNeighbours.getSuccessorGroup());
		s.setPredecessorGroup(setNeighbours.getPredecessorGroup());

		upperLayer.processEvent(source, setNeighbours);
	}

	private void processDivide(Endpoint source, Divide message) {
		if (message.getNewGroup().contains(endpoint)) {
			s.setPredecessorGroup(message.getOldGroup());
			if (s.getSuccessorGroup() == null)
				s.setSuccessorGroup(message.getOldGroup());
		} else {
			s.setSuccessorGroup(message.getNewGroup());
			if (s.getPredecessorGroup() == null)
				s.setPredecessorGroup(message.getNewGroup());
		}
		upperLayer.processEvent(source, message);
	}

	private void processMerge(Endpoint source, Merge message) {
		if (message.getSmallGroup().contains(endpoint)) {
			s.setSuccessorGroup(message.getSuccessorGroup());
			if (s.getSuccessorGroup() == null)
				s.setPredecessorGroup(null);
		}
		else if (message.getLargeGroup().contains(endpoint)) {
			s.setPredecessorGroup(message.getPredecessorGroup());
			if (s.getPredecessorGroup() == null)
				s.setSuccessorGroup(null);
		}

		upperLayer.processEvent(source, message);
	}

	private void processDieEvent(Endpoint source, DieEvent message) {
		super.die();
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
