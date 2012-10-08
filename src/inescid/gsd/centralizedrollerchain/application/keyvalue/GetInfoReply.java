package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class GetInfoReply extends UpperLayerMessage {
	private static final long serialVersionUID = 3871969048205446146L;
	private final Key[] keys;

	public GetInfoReply(Key[] keys) {
		this.keys = keys;
	}

	public Key[] getKeys() {
		return keys;
	}

	@Override
	public String toString() {
		return super.toString() + " for " + keys.length + " keys";
	}
}
