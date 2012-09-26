package gsd.protocols.vsp.dht;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

public class ReplicationState {
	private TreeSet<Key> keys;
	private Finger owner;
	private TreeSet<Key> keysCopy;
	private BigInteger lastLowerID;
	private BigInteger lastHigherID;
	private Object identity;
	private BigInteger middlePoint;
	private BigInteger myID;
	private Boolean lastHasWrongKeys;
	private ArrayList<Tuple<Pair<Integer, Integer>, BigInteger, BigInteger>> hashCodeMemoization;
	private Tuple<TreeSet<Key>, BigInteger, BigInteger> myKeysCopy;

	public ReplicationState() {
		owner = null;
		keys = null;
		resetMemoization();
	}

	public ReplicationState(Finger owner) {
		this.owner = owner;
		keys = new TreeSet<Key>();
		resetMemoization();
	}

	@SuppressWarnings("unchecked")
	public ReplicationState(ReplicationState keyReplicationState) {
		owner = keyReplicationState.owner;
		keys = (TreeSet<Key>) keyReplicationState.keys.clone();
		identity = keyReplicationState.identity;
		resetMemoization();
	}

	public ReplicationState getCopy() {
		ReplicationState tmp = new ReplicationState();
		tmp.owner = owner;
		tmp.keys = getKeysCopy();
		tmp.identity = identity;
		return tmp;
	}

	public void clear() {
		keys = new TreeSet<Key>();
		resetMemoization();
	}

	public void resetMemoization() {
		keysCopy = null;
		lastHasWrongKeys = null;
		identity = new Object();
		middlePoint = null;
		myID = null;
		hashCodeMemoization = new ArrayList<Tuple<Pair<Integer, Integer>, BigInteger, BigInteger>>();
		myKeysCopy = null;
	}

	/**
	 * merges other state with this.map. changes this.map
	 * 
	 * @param state
	 * @return
	 */
	public long merge(ReplicationState state) {
		long temp = keys.size();
		if (keys.addAll(state.keys)) resetMemoization();
		return keys.size() - temp;
	}

	/**
	 * adds the keys from <code>keys</code> to <code>this</code>
	 * 
	 * @param senderFinger
	 * @param keys
	 * @return
	 */
	public void addAll(Collection<Key> keys) {
		if (this.keys.addAll(keys)) resetMemoization();
	}

	/**
	 * removes the keys in the <code>other</code> list. adds them to the
	 * <code>removedNodes</code> list
	 * 
	 * @param other
	 * @param removedKeys
	 */
	public final void removeEntries(Collection<Key> other,
			Collection<Key> removedKeys) {
		if (!other.iterator().hasNext() || (keys.size() == 0))
			return;
		// declare iterators
		Iterator<Key> thisIterator = keys.iterator();
		Iterator<Key> otherIterator = other.iterator();
		// declare values
		Key thisCurrent = thisIterator.next();
		Key otherCurrent = otherIterator.next();
		boolean changed = false;
		while (true) {
			int result = thisCurrent.compareTo(otherCurrent);
			if (result == 0) {
				removedKeys.add(thisCurrent);
				thisIterator.remove();
				changed = true;
				if (thisIterator.hasNext())
					thisCurrent = thisIterator.next();
				else
					break;
			} else if (result > 0) {
				if (otherIterator.hasNext())
					otherCurrent = otherIterator.next();
				else
					break;
			} else if (thisIterator.hasNext())
				thisCurrent = thisIterator.next();
			else
				break;
		}
		if (changed) resetMemoization();
	}

