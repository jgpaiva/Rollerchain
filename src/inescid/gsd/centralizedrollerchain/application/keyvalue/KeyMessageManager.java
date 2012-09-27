package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Configuration;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.AllKeysRequest;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.KeysRequest;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.UniqueIDMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class KeyMessageManager {
	private int allKeysRequested;
	private final ArrayList<HashSet<UniqueIDMessage>> messages;
	private final HashMap<Long, UniqueIDMessage> messageMap;

	public KeyMessageManager() {
		allKeysRequested = 0;
		messages = new ArrayList<HashSet<UniqueIDMessage>>();
		for (int it = 0; it < Configuration.getRoundsToKeepMessages(); it++)
			messages.add(new HashSet<UniqueIDMessage>());
		messageMap = new HashMap<Long, UniqueIDMessage>();
	}

	public void registerMessage(UniqueIDMessage requestMessage) {
		messageMap.put(requestMessage.getMessageID(), requestMessage);
		messages.get(0).add(requestMessage);

		if (requestMessage instanceof AllKeysRequest)
			allKeysRequested++;
	}

	public void deRegisterMessage(long messageID) {
		UniqueIDMessage message = messageMap.remove(messageID);
		if (message != null) {
			for (HashSet<UniqueIDMessage> it : messages)
				it.remove(message);
			if (message instanceof AllKeysRequest)
				allKeysRequested--;
		}
	}

	public void removeRequestedKeys(KeyStorage otherKeys) {
		if (allKeysRequested > 0)
			otherKeys.clear();
		else
			for (UniqueIDMessage it : messageMap.values())
				if (it instanceof KeysRequest)
					otherKeys.removeAll(((KeysRequest) it).getToRequest().asList());
	}

	public void nextRound() {
		Collections.rotate(messages, 1);
		for (UniqueIDMessage it : messages.get(0)) {
			messageMap.remove(it.getMessageID());
			if (it instanceof AllKeysRequest)
				allKeysRequested--;
		}
		messages.get(0).clear();
	}
}