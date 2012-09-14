package inescid.gsd.rollerchain.events;

import inescid.gsd.rollerchain.interfaces.Event;
import inescid.gsd.rollerchain.interfaces.EventReceiver;

import java.util.TreeSet;

public class Merge extends Event {
	private final TreeSet<EventReceiver> smallGroup;
	private final TreeSet<EventReceiver> successorGroup;

	public Merge(TreeSet<EventReceiver> smallGroup, TreeSet<EventReceiver> successorGroup) {
		this.smallGroup = smallGroup;
		this.successorGroup = successorGroup;
	}

	public TreeSet<EventReceiver> getSmallGroup() {
		return this.smallGroup;
	}

	public TreeSet<EventReceiver> getSuccessorGroup() {
		return this.successorGroup;
	}
}