	/**
	 * removes the keys common to the this and the parameter collection
	 * 
	 * @param otherCollection
	 *            collection with the keys to be removed
	 * @return ordered list of keys removed
	 */
	public final List<Key> removeCommonKeysFrom(Collection<Key> otherCollection) {
		List<Key> toReturn = new LinkedList<Key>();
		if (!otherCollection.iterator().hasNext() || (keys.size() == 0))
			return toReturn;
		// declare iterators
		Iterator<Key> thisIterator = keys.iterator();
		Iterator<Key> otherIterator = otherCollection.iterator();
		// declare values
		Key thisCurrent = thisIterator.next();
		Key otherCurrent = otherIterator.next();
		while (true) {
			int result = thisCurrent.compareTo(otherCurrent);
			if (result == 0) {
				toReturn.add(thisCurrent);
				otherIterator.remove();
				if (thisIterator.hasNext())
					thisCurrent = thisIterator.next();
				else
					break;
			} else if (result > 0) {
				if (otherIterator.hasNext())
					otherCurrent = otherIterator.next();
				else
					break;
			} else if (thisIterator.hasNext())
				thisCurrent = thisIterator.next();
			else
				break;
		}
		return toReturn;
	}

	public long size() {
		return keys.size();
	}

	public boolean contains(Key key) {
		return keys.contains(key);
	}

	public TreeSet<Key> getKeys() {
		return keys;
	}

	/**
	 * gets keys common with this replication state and the parameter keys
	 * 
	 * @param keys
	 * @return NEW set with common keys
	 */
	@SuppressWarnings("unchecked")
	public TreeSet<Key> get(SortedSet<Key> keys) {
		TreeSet<Key> returnValue = (TreeSet<Key>) this.keys.clone();
		returnValue.retainAll(keys);
		return returnValue;
	}

	/**
	 * returns a NEW set, doesn't change otherSet
	 * 
	 * @return NEW set with keys
	 */
	public static final TreeSet<Key> getMyKeys(TreeSet<Key> otherSet,
			BigInteger lowerID, BigInteger higherID) {
		TreeSet<Key> keys = null;

		Key higher = new Key(higherID);
		Key lower = new Key(lowerID);
		if (lower.compareTo(higher) <= 0)
			keys = new TreeSet<Key>(otherSet.subSet(lower, false, higher,
					true));
		else {
			TreeSet<Key> temp = new TreeSet<Key>(otherSet.tailSet(lower, false));
			temp.addAll(otherSet.headSet(higher, true));
			keys = temp;
		}
		return keys;
	}

	private static final Pair<Integer, Integer> getMyHashCode(
			TreeSet<Key> otherSet,
			BigInteger lowerID, BigInteger higherID) {
		Key higher = new Key(higherID);
		Key lower = new Key(lowerID);
		if (lower.compareTo(higher) <= 0) {
			NavigableSet<Key> temp = otherSet.subSet(lower, false, higher,
					true);
			return new Pair<Integer, Integer>(temp.hashCode(), temp.size());
		}// else
		TreeSet<Key> temp = new TreeSet<Key>(otherSet.tailSet(lower, false));
		temp.addAll(otherSet.headSet(higher, true));
		Pair<Integer, Integer> toReturn = new Pair<Integer, Integer>(
				temp.hashCode(), temp.size());
		temp.clear();
		return toReturn;
	}

	public final boolean hasWrongKeys(BigInteger lowerID, BigInteger higherID) {
		if ((lastHasWrongKeys == null) || (lowerID != lastLowerID)
				|| (higherID != lastHigherID)) {
			Key higher = new Key(higherID);
			Key lower = new Key(lowerID);
			assert (!lower.equals(higher));
			int size = 0;
			if (lower.compareTo(higher) <= 0) {
				size = keys.headSet(lower, true).size();
				size += keys.tailSet(higher, false).size();
			} else
				size = keys.subSet(higher, false, lower, true).size();
			if (size != 0)
				lastHasWrongKeys = true;
			else
				lastHasWrongKeys = false;
			lastLowerID = lowerID;
			lastHigherID = higherID;
		}
		return lastHasWrongKeys;
	}

