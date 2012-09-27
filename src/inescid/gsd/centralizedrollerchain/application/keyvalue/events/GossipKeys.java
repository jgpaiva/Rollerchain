package inescid.gsd.centralizedrollerchain.application.keyvalue.events;

import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyListing;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class GossipKeys extends UpperLayerMessage {

	private static final long serialVersionUID = 4969900043715305232L;
	private final Identifier predecessorID;
	private final Identifier id;
	private final KeyListing keyListing;

	public GossipKeys(Identifier id, Identifier predecessorID, KeyListing keyListing) {
		this.id = id;
		this.predecessorID = predecessorID;
		this.keyListing = keyListing;
	}

	public Identifier getId() {
		return id;
	}

	public Identifier getPredecessorID() {
		return predecessorID;
	}

	public KeyListing getKeyListing() {
		return keyListing;
	}
}
