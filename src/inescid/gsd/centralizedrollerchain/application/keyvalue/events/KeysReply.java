package inescid.gsd.centralizedrollerchain.application.keyvalue.events;

import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyContainer;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class KeysReply extends UpperLayerMessage {
	private static final long serialVersionUID = 699661673403097461L;

	private final KeyContainer container;

	private final long messageID;

	public KeysReply(KeyContainer container, UniqueIDMessage message) {
		this.container = container;
		messageID = message.getMessageID();
	}

	public KeyContainer getContainer() {
		return container;
	}

	@Override
	public String toString() {
		return super.toString() + " with " + container.size() + " keys";
	}

	public long getMessageID() {
		return messageID;
	}
}
