package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Configuration;
import inescid.gsd.centralizedrollerchain.Identifier;

import java.util.Random;
import java.util.TreeSet;

public class KeyStorage {
	private final TreeSet<Key> keys;

	public KeyStorage() {
		keys = new TreeSet<Key>();
	}

	/**
	 * Creates the first batch of keys
	 */
	public void init() {
		Random r = new Random(Configuration.getRandomSeed());
		int minKeySize = Configuration.getMinKeySize();
		int maxKeySize = Configuration.getMaxKeySize();
		int idSize = Configuration.getIDSize();
		for (int it = 0; it < Configuration.getNKeys(); it++) {
			int currentKeySize = r.nextInt(maxKeySize - minKeySize) + minKeySize;
			Identifier currentID = new Identifier(idSize, r);
			keys.add(new Key(currentID, currentKeySize));
		}
	}

	@SuppressWarnings("unchecked")
	public KeyContainer getKeys(Identifier lowerID, Identifier higherID) {
		if ((lowerID == null) || (higherID == null))
			return new KeyContainer((TreeSet<Key>) keys.clone());

		TreeSet<Key> toReturn = null;

		Key higher = new Key(higherID, 0);
		Key lower = new Key(lowerID, 0);
		if (lower.compareTo(higher) <= 0)
			toReturn = new TreeSet<Key>(keys.subSet(lower, false, higher,
					true));
		else {
			TreeSet<Key> temp = new TreeSet<Key>(keys.tailSet(lower, false));
			temp.addAll(keys.headSet(higher, true));
			toReturn = temp;
		}
		return new KeyContainer(toReturn);
	}

	public void addAll(KeyContainer container) {
		keys.addAll(container.getKeys());
	}
}
