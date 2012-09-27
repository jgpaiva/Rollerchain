package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Configuration;
import inescid.gsd.centralizedrollerchain.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

public class KeyRemovalManager {
	private final KeyStorage keys;
	private final ArrayList<TreeSet<Key>> toRemove;

	public KeyRemovalManager(KeyStorage keys) {
		this.keys = keys;
		toRemove = new ArrayList<TreeSet<Key>>();
		for (int it = 0; it < Configuration.getRoundsToKeepKeys(); it++)
			toRemove.add(new TreeSet<Key>());
	}

	public void nextRound(Identifier lowerID, Identifier higherID) {
		TreeSet<Key> toRemoveThisRound = keys.getInverse(lowerID, higherID);

		// keep only keys that are not needed
		for (TreeSet<Key> it : toRemove) {
			it.retainAll(toRemoveThisRound);
			toRemoveThisRound.removeAll(it);
		}

		// remove keys that have gone through the N rounds
		Collections.rotate(toRemove, 1);
		keys.removeAll(toRemove.get(0));

		// add the keys for the current round;
		toRemove.set(0, toRemoveThisRound);
	}
}
