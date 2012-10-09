package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.Event;

public class Merge extends Event {
	private static final long serialVersionUID = 6845537627546612803L;

	private final StaticGroup smallGroup;
	private final StaticGroup largeGroup;
	private final StaticGroup predecessorGroup;
	private final StaticGroup successorGroup;

	public Merge(StaticGroup smallGroup, StaticGroup largeGroup,
			StaticGroup predecessorGroup, StaticGroup successorGroup) {
		this.smallGroup = smallGroup;
		this.largeGroup = largeGroup;
		this.predecessorGroup = predecessorGroup;
		this.successorGroup = successorGroup;
	}

	public StaticGroup getSmallGroup() {
		return smallGroup;
	}

	public StaticGroup getLargeGroup() {
		return largeGroup;
	}

	public StaticGroup getPredecessorGroup() {
		return predecessorGroup;
	}

	public StaticGroup getSuccessorGroup() {
		return successorGroup;
	}

	@Override
	public String toString() {
		return super.toString() + " smallGroup:" + smallGroup + " largeGroup:" + largeGroup
				+ " predecessorGroup:" + predecessorGroup + " successorGroup:" + successorGroup;
	}
}
