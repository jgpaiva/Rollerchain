package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.Event;

public class SetNeighbours extends Event {
	private static final long serialVersionUID = 1851878886720999187L;

	private final StaticGroup group;
	private final StaticGroup predGroup;
	private final StaticGroup succGroup;

	public SetNeighbours(StaticGroup group, StaticGroup predGroup,
			StaticGroup succGroup) {
		this.group = group;
		this.predGroup = predGroup;
		this.succGroup = succGroup;
	}

	public StaticGroup getGroup() {
		return group;
	}

	public StaticGroup getSuccessorGroup() {
		return succGroup;
	}

	public StaticGroup getPredecessorGroup() {
		return predGroup;
	}

	@Override
	public String toString() {
		return super.toString() + ":" + "myGroup:" + group + " preds:" + predGroup + " succs:"
				+ succGroup;
	}
}
