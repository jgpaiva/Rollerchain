package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyStorage;
import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyValueStore;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.utils.Utils;

import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Group {
	private static final Logger logger = Logger.getLogger(Group.class.getName());

	private final TreeSet<Endpoint> finger;
	private final Identifier id;

	private Group successor;
	private Group predecessor;
	private ScheduledFuture<?> schedule;
	private boolean active;
	private final MasterNode owner;
	private KeyStorage keys;

	Group(MasterNode owner, Identifier id, TreeSet<Endpoint> members) {
		active = true;
		this.id = id;
		this.owner = owner;
		finger = members;

		successor = null;
		predecessor = null;
		schedule = null;

		if (this.id == null)
			throw new RuntimeException("Should never happen!");
	}

	public void setSchedule(ScheduledFuture<?> schedule) {
		if (this.schedule != null)
			Node.die("Cannot set schedule more than once!");

		this.schedule = schedule;
	}

	Group merge() {
		if (owner.s.getAllGroupsSize() == 1)
			return null;
		assert (successor != null);

		Group.testIntegrity(this);
		Group.testIntegrity(successor);

		owner.s.removeFromAllGroups(this);
		owner.moveAllFrom(this, successor, finger);

		for (Endpoint it : finger)
			successor.addNode(it);
		finger.clear();

		cancelSchedule();

		if (predecessor.equals(successor)) {
			successor.predecessor = null;
			successor.successor = null;
		} else {
			predecessor.successor = successor;
			successor.predecessor = predecessor;
		}
		successor.getKeys().addAll(keys);

		Group.testIntegrity(successor);

		return successor;
	}

	void divide(Group newGroup) {
		if ((getPredecessorID() != null)
				&& !Identifier.isBetween(newGroup.getID(), getPredecessorID(), getID()))
			KeyValueStore.die(newGroup.getID() + " is not between " + getPredecessorID() + " and " + getID());
		Group.testIntegrity(this);

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

		if (!getSuccessorID().equals(newGroup.getID())
				&& !Identifier.isBetween(newGroup.getID(), getSuccessorID(), getID()))
			KeyValueStore.die(newGroup.getID() + " is not between " + getSuccessorID() + " and " + getID());

		keys.filter(getStaticGroup(), getPredecessorID());
		Group.testIntegrity(this);
		Group.testIntegrity(newGroup);
	}

	public static void testIntegrity(Group group) {
		if ((group.getPredecessorID() != null) && (group.getSuccessorID() != null))
			if (group.getPredecessorID().equals(group.getSuccessorID()))
				Group.logger.log(Level.WARNING, "group " + group + " has equal successor and predecessor: "
						+ group.getPredecessorID() + "   AT: "
						+ Utils.stackTraceFormat());
			else if (!Identifier.isBetween(group.getID(), group.getPredecessorID(), group.getSuccessorID()))
				Node.die(group.getID() + " is not between " + group.getPredecessorID() + " and "
						+ group.getSuccessorID());
		if ((group.predecessor != null) && (group.predecessor.successor != group))
			Node.die(group.getID() + " is different from predecessor " + group.getPredecessorID()
					+ " successor "
					+ group.predecessor.successor.getID());

		if ((group.successor != null) && (group.successor.predecessor != group))
			Node.die(group.getID() + " is different from successor " + group.getSuccessorID() + " successor "
					+ group.successor.predecessor.getID());
	}

	void addNode(Endpoint node) {
		finger.add(node);
	}

	public void removeNode(Endpoint source) {
		if (!finger.remove(source))
			Node.die("Endpoint " + source + " was not in finger!" + this);
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

	public KeyStorage getKeys() {
		return keys;
	}

	public void setKeys(KeyStorage tmp) {
		keys = tmp;
	}

	private int getKeySize() {
		return keys != null ? keys.size() : 0;
	}

	@Override
	public String toString() {
		return getID() + ":" + getKeySize() + " " + finger + "" + (active ? "A" : "I");
	}

	public Identifier getPredecessorID() {
		return getPredecessor() != null ? getPredecessor().getID() : null;
	}

	Identifier getSuccessorID() {
		return getSuccessor() != null ? getSuccessor().getID() : null;
	}
}
