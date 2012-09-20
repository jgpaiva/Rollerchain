package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.transport.Endpoint;

import java.util.Set;

public class SetNeighbours extends Event {
	private final Set<Endpoint> group;
	private final Set<Endpoint> predGroup;
	private final Set<Endpoint> succGroup;

	public SetNeighbours(Set<Endpoint> group, Set<Endpoint> predGroup,
			Set<Endpoint> succGroup) {
		this.group = group;
		this.predGroup = predGroup;
		this.succGroup = succGroup;
	}

	public Set<Endpoint> getGroup() {
		return group;
	}

	public Set<Endpoint> getSuccessorGroup() {
		return succGroup;
	}

	public Set<Endpoint> getPredecessorGroup() {
		return predGroup;
	}
}
