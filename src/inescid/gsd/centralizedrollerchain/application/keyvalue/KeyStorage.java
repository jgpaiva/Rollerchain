package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Configuration;
import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.StaticGroup;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;

public class KeyStorage implements Iterable<Key> {
	private TreeSet<Key> keys;

	public KeyStorage() {
		keys = new TreeSet<Key>();
	}

	public KeyStorage(Key[] listing) {
		keys = new TreeSet<Key>();
		for (Key it : listing)
			keys.add(it);
	}

	/**
	 * Creates the first batch of keys
	 */
	public void init() {
		Random r = new Random(Configuration.getKeysRandomSeed());
		int minKeySize = Configuration.getMinKeySize();
		int maxKeySize = Configuration.getMaxKeySize();
		int idSize = Configuration.getIDSize();
		for (int it = 0; it < Configuration.getNKeys(); it++) {
			int currentKeySize = r.nextInt(maxKeySize - minKeySize) + minKeySize;
			Identifier currentID = new Identifier(idSize, r);
			keys.add(new Key(currentID, currentKeySize));
		}
	}

	public KeyContainer getKeys(Identifier lowerID, Identifier higherID) {
		TreeSet<Key> result = KeyStorage.filterInternal(lowerID, higherID, keys, false);
		if (result == null)
			return new KeyContainer(new TreeSet<Key>(keys));
		else
			return new KeyContainer(result);
	}

	public TreeSet<Key> getInverse(Identifier lowerID, Identifier higherID) {
		TreeSet<Key> result = KeyStorage.filterInternal(lowerID, higherID, keys, true);
		if (result == null)
			return new TreeSet<Key>();
		else
			return result;
	}

	/**
	 * Filters a keySet by identifiers. Returns null if the keySet should not be
	 * modified. Always returns NULL or a copy of the filtered object.
	 * 
	 * @param lowerID
	 * @param higherID
	 * @param keySet
	 * @param reverse
	 *            defines if it should use the interval or its oposite
	 * @return NULL or a copy of the filtered object.
	 */
	private static TreeSet<Key> filterInternal(Identifier lowerID, Identifier higherID,
			TreeSet<Key> keySet, boolean reverse) {
		if ((lowerID == null) || (higherID == null))
			return null;

		Key higher = new Key(higherID, 0);
		Key lower = new Key(lowerID, 0);

		if (reverse) {
			Key temp = higher;
			higher = lower;
			lower = temp;
		}

		if (lower.compareTo(higher) <= 0) {
			NavigableSet<Key> temp = keySet.subSet(lower, false, higher, true);
			if (temp.size() == keySet.size())
				return null;
			else
				return new TreeSet<Key>(temp);
		} else {
			NavigableSet<Key> part1 = keySet.tailSet(lower, false);
			NavigableSet<Key> part2 = keySet.headSet(higher, true);
			if ((part1.size() + part2.size()) == keySet.size())
				return null;
			else {
				TreeSet<Key> temp = new TreeSet<Key>(part1);
				temp.addAll(part2);
				return temp;
			}
		}
	}

	public void addAll(KeyContainer container) {
		keys.addAll(container.getKeys());
	}

	public void addAll(KeyStorage tmp) {
		keys.addAll(tmp.getRawKeys());
	}

	public KeyListing getKeyListing() {
		return new KeyListing(keys);
	}

	public int size() {
		return keys.size();
	}

	public void removeAll(KeyStorage keys2) {
		keys.removeAll(keys2.keys);
	}

	public void filter(StaticGroup group, Identifier predecessorID) {
		TreeSet<Key> result = KeyStorage.filterInternal(predecessorID, group.getID(), keys, false);
		if (result == null)
			return;
		keys = result;
	}

	public void clear() {
		keys.clear();
	}

	@Override
	public Iterator<Key> iterator() {
		return keys.iterator();
	}

	public void removeAll(Collection<Key> toRemove) {
		keys.removeAll(toRemove);
	}

	public void retainAll(Collection<Key> toRetain) {
		keys.retainAll(toRetain);
	}

	public KeyContainer get(KeyListing toGet) {
		@SuppressWarnings("unchecked")
		TreeSet<Key> temp = (TreeSet<Key>) keys.clone();
		temp.retainAll(toGet.asList());
		return new KeyContainer(temp);
	}

	public TreeSet<Key> getRawKeys() {
		return keys;
	}
}
