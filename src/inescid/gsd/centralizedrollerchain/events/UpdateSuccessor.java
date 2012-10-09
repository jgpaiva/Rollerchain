package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.Event;

public class UpdateSuccessor extends Event {

	private static final long serialVersionUID = 1970044103419853707L;
	private final Identifier oldGroup;
	private final StaticGroup newGroup;

	public UpdateSuccessor(StaticGroup newGroup, Identifier oldGroup) {
		this.newGroup = newGroup;
		this.oldGroup = oldGroup;
	}

	public Identifier getOldGroup() {
		return oldGroup;
	}

	public StaticGroup getNewGroup() {
		return newGroup;
	}

}
