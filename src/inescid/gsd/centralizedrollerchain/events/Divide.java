package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.Event;

public class Divide extends Event {
	private static final long serialVersionUID = -7125443993296880262L;
	private final StaticGroup newGroup;
	private final StaticGroup oldGroup;

	public Divide(StaticGroup newGroup, StaticGroup oldGroup) {
		this.newGroup = newGroup;
		this.oldGroup = oldGroup;
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
