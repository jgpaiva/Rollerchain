package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.transport.Endpoint;

import java.util.TreeSet;

public class Merge extends Event {
	private static final long serialVersionUID = 6845537627546612803L;

	private final TreeSet<Endpoint> smallGroup;
	private final TreeSet<Endpoint> successorGroup;

	public Merge(TreeSet<Endpoint> smallGroup, TreeSet<Endpoint> successorGroup) {
		this.smallGroup = smallGroup;
		this.successorGroup = successorGroup;
	}

	public TreeSet<Endpoint> getSmallGroup() {
		return smallGroup;
	}

	public TreeSet<Endpoint> getSuccessorGroup() {
		return successorGroup;
	}
}