	/**
	 * adds keys to this replication state which are between the lower and
	 * higher ID
	 * 
	 * @param otherSet
	 * @param lowerID
	 * @param higherID
	 */
	public void addMine(TreeSet<Key> otherSet,
			BigInteger lowerID, BigInteger higherID) {
		Key higher = new Key(higherID);
		Key lower = new Key(lowerID);
		if (lower.compareTo(higher) <= 0)
			addAll(otherSet.subSet(lower, false, higher,
					true));
		else {
			addAll(otherSet.tailSet(lower, false));
			addAll(otherSet.headSet(higher, true));
		}
		// this.resetMemoization(); - this.addall already resets memoization
	}

	public Finger getOwner() {
		return owner;
	}

	public boolean removeAll(Collection<Key> remainingKeys) {
		if (keys.removeAll(remainingKeys)) {
			resetMemoization();
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public TreeSet<Key> getKeysCopy() {
		if (keysCopy == null) keysCopy = (TreeSet<Key>) keys.clone();
		return keysCopy;
	}

	Key getFirst() {
		return keys.first();
	}

	Key getLast() {
		return keys.last();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ReplicationState)) return false;
		ReplicationState rs = (ReplicationState) o;
		return rs.owner.equals(owner) && (rs.identity == identity);
	}

	public Object getIdentity() {
		return identity;
	}

	public void setIdentity(Object ident) {
		identity = ident;
	}

	public BigInteger getMiddlePoint(BigInteger myID) {
		if ((middlePoint == null) || (this.myID != myID)) {
			middlePoint = calculateMiddlePoint(myID);
			this.myID = myID;
		}
		return middlePoint;
	}

	private BigInteger calculateMiddlePoint(BigInteger myID) {
		int size = keys.size();
		int middle = size / 2;
		if (middle == 0)
			return null;

		NavigableSet<Key> currentSet = keys.headSet(new Key(myID),
				false);

		int counter = 0;
		for (Iterator<Key> it = currentSet.descendingIterator(); it.hasNext();) {
			counter++;
			BigInteger value = it.next().value;
			if (counter >= middle)
				return value;
		}

		currentSet = keys.tailSet(new Key(myID), true);
		for (Iterator<Key> it = currentSet.descendingIterator(); it.hasNext();) {
			counter++;
			BigInteger value = it.next().value;
			if (counter >= middle)
				return value;
		}

		throw new RuntimeException("Unreacheable code!");
	}

	public void addKey(Key it) {
		if (keys.add(it)) resetMemoization();
	}

	public Pair<Integer, Integer> getHashCodeForMyKeys(BigInteger lowerID,
			BigInteger higherID) {
		Tuple<Pair<Integer, Integer>, BigInteger, BigInteger> temp = getHashCodeMemoization(
				lowerID, higherID);
		if (temp == null) {
			Pair<Integer, Integer> it = ReplicationState.getMyHashCode(
					keys, lowerID, higherID);
			temp = new Tuple<Pair<Integer, Integer>, BigInteger, BigInteger>(
					new Pair<Integer, Integer>(it.fst, it.snd), lowerID,
					higherID);
			hashCodeMemoization.add(temp);
		}
		return temp.fst;
	}

	private Tuple<Pair<Integer, Integer>, BigInteger, BigInteger> getHashCodeMemoization(
			BigInteger lowerID, BigInteger higherID) {
		for (Tuple<Pair<Integer, Integer>, BigInteger, BigInteger> it : hashCodeMemoization)
			if ((it.snd == lowerID) && (it.trd == higherID))
				return it;
		return null;
	}

	public TreeSet<Key> getMyKeysCopy(BigInteger lowerID, BigInteger higherID) {
		if ((myKeysCopy == null) || (myKeysCopy.snd != lowerID)
				|| (myKeysCopy.trd != higherID))
			myKeysCopy = new Tuple<TreeSet<Key>, BigInteger, BigInteger>(
					ReplicationState.getMyKeys(keys,
							lowerID, higherID), lowerID, higherID);
		return myKeysCopy.fst;
	}

	@Override
	public int hashCode() {
		if (keys != null)
			return keys.hashCode();
		// else
		return 0;
	}

	public void retainAll(TreeSet<Key> otherKeys) {
		if (keys.retainAll(otherKeys)) resetMemoization();
	}
}