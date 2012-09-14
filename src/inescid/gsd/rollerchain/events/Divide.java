package inescid.gsd.rollerchain.events;

import inescid.gsd.rollerchain.interfaces.Event;
import inescid.gsd.rollerchain.interfaces.EventReceiver;

import java.util.TreeSet;

public class Divide extends Event {
	private final TreeSet<EventReceiver> newGroup;
	private final TreeSet<EventReceiver> oldGroup;

	public Divide(TreeSet<EventReceiver> newGroup, TreeSet<EventReceiver> oldGroup) {
		this.newGroup = newGroup;
		this.oldGroup = oldGroup;
	}

	public TreeSet<EventReceiver> getNewGroup() {
		return this.newGroup;
	}

	public TreeSet<EventReceiver> getOldGroup() {
		return this.oldGroup;
	}

}
