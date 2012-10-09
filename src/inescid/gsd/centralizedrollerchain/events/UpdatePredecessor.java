package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.Event;

public class UpdatePredecessor extends Event {

	private static final long serialVersionUID = 2476986654225613809L;
	private final StaticGroup newGroup;
	private final Identifier oldGroup;

	public UpdatePredecessor(StaticGroup staticGroup, Identifier oldGroup) {
		newGroup = staticGroup;
		this.oldGroup = oldGroup;
	}

	public StaticGroup getNewGroup() {
		return newGroup;
	}

	public Identifier getOldGroup() {
		return oldGroup;
	}
}
