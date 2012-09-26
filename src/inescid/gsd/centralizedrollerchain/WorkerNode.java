package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.events.Divide;
import inescid.gsd.centralizedrollerchain.events.DivideIDUpdate;
import inescid.gsd.centralizedrollerchain.events.GroupUpdate;
import inescid.gsd.centralizedrollerchain.events.JoinedNetwork;
import inescid.gsd.centralizedrollerchain.events.KeepAlive;
import inescid.gsd.centralizedrollerchain.events.Merge;
import inescid.gsd.centralizedrollerchain.events.MergeIDUpdate;
import inescid.gsd.centralizedrollerchain.events.SetNeighbours;
import inescid.gsd.centralizedrollerchain.events.WorkerInit;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;
import inescid.gsd.centralizedrollerchain.internalevents.KillEvent;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.interfaces.EventReceiver;

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
		else if (message instanceof KillEvent)
			processKillEvent(source, (KillEvent) message);
		else if (message instanceof KeepAlive)
			; // drop
		else if (message instanceof UpperLayerMessage)
			upperLayer.processEvent(source, message);
		else
			Node.logger.log(Level.SEVERE, "Received unknown event: " + message);
	}

	@Override
	public void init() {
		sendMessage(masterEndpoint, new WorkerInit());
	}

	private void processSetNeighbours(Endpoint source, SetNeighbours setNeighbours) {
		StaticGroup oldGroup = s.getGroup();
		s.setGroup(setNeighbours.getGroup());
		s.setSuccessorGroup(setNeighbours.getSuccessorGroup());
		s.setPredecessorGroup(setNeighbours.getPredecessorGroup());

		if (s.getGroup() == null)
			upperLayer.processEvent(source,
					new JoinedNetwork(setNeighbours.getGroup(), s.getPredecessorId()));
		else {
			if (!s.getGroup().getID().equals(setNeighbours.getGroup().getID()))
				Node.die("Should never happen");

			upperLayer.processEvent(source,
					new GroupUpdate(oldGroup, setNeighbours.getGroup(), s.getPredecessorId()));
		}
	}

	private void processDivide(Endpoint source, Divide message) {
		StaticGroup oldGroup = s.getGroup();
		if (message.getNewGroup().contains(endpoint)) {
			s.setGroup(message.getNewGroup());
			s.setPredecessorGroup(message.getOldGroup());
			if (s.getSuccessorGroup() == null)
				s.setSuccessorGroup(message.getOldGroup());
		} else {
			s.setGroup(message.getOldGroup());
			s.setSuccessorGroup(message.getNewGroup());
			if (s.getPredecessorGroup() == null)
				s.setPredecessorGroup(message.getNewGroup());
		}
		upperLayer.processEvent(source,
				new DivideIDUpdate(oldGroup, s.getGroup(), s.getPredecessorGroup()));
	}

	private void processMerge(Endpoint source, Merge message) {
		StaticGroup oldGroup = s.getGroup();
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
		upperLayer.processEvent(source,
				new MergeIDUpdate(oldGroup, s.getGroup(), s.getPredecessorGroup()));
	}

	private void processKillEvent(Endpoint source, KillEvent message) {
		super.kill();
	}
}

class WorkerNodeInternalState {
	private StaticGroup group;
	private StaticGroup successorGroup;
	private StaticGroup predecessorGroup;

	public StaticGroup getGroup() {
		return group;
	}

	public void setGroup(StaticGroup group) {
		this.group = group;
	}

	public StaticGroup getSuccessorGroup() {
		return successorGroup;
	}

	public void setSuccessorGroup(StaticGroup successorGroup) {
		this.successorGroup = successorGroup;
	}

	public StaticGroup getPredecessorGroup() {
		return predecessorGroup;
	}

	public Identifier getPredecessorId() {
		return predecessorGroup != null ? predecessorGroup.getID() : null;
	}

	public Identifier getSuccessorId() {
		return successorGroup != null ? successorGroup.getID() : null;
	}

	public void setPredecessorGroup(StaticGroup predecessorGroup) {
		this.predecessorGroup = predecessorGroup;
	}
}
