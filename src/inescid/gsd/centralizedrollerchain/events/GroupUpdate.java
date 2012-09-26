package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class GroupUpdate extends UpperLayerMessage {
	private static final long serialVersionUID = 4429123280680581309L;
	private final StaticGroup oldGroup;
	private final StaticGroup currentGroup;
	private final Identifier predecessor;

	public GroupUpdate(StaticGroup oldGroup, StaticGroup group, Identifier predecessor) {
		this.oldGroup = oldGroup;
		currentGroup = group;
		this.predecessor = predecessor;
	}

	public StaticGroup getOldGroup() {
		return oldGroup;
	}

	public StaticGroup getCurrentGroup() {
		return currentGroup;
	}

	public Identifier getPredecessor() {
		return predecessor;
	}

}
