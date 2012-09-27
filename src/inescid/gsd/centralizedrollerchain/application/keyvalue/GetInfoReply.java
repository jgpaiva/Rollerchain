package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.interfaces.Event;

public class GetInfoReply extends Event {
	private static final long serialVersionUID = 3871969048205446146L;
	private final Key[] keys;

	public GetInfoReply(Key[] keys) {
		this.keys = keys;
	}

	public Key[] getKeys() {
		return keys;
	}
}
