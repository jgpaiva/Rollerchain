package inescid.gsd.centralizedrollerchain.application.keyvalue.events;

import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class UniqueIDMessage extends UpperLayerMessage {
	private static final long serialVersionUID = 94291279139157399L;
	private final long messageID;
	private static long counter = 0;

	UniqueIDMessage() {
		messageID = UniqueIDMessage.counter++;
	}

	public long getMessageID() {
		return messageID;
	}
}
