package inescid.gsd.centralizedrollerchain.application.keyvalue.flux;

import inescid.gsd.centralizedrollerchain.application.keyvalue.AppWrite;
import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyValueStore;
import inescid.gsd.transport.Endpoint;

public class WriteFlux extends Flux {
	private final Endpoint source;
	private final AppWrite message;

	public WriteFlux(KeyValueStore keyValueStore, Endpoint source, AppWrite message) {
		super(keyValueStore);
		this.source = source;
		this.message = message;
	}

	public AppWrite getMessage() {
		return message;
	}

	public Endpoint getSource() {
		return source;
	}

	public void start() {

	}
}
