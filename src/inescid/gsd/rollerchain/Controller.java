package inescid.gsd.rollerchain;

import inescid.gsd.rollerchain.events.Divide;
import inescid.gsd.rollerchain.events.Merge;
import inescid.gsd.rollerchain.events.SetNeighbours;
import inescid.gsd.rollerchain.events.WorkerInit;
import inescid.gsd.rollerchain.interfaces.Event;
import inescid.gsd.rollerchain.interfaces.EventReceiver;
import inescid.gsd.rollerchain.utils.Output;
import inescid.gsd.utils.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.PriorityBlockingQueue;

public class Controller implements EventReceiver, Runnable {
	private Output out;
	private ControllerInternalState s;

	PriorityBlockingQueue<Event> queue = new PriorityBlockingQueue<Event>();

	Controller() {
		new Thread(this).start();
	}

	@Override
	public void processEvent(Event e) {
		this.queue.put(e); // TODO: take advantage of priorities
		this.out.write(this, "queued " + e + ". queue has: " + this.queue.size());
	}

	@Override
	public void run() {
		while (true) {
			try {
				this.processEventInternal(this.queue.take());
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	public void processEventInternal(Event e) {
		if (e instanceof WorkerInit) {
			this.processWorkerInit((WorkerInit) e);
		}
	}

	private void processWorkerInit(WorkerInit e) {
		if (this.s.addToWorkerList(e)) {
			this.out.write(this, e + " was in worker list already");
		}

		if (Group.groups.size() == 0) {
			Group.createSeedGroup(e.getWorker());
		} else {
			Group toJoin = this.getGroupToJoin();
			toJoin.joinNode(e.getWorker());

			if (toJoin.size() > Settings.getMaxReplication()) {
				toJoin.divide();
			}
		}
	}

	private Group getGroupToJoin() {
		double maxLoad = 0;
		Group toReturn = null;

		for (Group it : Group.groups) {
			double load =
					// ((double) it.keys())
					1D / it.size();
			if (load > maxLoad) {
				maxLoad = load;
				toReturn = it;
			}
		}
		assert (toReturn != null) : Group.groups;
		return toReturn;
	}
	//
	// public void killed(Collection<Pair<Node, Integer>> availabilityList) {
	// Group tempGroup = this.myGroup;
	// this.myGroup.removeNode(this.getNode());
	// if (tempGroup.size() < GroupReplication.minReplication) {
	// tempGroup.merge();
	// }
	// if (tempGroup.size() > GroupReplication.maxReplication) {
	// tempGroup.divide();
	// }
	// }
}

class ControllerInternalState {
	Set<EventReceiver> workerList = new HashSet<EventReceiver>(); // TODO:
																	// confirm
																	// that
																	// there's
																	// an equals
																	// implemented

	public boolean addToWorkerList(WorkerInit e) {
		return this.workerList.add(e.getWorker());
	}
}

class Group {
	static final HashSet<Group> groups = new HashSet<Group>();

	private TreeSet<EventReceiver> finger;
	private Group successor;
	private Group predecessor;

	// private int keys = 0;

	TreeSet<EventReceiver> getFinger() {
		return this.finger;
	}

	static Group createSeedGroup(EventReceiver node) {
		TreeSet<EventReceiver> set = new TreeSet<EventReceiver>();
		set.add(node);
		Group toReturn = new Group();
		toReturn.finger = set;
		// toReturn.keys = Settings.getNKeys();
		Group.groups.add(toReturn);
		return toReturn;
	}

	private Group() {
	}

	// public int keys() {
	// return this.keys;
	// }

	void merge() {
		if (Group.groups.size() == 1) return;
		assert (this.successor != null);

		TreeSet<EventReceiver> smallGroup = (TreeSet<EventReceiver>) this.finger.clone();
		TreeSet<EventReceiver> successorGroup = (TreeSet<EventReceiver>) this.successor.finger
				.clone();

		boolean ret = Group.groups.remove(this);
		assert (ret);
		this.successor.finger.addAll(this.finger);
		// int oldKeys = this.keys;
		// int successorKeys = this.successor.keys;
		// this.successor.keys = successorKeys + oldKeys;
		// assert (this.keys >= 0);
		// assert (this.successor.keys >= 0) : this.successor.keys + " " +
		// this.keys;
		if (this.predecessor == this.successor) {
			this.successor.predecessor = null;
			this.successor.successor = null;
		} else {
			this.predecessor.successor = this.successor;
			this.successor.predecessor = this.predecessor;
		}
		for (EventReceiver it : this.successor.finger) {
			it.processEvent(new Merge(smallGroup, successorGroup));
		}
	}

	void divide() {
		final int initialSize = this.finger.size();
		TreeSet<EventReceiver> setNew = new TreeSet<EventReceiver>();
		TreeSet<EventReceiver> oldGroup = new TreeSet<EventReceiver>();
		int newSize = initialSize / 2;
		int oldSize = initialSize - newSize;

		assert (oldSize >= newSize) : oldSize + " " + newSize + " " +
				this.finger.size();
		assert (oldSize >= 0) : oldSize + " " + newSize;
		// assert (this.keys >= 0);
		// int newKeys = (int) (this.keys / (((double) initialSize) / ((double)
		// newSize)));
		// int oldKeys = this.keys - newKeys;
		// assert (newKeys > 0) : newKeys + " " + initialSize + " " + newSize +
		// " " + this.keys;
		// assert (oldKeys > 0) : oldKeys;

		while (this.finger.size() > oldSize) {
			setNew.add(Utils.removeRandomEl(this.finger));
		}

		Group newGroup = this.createGroup(setNew);

		assert (newGroup.size() + this.size() == initialSize) : newGroup.size() + " " + this.size()
				+ " " + initialSize + " " + oldGroup.size() + " " + setNew.size();

		if (this.predecessor != null) {
			newGroup.predecessor = this.predecessor;
			this.predecessor.successor = newGroup;
			this.predecessor = newGroup;
			newGroup.successor = this;
		} else {
			this.predecessor = newGroup;
			this.successor = newGroup;
			newGroup.predecessor = this;
			newGroup.successor = this;
		}
		// newGroup.keys = newKeys;
		// this.keys = oldKeys;

		for (EventReceiver it : newGroup.finger) {
			it.processEvent(new Divide(newGroup.finger, this.finger));
		}
		for (EventReceiver it : this.finger) {
			it.processEvent(new Divide(newGroup.finger, this.finger));
		}
	}

	private Group createGroup(TreeSet<EventReceiver> setNew) {
		Group toReturn = new Group();
		toReturn.finger = setNew;
		Group.groups.add(toReturn);
		return toReturn;
	}

	public int size() {
		return this.finger.size();
	}

	void joinNode(EventReceiver node) {
		this.finger.add(node);
		node.processEvent(new SetNeighbours(this.finger, this.predecessor.finger,
				this.successor.finger));
	}

	@Override
	public String toString() {
		return
		// this.keys +
		"" + this.finger + "";
	}
}