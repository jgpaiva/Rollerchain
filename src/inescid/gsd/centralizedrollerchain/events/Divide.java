package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.transport.Endpoint;

import java.util.TreeSet;

public class Divide extends Event {
	private static final long serialVersionUID = -7125443993296880262L;
	private final TreeSet<Endpoint> newGroup;
	private final TreeSet<Endpoint> oldGroup;

	public Divide(TreeSet<Endpoint> newGroup, TreeSet<Endpoint> oldGroup) {
		this.newGroup = newGroup;
		this.oldGroup = oldGroup;
	}

	public TreeSet<Endpoint> getNewGroup() {
		return newGroup;
	}

	public TreeSet<Endpoint> getOldGroup() {
		return oldGroup;
	}
}
