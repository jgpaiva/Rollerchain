package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Identifier;

import java.io.Serializable;

public class Key implements Serializable, Comparable<Key> {
	private static final long serialVersionUID = -4812188019768566997L;
	private final int keySize;
	private final Identifier id;

	public Key(Identifier id, int currentKeySize) {
		this.id = id;
		keySize = currentKeySize;
	}

	@Override
	public final boolean equals(Object obj) {
		return obj instanceof Key ? ((Key) obj).id.equals(id)
				: false;
	}

	@Override
	public final int compareTo(Key key) {
		return id.compareTo(key.id);
	}

	@Override
	public final String toString() {
		return "K:" + id;
	}

	@Override
	public final int hashCode() {
		if (id == null)
			return 0;
		return id.hashCode();
	}
}
