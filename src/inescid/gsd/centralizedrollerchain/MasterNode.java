package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.MasterNode.Group;
import inescid.gsd.centralizedrollerchain.events.Divide;
import inescid.gsd.centralizedrollerchain.events.KeepAlive;
import inescid.gsd.centralizedrollerchain.events.Merge;
import inescid.gsd.centralizedrollerchain.events.SetNeighbours;
import inescid.gsd.centralizedrollerchain.events.WorkerInit;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.utils.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MasterNode extends Node {
	private final ScheduledExecutorService executor;

	private final MasterNodeInternalState s = new MasterNodeInternalState();

	private static final int MAX_REPLICATION = Configuration.getMaxReplication();
	private static final int MIN_REPLICATION = Configuration.getMinReplication();

	private static final long KEEP_ALIVE_INTERVAL = Configuration.getKeepAliveInterval();

	public MasterNode(Endpoint endpoint) {
		super(endpoint);
		executor = Executors.newScheduledThreadPool(1);
	}

	@Override
	protected void processEventInternal(Endpoint source, Object object) {
		Node.logger.log(Level.FINE, "master R: " + source + " / " + object);
		if (object instanceof WorkerInit)
			processWorkerInit(source, (WorkerInit) object);
		else if (object instanceof DeathNotification)
			processDeathNotification(source, (DeathNotification) object);
		else
			Node.logger.log(Level.SEVERE, "Received unknown event: " + object);
		Node.logger.log(Level.FINER, "active nodes:" + s.workerList.size() + "     list: "
				+ s.workerList);
	}

	private void processWorkerInit(Endpoint source, WorkerInit e) {
		Group toJoin = null;
		if (MasterNode.allGroups.size() == 0)
			toJoin = createSeedGroup(source);
		else {
			toJoin = getGroupToJoin();
			toJoin.addNode(source);
		}

		if (s.addToWorkerList(source, toJoin) != null)
			Node.logger.log(Level.SEVERE, "worker set contains " + source
					+ " associated with group: " + toJoin);

		if (toJoin.size() > MasterNode.MAX_REPLICATION) toJoin.divide();
	}

	private void processDeathNotification(Endpoint source, DeathNotification object) {
		Group oldGroup = s.removeWorkerFromList(source);
		if (oldGroup != null) {
			oldGroup.removeNode(source);
			if (oldGroup.size() < MasterNode.MIN_REPLICATION) oldGroup.merge();
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

	private Group createSeedGroup(Endpoint node) {
		TreeSet<Endpoint> set = new TreeSet<Endpoint>();
		set.add(node);
		Group toReturn = createGroup(set);
		sendMessage(node, new SetNeighbours(toReturn.getFinger(), null, null));
		return toReturn;
	}

	private Group createGroup(TreeSet<Endpoint> setNew) {
		Group toReturn = new Group();
		toReturn.setFinger(setNew);
		MasterNode.allGroups.add(toReturn);
		return toReturn;
	}

	class CheckGroupConnections implements Runnable {
		private final Group group;

		public CheckGroupConnections(Group group) {
			this.group = group;
		}

		@Override
		public void run() {
			checkGroupConnections(group);
		}
	}

	private void checkGroupConnections(Group group) {
		for (Endpoint it : group.getFinger())
			sendMessage(it, new KeepAlive());
	}

	static final HashSet<Group> allGroups = new HashSet<Group>();

	public void checkIntegrity() {
		System.out.println("Checking Integrity");
		Set<Endpoint> allNodes = new TreeSet<Endpoint>();
		for (Group it : MasterNode.allGroups)
			for (Endpoint it2 : it.finger) {
				if (!allNodes.add(it2))
					System.out.println("ERROR: Node " + it2 + " was in group " + it
							+ " and in some other group!");
				Group res = s.getWorkerSet().get(it2);
				if (res != it)
					System.out.println("ERROR: Node " + it2 + " was registered in group " + res
							+ " and should be in " + it);
			}
		if (allNodes.size() != s.getWorkerSet().size())
			System.out.println("WorkerSet and AllNodes differ!" + " WorkerSet:"
					+ s.getWorkerSet().size() + " allNodes:" + allNodes.size());
		System.out.println("Done checking Integrity");
	}

	class Group {
		private TreeSet<Endpoint> finger;
		private Group successor;
		private Group predecessor;
		private final ScheduledFuture<?> schedule;
		private boolean active;

		public Group() {
			schedule = executor.scheduleAtFixedRate(new CheckGroupConnections(
					this), MasterNode.KEEP_ALIVE_INTERVAL,
					MasterNode.KEEP_ALIVE_INTERVAL, TimeUnit.SECONDS);
			active = true;
		}

		public void cancelSchedule() {
			if (getFinger().size() > 0) {
				Node.logger
						.log(Level.SEVERE, "canceling a schedule for a group with nodes: + this");
				Thread.dumpStack();
			}
			schedule.cancel(false);
			active = false;
		}

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

		@SuppressWarnings("unchecked")
		void merge() {
			if (MasterNode.allGroups.size() == 1) return;
			assert (successor != null);

			TreeSet<Endpoint> smallGroup = (TreeSet<Endpoint>) finger.clone();
			TreeSet<Endpoint> successorGroup = (TreeSet<Endpoint>) successor.finger
					.clone();

			boolean ret = MasterNode.allGroups.remove(this);
			assert (ret);
			successor.finger.addAll(finger);
			moveAllFrom(this, successor, finger);
			finger.clear();
			cancelSchedule();

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
				sendMessage(it, new Merge(smallGroup, successorGroup,
						successor != null ? successor.finger : null,
						predecessor != null ? predecessor.finger : null));
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
				setNew.add(Utils.removeRandomEl(getFinger()));

			Group newGroup = createGroup(setNew);
			moveAllFrom(this, newGroup, newGroup.finger);

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

		private void moveAllFrom(Group group, Group newGroup, TreeSet<Endpoint> toMove) {
			for (Endpoint it : toMove) {
				Group res = s.addToWorkerList(it, newGroup);
				if (res != group)
					Node.logger.log(Level.SEVERE, "Node was in wrong group! Should be in " + this
							+ " but was in " + res);
			}
		}

		public int size() {
			return finger.size();
		}

		void addNode(Endpoint node) {
			finger.add(node);
			sendMessage(node, new SetNeighbours(finger, predecessor != null ? predecessor.finger
					: null,
					successor != null ? successor.finger : null));
		}

		public void removeNode(Endpoint source) {
			if (!finger.remove(source))
				Node.logger
						.log(Level.SEVERE, "Endpoint " + endpoint + " was not in finger!" + this);
		}

		@Override
		public String toString() {
			return
			// this.keys +
			"" + finger + "" + (active ? "A" : "I");
		}
	}
}

class MasterNodeInternalState {
	Map<Endpoint, Group> workerList = new TreeMap<Endpoint, Group>();

	public Group addToWorkerList(Endpoint e, Group g) {
		return workerList.put(e, g);
	}

	public Group removeWorkerFromList(Endpoint e) {
		return workerList.remove(e);
	}

	public Map<Endpoint, Group> getWorkerSet() {
		return Collections.unmodifiableMap(workerList);
	}
}
