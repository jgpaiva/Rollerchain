package inescid.gsd.centralizedrollerchain.application.keyvalue.events;

import inescid.gsd.centralizedrollerchain.Identifier;

public class AllKeysRequest extends UniqueIDMessage {
	private static final long serialVersionUID = 8007097369918437701L;
	private final Identifier groupID;
	private final Identifier predecessor;

	public AllKeysRequest(Identifier groupID, Identifier predecessor) {
		super();
		this.groupID = groupID;
		this.predecessor = predecessor;
	}

	public Identifier getGroupID() {
		return groupID;
	}

	public Identifier getPredecessorID() {
		return predecessor;
	}

	@Override
	public String toString() {
		return super.toString() + " for group " + groupID + " with predecessor " + predecessor;
	}
}
