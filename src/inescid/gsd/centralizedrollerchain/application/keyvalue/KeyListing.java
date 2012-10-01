package inescid.gsd.centralizedrollerchain.application.keyvalue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class KeyListing implements Serializable, Iterable<Key> {
	private static final long serialVersionUID = 8366999392427885779L;
	private final Key[] listing;

	public KeyListing(TreeSet<Key> keys) {
		listing = keys.toArray(new Key[keys.size()]);
	}

	public KeyStorage getKeyStorage() {
		return new KeyStorage(listing);
	}

	@Override
	public Iterator<Key> iterator() {
		return asList().iterator();
	}

	public List<Key> asList() {
		return Arrays.asList(listing);
	}
}
