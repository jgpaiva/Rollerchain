package inescid.gsd.centralizedrollerchain.application.keyvalue.flux;

import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyValueStore;

public class Flux {
	private final KeyValueStore owner;
	private final int id;
	private static int counter = 0;

	public Flux(KeyValueStore keyValueStore) {
		owner = keyValueStore;
		id = Flux.counter++;
	}

	public KeyValueStore getOwner() {
		return owner;
	}

	public int getId() {
		return id;
	}
}
