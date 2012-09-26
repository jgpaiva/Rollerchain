package inescid.gsd.centralizedrollerchain.application.keyvalue;

import java.io.Serializable;
import java.util.TreeSet;

public class KeyContainer implements Serializable {
	private static final long serialVersionUID = 4080646373971888650L;
	private final TreeSet<Key> keys;

	public KeyContainer(TreeSet<Key> keys) {
		this.keys = keys;
	}

	public TreeSet<Key> getKeys() {
		return keys;
	}
}
