package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.application.keyvalue.Key;
import inescid.gsd.centralizedrollerchain.events.Merge;
import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.utils.Utils;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

public class Group {
	private static final long serialVersionUID = 4251391059953853138L;
	private static final Logger logger = Logger.getLogger(Group.class.getName());

	private final TreeSet<Endpoint> finger;
	private final Identifier id;

	private Group successor;
	private Group predecessor;
	private ScheduledFuture<?> schedule;
	private boolean active;
	private final MasterNode owner;
	private TreeSet<Key> keys;

	Group(MasterNode owner, Identifier id, TreeSet<Endpoint> members) {
		active = true;
		this.id = id;
		this.owner = owner;
		finger = members;

		successor = null;
		predecessor = null;
		schedule = null;
	}

	public void setSchedule(ScheduledFuture<?> schedule) {
		if (this.schedule != null)
			Node.die("Cannot set schedule more than once!");

		this.schedule = schedule;
	}

	@SuppressWarnings("unchecked")
	void merge() {
		if (owner.s.getAllGroupsSize() == 1)
			return;
		assert (successor != null);

		StaticGroup smallGroup = getStaticGroup();
		StaticGroup successorGroup = successor.getStaticGroup();

		owner.s.removeFromAllGroups(this);
		owner.moveAllFrom(this, successor, finger);
		for (Endpoint it : finger)
			successor.addNode(it);

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
					getSuccessor(), getPredecessor()));
	}

	void divide(Group newGroup) {
		int futureGroupSize = finger.size() - (finger.size() / 2);
		while (finger.size() > futureGroupSize)
			newGroup.addNode(Utils.removeRandomEl(getFinger()));

		owner.moveAllFrom(this, newGroup, newGroup.finger);

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
	}

	void addNode(Endpoint node) {
		finger.add(node);
	}

	public void removeNode(Endpoint source) {
		if (!finger.remove(source))
			Node.die("Endpoint " + source + " was not in finger!" + this);
	}

	private void sendMessage(Endpoint dest, Event message) {
		owner.sendMessage(dest, message);
	}

	private void cancelSchedule() {
		if (getFinger().size() > 0) {
			Node.die("canceling a schedule for a group with nodes: + this");
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

	public StaticGroup getPredecessor() {
		return predecessor != null ? predecessor.getStaticGroup() : null;
	}

	public StaticGroup getSuccessor() {
		return successor != null ? successor.getStaticGroup() : null;
	}

	public TreeSet<Key> getKeys() {
		return keys;
	}

	public void setKeys(TreeSet<Key> keys) {
		this.keys = keys;
	}

	private int getKeySize() {
		return keys != null ? keys.size() : 0;
	}

	@Override
	public String toString() {
		return getID() + ":" + getKeySize() + " " + finger + "" + (active ? "A" : "I");
	}
}
