package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.Event;

public class Merge extends Event {
	private static final long serialVersionUID = 6845537627546612803L;

	private final StaticGroup predecessorGroup;
	private final StaticGroup smallGroup;
	private final StaticGroup successorGroup;
	private final StaticGroup largeGroup;

	public Merge(StaticGroup smallGroup, StaticGroup largeGroup,
			StaticGroup predecessorGroup, StaticGroup successorGroup) {
		this.smallGroup = smallGroup;
		this.largeGroup = largeGroup;
		this.successorGroup = successorGroup;
		this.predecessorGroup = predecessorGroup;
	}

	public StaticGroup getSmallGroup() {
		return smallGroup;
	}

	public StaticGroup getSuccessorGroup() {
		return successorGroup;
	}

	public StaticGroup getLargeGroup() {
		return largeGroup;
	}

	public StaticGroup getPredecessorGroup() {
		return predecessorGroup;
	}
}
