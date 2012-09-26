package inescid.gsd.centralizedrollerchain.application.keyvalue.events;

import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class GetAllKeys extends UpperLayerMessage {
	private static final long serialVersionUID = 8007097369918437701L;
	private final Identifier id;
	private final Identifier predecessor;

	public GetAllKeys(Identifier id, Identifier predecessor) {
		this.id = id;
		this.predecessor = predecessor;
	}

	public Identifier getID() {
		return id;
	}

	public Identifier getPredecessorID() {
		return predecessor;
	}
}
