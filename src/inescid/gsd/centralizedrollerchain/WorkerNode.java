package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.events.Divide;
import inescid.gsd.centralizedrollerchain.events.DivideIDUpdate;
import inescid.gsd.centralizedrollerchain.events.GetInfo;
import inescid.gsd.centralizedrollerchain.events.GroupUpdate;
import inescid.gsd.centralizedrollerchain.events.JoinedNetwork;
import inescid.gsd.centralizedrollerchain.events.Merge;
import inescid.gsd.centralizedrollerchain.events.MergeIDUpdate;
import inescid.gsd.centralizedrollerchain.events.SetNeighbours;
import inescid.gsd.centralizedrollerchain.events.UpdatePredecessor;
import inescid.gsd.centralizedrollerchain.events.UpdateSuccessor;
import inescid.gsd.centralizedrollerchain.events.WorkerInit;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayer;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;
import inescid.gsd.centralizedrollerchain.internalevents.KillEvent;
import inescid.gsd.centralizedrollerchain.utils.FileOutput;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.utils.Utils;

import java.util.logging.Level;

public class WorkerNode extends Node {
	private final Endpoint masterEndpoint;

	private final WorkerNodeInternalState s = new WorkerNodeInternalState();

	private final UpperLayer upperLayer;

	private final FileOutput writer;

	public WorkerNode(Endpoint endpoint, Endpoint masterEndpoint) {
		super(endpoint);
		this.masterEndpoint = masterEndpoint;
		if (masterEndpoint == null)
			Node.die("Master endpoint is null");
		upperLayer = new UpperLayer() {
			@Override
			public void processEvent(Endpoint source, Object message) {
				// discard event
			}

			@Override
			public void init(Node n) {
				// discard init
			}

			@Override
			public void nextRound() {
				// discard next round
			}
		};
		writer = new FileOutput(endpoint, this.getClass());
	}

	public WorkerNode(Endpoint endpoint, Endpoint masterEndpoint, UpperLayer upperLayer) {
		super(endpoint);
		this.masterEndpoint = masterEndpoint;
		if (masterEndpoint == null)
			Node.die("Master endpoint is null");
		this.upperLayer = upperLayer;
		writer = new FileOutput(endpoint, this.getClass());
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
		else if (message instanceof GetInfo)
			processGetInfo(source, (GetInfo) message);
		else if (message instanceof UpperLayerMessage)
			upperLayer.processEvent(source, message);
		else if (message instanceof DeathNotification)
			upperLayer.processEvent(source, message);
		else if (message instanceof UpdatePredecessor)
			processUpdatePredecessor(source, (UpdatePredecessor) message);
		else if (message instanceof UpdateSuccessor)
			processUpdateSuccessor(source, (UpdateSuccessor) message);
		else
			Node.logger.log(Level.SEVERE, "Received unknown event: " + message);
	}

	@Override
	public void init() {
		super.init();
		upperLayer.init(this);
		sendMessage(masterEndpoint, new WorkerInit());
	}

	@Override
	public void nextRound() {
		upperLayer.nextRound();
		writer.status("group: " + getGroup() + " pred:" + getPredecessor() + " succ:" + getSuccessor());
	}

	private void processSetNeighbours(Endpoint source, SetNeighbours setNeighbours) {
		StaticGroup oldGroup = s.getGroup();
		s.setGroup(setNeighbours.getGroup());
		s.setSuccessorGroup(setNeighbours.getSuccessorGroup());
		s.setPredecessorGroup(setNeighbours.getPredecessorGroup());

		if (oldGroup == null)
			upperLayer.processEvent(source,
					new JoinedNetwork(setNeighbours.getGroup(), s.getPredecessorID()));
		else {
			if (!s.getGroup().getID().equals(setNeighbours.getGroup().getID()))
				Node.die("Should never happen");

			upperLayer.processEvent(source,
					new GroupUpdate(oldGroup, setNeighbours.getGroup(), s.getPredecessorID()));
		}
		upperLayer.processEvent(source, new GetInfo());
	}

