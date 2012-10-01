package inescid.gsd.centralizedrollerchain.application.keyvalue.events;

import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyListing;

public class KeysRequest extends UniqueIDMessage {
	private final KeyListing toRequest;
	private static final long serialVersionUID = 1457609400917018551L;

	public KeysRequest(KeyListing toRequest) {
		super();
		this.toRequest = toRequest;
	}

	public KeyListing getRequested() {
		return toRequest;
	}
}
