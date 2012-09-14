package inescid.gsd.rollerchain.events;

import inescid.gsd.rollerchain.interfaces.Event;
import inescid.gsd.rollerchain.interfaces.EventReceiver;

import java.util.Set;

public class SetNeighbours extends Event {
	private Set<EventReceiver> group;
	private Set<EventReceiver> predGroup;
	private Set<EventReceiver> succGroup;

	public SetNeighbours(Set<EventReceiver> group, Set<EventReceiver> predGroup,
			Set<EventReceiver> succGroup) {
		this.group = group;
		this.predGroup = predGroup;
		this.succGroup = succGroup;
	}

	public Set<EventReceiver> getGroup() {
		return this.group;
	}

	public Set<EventReceiver> getSuccessorGroup() {
		return this.succGroup;
	}

	public Set<EventReceiver> getPredecessorGroup() {
		return this.predGroup;
	}
}
