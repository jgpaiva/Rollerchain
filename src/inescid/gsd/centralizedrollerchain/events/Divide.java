package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.Event;

public class Divide extends Event {
	private static final long serialVersionUID = -7125443993296880262L;
	private final StaticGroup newGroup;
	private final StaticGroup oldGroup;
	private final StaticGroup successor;
	private final StaticGroup predecessor;

	public Divide(StaticGroup newGroup, StaticGroup oldGroup, StaticGroup predecessor, StaticGroup successor) {
		this.newGroup = newGroup;
		this.oldGroup = oldGroup;
		this.predecessor = predecessor;
		this.successor = successor;
	}

	public StaticGroup getSuccessor() {
		return successor;
	}

	public StaticGroup getPredecessor() {
		return predecessor;
	}

	public StaticGroup getNewGroup() {
		return newGroup;
	}

	public StaticGroup getOldGroup() {
		return oldGroup;
	}

	@Override
	public String toString() {
		return super.toString() + " :" + " newGroup:" + newGroup + " oldGroup:" + oldGroup;
	}
}
