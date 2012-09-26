package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class JoinedNetwork extends UpperLayerMessage {
	private static final long serialVersionUID = 2082148085852330320L;

	private final StaticGroup group;

	private final Identifier predecessor;

	public JoinedNetwork(StaticGroup group, Identifier identifier) {
		this.group = group;
		predecessor = identifier;
	}

	public StaticGroup getGroup() {
		return group;
	}

	public Identifier getPredecessorID() {
		return predecessor;
	}
}
