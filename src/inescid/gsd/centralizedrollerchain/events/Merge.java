package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.transport.Endpoint;

import java.util.Set;

public class Merge extends Event {
	private static final long serialVersionUID = 6845537627546612803L;

	private final Set<Endpoint> predecessorGroup;
	private final Set<Endpoint> smallGroup;
	private final Set<Endpoint> successorGroup;
	private final Set<Endpoint> largeGroup;

	public Merge(Set<Endpoint> smallGroup, Set<Endpoint> largeGroup,
			Set<Endpoint> predecessorGroup, Set<Endpoint> successorGroup) {
		this.smallGroup = smallGroup;
		this.largeGroup = largeGroup;
		this.successorGroup = successorGroup;
		this.predecessorGroup = predecessorGroup;
	}

	public Set<Endpoint> getSmallGroup() {
		return smallGroup;
	}

	public Set<Endpoint> getSuccessorGroup() {
		return successorGroup;
	}

	public Set<Endpoint> getLargeGroup() {
		return largeGroup;
	}

	public Set<Endpoint> getPredecessorGroup() {
		return predecessorGroup;
	}
}
