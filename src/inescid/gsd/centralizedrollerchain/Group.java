package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.events.Divide;
import inescid.gsd.centralizedrollerchain.events.Merge;
import inescid.gsd.centralizedrollerchain.events.SetNeighbours;
import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.utils.Utils;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Group {
	private static final long serialVersionUID = 4251391059953853138L;
	private static final Logger logger = Logger.getLogger(Group.class.getName());

	private final TreeSet<Endpoint> finger;
	private final Identifier id;

	private Group successor;
	private Group predecessor;
	private ScheduledFuture<?> schedule;
	private final boolean active;
	private final MasterNode owner;

	Group(MasterNode owner,Identifier id,  TreeSet<Endpoint> members) {
		active = true;
		this.id = id;
		this.owner = owner;
		finger = members;

		successor = null;
		predecessor = null;
		schedule = null;
	}

	public void setSchedule(ScheduledFuture<?> schedule) {
		if(this.schedule != null)
			Node.die("Cannot set schedule more than once!");
		
		this.schedule = schedule;
	}

	@SuppressWarnings("unchecked")
	void merge() {
		if (MasterNode.getAllGroupsSize() == 1) return;
		assert (successor != null);

		StaticGroup smallGroup = getStaticGroup();
		StaticGroup successorGroup = successor.getStaticGroup();

		MasterNode.removeFromAllGroups(this);
		moveAllFrom(this, successor, finger);
		finger.clear();
		cancelSchedule();

		// int oldKeys = this.keys;
		// int successorKeys = this.successor.keys;
		// this.successor.keys = successorKeys + oldKeys;
		// assert (this.keys >= 0);
		// assert (this.successor.keys >= 0) : this.successor.keys + " " +
		// this.keys;
		if (predecessor == successor) {
			successor.predecessor = null;
			successor.successor = null;
		} else {
			predecessor.successor = successor;
			successor.predecessor = predecessor;
		}
		for (Endpoint it : successor.finger)
			sendMessage(it, new Merge(smallGroup, successorGroup,
					getSuccessor(),getPredecessor()));
	}

	void divide(Group newGroup) {
		final int initialSize = finger.size();
		TreeSet<Endpoint> setNew = new TreeSet<Endpoint>();
		TreeSet<Endpoint> oldGroup = new TreeSet<Endpoint>();
		int newSize = initialSize / 2;
		int oldSize = initialSize - newSize;

		assert (oldSize >= newSize) : oldSize + " " + newSize + " " +
				finger.size();
		assert (oldSize >= 0) : oldSize + " " + newSize;
		// assert (this.keys >= 0);
		// int newKeys = (int) (this.keys / (((double) initialSize) /
		// ((double)
		// newSize)));
		// int oldKeys = this.keys - newKeys;
		// assert (newKeys > 0) : newKeys + " " + initialSize + " " +
		// newSize +
		// " " + this.keys;
		// assert (oldKeys > 0) : oldKeys;

		//new group will be smaller or equal to this
		int futureGroupSize = finger.size() - (finger.size() / 2);
		while (finger.size() > futureGroupSize)
			setNew.add(Utils.removeRandomEl(getFinger()));

		moveAllFrom(this, newGroup, newGroup.finger);

		if (predecessor != null) {
			newGroup.predecessor = predecessor;
			predecessor.successor = newGroup;
			predecessor = newGroup;
			newGroup.successor = this;
		} else {
			predecessor = newGroup;
			successor = newGroup;
			newGroup.predecessor = this;
			newGroup.successor = this;
		}
		// newGroup.keys = newKeys;
		// this.keys = oldKeys;

		for (Endpoint it : newGroup.finger)
			sendMessage(it, new Divide(newGroup.getStaticGroup(), getStaticGroup()));
		for (Endpoint it : finger)
			sendMessage(it, new Divide(newGroup.getStaticGroup(), getStaticGroup()));
	}

	void addNode(Endpoint node) {
		finger.add(node);
		for (Endpoint it : finger)
			sendMessage(node, new SetNeighbours(getStaticGroup(), getPredecessor(),getSuccessor()));
	}

	public void removeNode(Endpoint source) {
		if (!finger.remove(source))
			Group.logger.log(Level.SEVERE, "Endpoint " + source + " was not in finger!" + this);
	}

	private void moveAllFrom(Group group, Group newGroup, TreeSet<Endpoint> toMove) {
		for (Endpoint it : toMove) {
			Group res = s.addToWorkerList(it, newGroup);
			if (res != group)
				Group.logger.log(Level.SEVERE, "Node was in wrong group! Should be in " + this
						+ " but was in " + res);
		}
	}

	private void sendMessage(Endpoint dest, Event Message) {
		// TODO: finish me!
	}
	
	private void createGroup(TreeSet<Endpoint>, Identifier id){
		// TODO: finish me!
	}


	private void cancelSchedule() {
		if (getFinger().size() > 0) {
			Group.logger.log(Level.SEVERE, "canceling a schedule for a group with nodes: + this");
			Thread.dumpStack();
		}
		schedule.cancel(false);
		active = false;
	}
	
	public int size() {
		return finger.size();
	}

	public Identifier getID() {
		return id;
	}

	public Collection<? extends Endpoint> getFinger() {
		return finger;
	}

	public StaticGroup getStaticGroup() {
		return new StaticGroup(this);
	}
	
	public StaticGroup getPredecessor(){
		return predecessor!=null? predecessor.getStaticGroup():null;
	}
	
	public StaticGroup getSuccessor(){
		return successor!=null? successor.getStaticGroup():null;
	}

	@Override
	public String toString() {
		return
		// this.keys +
		"" + finger + "" + (active ? "A" : "I");
	}
}