	private void processDivide(Endpoint source, Divide message) {
		testIntegrity();
		StaticGroup oldGroup = s.getGroup();

		if (message.getNewGroup().contains(endpoint)) {
			s.setGroup(message.getNewGroup());
			s.setSuccessorGroup(message.getOldGroup());
			if (s.getPredecessorGroup() == null)
				s.setPredecessorGroup(message.getOldGroup());

			if ((s.getPredecessorID() != null) && (message.getPredecessor() != null)
					&& !s.getPredecessorID().equals(message.getPredecessor().getID()))
				Node.die("for group " + getGroup() + "predecessors differ:" + s.getPredecessorID() + " "
						+ message.getPredecessor().getID());
		} else {
			if (!message.getOldGroup().contains(endpoint))
				Node.die("Should never happen: " + endpoint + " not in:" + message);
			s.setGroup(message.getOldGroup());
			s.setPredecessorGroup(message.getNewGroup());
			if (s.getSuccessorGroup() == null)
				s.setSuccessorGroup(message.getNewGroup());

			if ((s.getSuccessorID() != null) && (message.getSuccessor() != null)
					&& !s.getSuccessorID().equals(message.getSuccessor().getID()))
				Node.die("for group " + getGroup() + "successors differ:" + s.getSuccessorID() + " "
						+ message.getSuccessor().getID());
		}

		upperLayer.processEvent(source,
				new DivideIDUpdate(oldGroup, s.getGroup(), s.getPredecessorGroup()));
		upperLayer.processEvent(source, new GetInfo());

		Node.logger.log(Level.FINEST, "Divided from: " + oldGroup + " to " + getGroup());
		testIntegrity();
	}

	public void testIntegrity() {
		if ((getPredecessorID() != null) && (getSuccessorID() != null)
				&& !(getSuccessorID().equals(getPredecessorID()))
				&& !Identifier.isBetween(getGroup().getID(), getPredecessorID(), getSuccessorID()))
			Node.die(getGroup().getID() + " is not between " + getPredecessorID() + " and "
					+ getSuccessorID());
	}

	private void processMerge(Endpoint source, Merge message) {
		testIntegrity();
		StaticGroup oldGroup = s.getGroup();

		if (message.getSmallGroup().contains(endpoint)) {
			s.setSuccessorGroup(message.getSuccessorGroup());
			if ((s.getPredecessorID() != null) && (message.getPredecessorGroup() != null)
					&& !s.getPredecessorID().equals(message.getPredecessorGroup().getID()))
				Node.die("for group " + oldGroup + "predecessors differ:" + s.getPredecessorID() + " "
						+ message.getPredecessorGroup().getID());
			if (s.getSuccessorGroup() == null)
				s.setPredecessorGroup(null);
		}
		else if (message.getLargeGroup().contains(endpoint)) {
			s.setPredecessorGroup(message.getPredecessorGroup());
			if ((s.getSuccessorID() != null) && (message.getSuccessorGroup() != null)
					&& !s.getSuccessorID().equals(message.getSuccessorGroup().getID()))
				Node.die("for group " + oldGroup + " successors differ:" + s.getSuccessorID() + " "
						+ message.getSuccessorGroup().getID());
			if (s.getPredecessorGroup() == null)
				s.setSuccessorGroup(null);
		} else
			Node.die("should never be reached");

		StaticGroup tempGroup = message.getLargeGroup();
		tempGroup.addAll(message.getSmallGroup());
		s.setGroup(tempGroup);

		upperLayer.processEvent(source,
				new MergeIDUpdate(oldGroup, s.getGroup(), s.getPredecessorGroup()));
		upperLayer.processEvent(source, new GetInfo());

		testIntegrity();
	}

	private void processKillEvent(Endpoint source, KillEvent message) {
		super.kill(source);
	}

	private void processGetInfo(Endpoint source, GetInfo message) {
		// I have no info to return
		upperLayer.processEvent(source, message);
	}

	private void processUpdateSuccessor(Endpoint source, UpdateSuccessor message) {
		if (!Utils.testEquals(getSuccessorID(), message.getOldGroup()))
			Node.die("successor " + getSuccessorID() + " != " + message.getOldGroup());
		s.setSuccessorGroup(message.getNewGroup());
	}

	private void processUpdatePredecessor(Endpoint source, UpdatePredecessor message) {
		if (!Utils.testEquals(getPredecessorID(), message.getOldGroup()))
			Node.die("predecessor " + getPredecessorID() + " != " + message.getOldGroup());
		s.setPredecessorGroup(message.getNewGroup());
	}

	public StaticGroup getGroup() {
		return s.getGroup();
	}

	public Identifier getPredecessorID() {
		return s.getPredecessorID();
	}

	public Identifier getSuccessorID() {
		return s.getSuccessorID();
	}

	public StaticGroup getPredecessor() {
		return s.getPredecessorGroup();
	}

	public StaticGroup getSuccessor() {
		return s.getSuccessorGroup();
	}
}

class WorkerNodeInternalState {
	private StaticGroup group = null;
	private StaticGroup successorGroup = null;
	private StaticGroup predecessorGroup = null;

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

	public Identifier getPredecessorID() {
		return predecessorGroup != null ? predecessorGroup.getID() : null;
	}

	public Identifier getSuccessorID() {
		return successorGroup != null ? successorGroup.getID() : null;
	}

	public void setPredecessorGroup(StaticGroup predecessorGroup) {
		this.predecessorGroup = predecessorGroup;
	}
}
