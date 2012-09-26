package inescid.gsd.centralizedrollerchain.application.keyvalue.events;

import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyContainer;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class KeyMessage extends UpperLayerMessage {

	private static final long serialVersionUID = 699661673403097461L;
	private final KeyContainer container;

	public KeyMessage(KeyContainer container) {
		this.container = container;
	}

	public KeyContainer getContainer() {
		return container;
	}

}
