package inescid.gsd.centralizedrollerchain;

import inescid.gsd.transport.Endpoint;

import java.util.TreeSet;

public class StaticGroup extends TreeSet<Endpoint> {
	private static final long serialVersionUID = 4251391059953853138L;
	private final Identifier id;

	public StaticGroup(Group group) {
		id = group.getID();
		addAll(group.getFinger());
	}

	public Identifier getID() {
		return id;
	}
}
