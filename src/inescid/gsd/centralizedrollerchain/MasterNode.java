package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.events.Divide;
import inescid.gsd.centralizedrollerchain.events.Merge;
import inescid.gsd.centralizedrollerchain.events.SetNeighbours;
import inescid.gsd.centralizedrollerchain.events.WorkerInit;
import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.centralizedrollerchain.utils.Pair;
import inescid.gsd.common.EventReceiver;
import inescid.gsd.transport.Connection;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.utils.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MasterNode implements EventReceiver {
	private final ConnectionManager connectionManager;
	private final ScheduledExecutorService executor;
	private final Endpoint endpoint;
	private final MasterNodeInternalState s = new MasterNodeInternalState();

	PriorityBlockingQueue<Pair<Endpoint, Event>> queue = new PriorityBlockingQueue<Pair<Endpoint, Event>>();

	private static final Logger logger = Logger.getLogger(
			MasterNode.class.getName());
	private static final int MAX_REPLICATION = Configuration.getMaxReplication();

	public MasterNode(Endpoint endpoint) {
		// TODO: add keepalives on connections
		executor = Executors.newScheduledThreadPool(1);
		this.endpoint = endpoint;
		connectionManager = new ConnectionManager(this, endpoint);
	}

	public void start() {
		while (true)
			try {
				Pair<Endpoint, Event> res = queue.take();
				processEventInternal(res.getFst(), res.getSnd());
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		if (message instanceof Event)
			queue.add(new Pair<Endpoint, Event>(source, (Event) message));
		else
			MasterNode.logger.log(Level.SEVERE, "Received unknown event: " + message);
	}

	public void sendMessage(Endpoint endpoint, Event message) {
		Connection temp = connectionManager.getConnection(endpoint);
		temp.sendMessage(message);
	}

	private void processEventInternal(Endpoint source, Event message) {
		if (message instanceof WorkerInit) processWorkerInit(source, (WorkerInit) message);
	}

	private void processWorkerInit(Endpoint source, WorkerInit e) {
		if (s.addToWorkerList(e))
			MasterNode.logger.log(Level.SEVERE, "worker set contains " + source + ". worker set: "
					+ s.getWorkerSet());

		if (MasterNode.allGroups.size() == 0)
			createSeedGroup(e.getWorker());
		else {
			Group toJoin = getGroupToJoin();
			toJoin.joinNode(e.getWorker());

			if (toJoin.size() > MasterNode.MAX_REPLICATION) toJoin.divide();
		}
	}

	private Group getGroupToJoin() {
		double maxLoad = 0;
		Group toReturn = null;

		for (Group it : MasterNode.allGroups) {
			double load =
					// ((double) it.keys())
					1D / it.size();
			if (load > maxLoad) {
				maxLoad = load;
				toReturn = it;
			}
		}
		assert (toReturn != null) : MasterNode.allGroups;
		return toReturn;
	}

	Group createSeedGroup(Endpoint node) {
		TreeSet<Endpoint> set = new TreeSet<Endpoint>();
		set.add(node);
		Group toReturn = new Group();
		toReturn.setFinger(set);
		// toReturn.keys = Settings.getNKeys();
		MasterNode.allGroups.add(toReturn);
		sendMessage(node, new SetNeighbours(toReturn.finger, null, null));
		return toReturn;
	}

	static final HashSet<Group> allGroups = new HashSet<Group>();

	class Group {
		private TreeSet<Endpoint> finger;
		private Group successor;
		private Group predecessor;

		// private int keys = 0;

		TreeSet<Endpoint> getFinger() {
			return finger;
		}

		// public int keys() {
		// return this.keys;
		// }

		public void setFinger(TreeSet<Endpoint> set) {
			finger = set;
		}

		void merge() {
			if (MasterNode.allGroups.size() == 1) return;
			assert (successor != null);

			TreeSet<Endpoint> smallGroup = (TreeSet<Endpoint>) finger.clone();
			TreeSet<Endpoint> successorGroup = (TreeSet<Endpoint>) successor.finger
					.clone();

			boolean ret = MasterNode.allGroups.remove(this);
			assert (ret);
			successor.finger.addAll(finger);
			// int oldKeys = this.keys;
			// int successorKeys = this.successor.keys;
			// this.successor.keys = successorKeys + oldKeys;
			// assert (this.keys >= 0);
			// assert (this.successor.keys >= 0) : this.successor.keys + " " +
			// this.keys;
			if (predecessor == successor) {
				successor.predecessor = null;
				successor.successor = null;
			} else {
				predecessor.successor = successor;
				successor.predecessor = predecessor;
			}
			for (Endpoint it : successor.finger)
				sendMessage(it, new Merge(smallGroup, successorGroup));
		}

		void divide() {
			final int initialSize = finger.size();
			TreeSet<Endpoint> setNew = new TreeSet<Endpoint>();
			TreeSet<Endpoint> oldGroup = new TreeSet<Endpoint>();
			int newSize = initialSize / 2;
			int oldSize = initialSize - newSize;

			assert (oldSize >= newSize) : oldSize + " " + newSize + " " +
					finger.size();
			assert (oldSize >= 0) : oldSize + " " + newSize;
			// assert (this.keys >= 0);
			// int newKeys = (int) (this.keys / (((double) initialSize) /
			// ((double)
			// newSize)));
			// int oldKeys = this.keys - newKeys;
			// assert (newKeys > 0) : newKeys + " " + initialSize + " " +
			// newSize +
			// " " + this.keys;
			// assert (oldKeys > 0) : oldKeys;

			while (finger.size() > oldSize)
				setNew.add(Utils.removeRandomEl(finger));

			Group newGroup = createGroup(setNew);

			assert ((newGroup.size() + size()) == initialSize) : newGroup.size() + " " + size()
					+ " " + initialSize + " " + oldGroup.size() + " " + setNew.size();

			if (predecessor != null) {
				newGroup.predecessor = predecessor;
				predecessor.successor = newGroup;
				predecessor = newGroup;
				newGroup.successor = this;
			} else {
				predecessor = newGroup;
				successor = newGroup;
				newGroup.predecessor = this;
				newGroup.successor = this;
			}
			// newGroup.keys = newKeys;
			// this.keys = oldKeys;

			for (Endpoint it : newGroup.finger)
				sendMessage(it, new Divide(newGroup.finger, finger));
			for (Endpoint it : finger)
				sendMessage(it, new Divide(newGroup.finger, finger));
		}

		private Group createGroup(TreeSet<Endpoint> setNew) {
			Group toReturn = new Group();
			toReturn.finger = setNew;
			MasterNode.allGroups.add(toReturn);
			return toReturn;
		}

		public int size() {
			return finger.size();
		}

		void joinNode(Endpoint node) {
			finger.add(node);
			sendMessage(node, new SetNeighbours(finger, predecessor.finger,
					successor.finger));
		}

		@Override
		public String toString() {
			return
			// this.keys +
			"" + finger + "";
		}
	}
}

class MasterNodeInternalState {
	Set<Endpoint> workerList = new HashSet<Endpoint>();

	public boolean addToWorkerList(WorkerInit e) {
		return workerList.add(e.getWorker());
	}

	public Set<Endpoint> getWorkerSet() {
		return Collections.unmodifiableSet(workerList);
	}
}
