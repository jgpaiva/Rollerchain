package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Configuration;
import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.utils.FileOutput;
import inescid.gsd.transport.Endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyRemovalManager {
	private final KeyStorage keys;
	private final ArrayList<TreeSet<Key>> toRemove;
	private final FileOutput writer;
	private static final Logger logger = Logger.getLogger(KeyRemovalManager.class.getName());

	public KeyRemovalManager(KeyStorage keys, Endpoint e) {
		this.keys = keys;
		toRemove = new ArrayList<TreeSet<Key>>();
		for (int it = 0; it < Configuration.getRoundsToKeepKeys(); it++)
			toRemove.add(new TreeSet<Key>());
		writer = new FileOutput(e, this.getClass());
	}

	public void nextRound(Identifier lowerID, Identifier higherID) {
		TreeSet<Key> toRemoveThisRound = keys.getInverse(lowerID, higherID);

		// keep only keys that are not needed
		for (TreeSet<Key> it : toRemove) {
			it.retainAll(toRemoveThisRound);
			toRemoveThisRound.removeAll(it);
		}

		if (toRemoveThisRound.size() >= keys.size())
			KeyRemovalManager.logger.log(Level.WARNING,
					"Scheduling all keys for deletion? toRemoveThisRound:"
							+ toRemoveThisRound + " keys:" + keys + " lowerID: " + lowerID + " higherID" + higherID);

		// remove keys that have gone through the N rounds
		Collections.rotate(toRemove, 1);
		if (toRemove.get(0).size() > 0)
			keys.removeAll(toRemove.get(0));
		int removed = toRemove.get(0).size();

		printInfo(toRemove.get(0), toRemoveThisRound);

		// add the keys for the current round;
		toRemove.set(0, toRemoveThisRound);

		writer.status(keys.size() + " keys. " + toRemoveThisRound.size() + " toRemove. " + removed
				+ " removed.");
	}

	private void printInfo(TreeSet<Key> nowRemoving, TreeSet<Key> toRemoveThisRound) {
		if (nowRemoving.size() > 0) {
			String tmp = "Removed " + nowRemoving.size() + ". Has " + keys.size() + " keys. ";

			if (toRemoveThisRound.size() > 0)
				KeyRemovalManager.logger.log(Level.INFO, tmp + "Scheduled " + toRemoveThisRound.size()
						+ " for removal.");
			else
				KeyRemovalManager.logger.log(Level.FINER, tmp + "Did not schedule for removal. ");
		} else {
			String tmp = "Did not remove. Has " + keys.size() + " keys. ";
			if (toRemoveThisRound.size() > 0)
				KeyRemovalManager.logger.log(Level.FINE, tmp + "Scheduled " + toRemoveThisRound.size()
						+ " for removal.");
			else
				KeyRemovalManager.logger.log(Level.FINER, tmp + "Did not schedule for removal. ");
		}
	}
}
