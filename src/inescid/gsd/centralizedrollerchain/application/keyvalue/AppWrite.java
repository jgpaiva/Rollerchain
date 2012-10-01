package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class AppWrite extends UpperLayerMessage {
	private static final long serialVersionUID = 6910042513493956107L;
	private final Key key;
	private final int size;

	public AppWrite(Key k, int size) {
		key = k;
		this.size = size;
	}

	public Key getKey() {
		return key;
	}

	public int getSize() {
		return size;
	}

}
