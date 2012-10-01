package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.application.keyvalue.GetInfoReply;
import inescid.gsd.centralizedrollerchain.application.keyvalue.Key;
import inescid.gsd.centralizedrollerchain.events.Divide;
import inescid.gsd.centralizedrollerchain.events.GetInfo;
import inescid.gsd.centralizedrollerchain.events.SetNeighbours;
import inescid.gsd.centralizedrollerchain.events.WorkerInit;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MasterNode extends Node {
	final MasterNodeInternalState s = new MasterNodeInternalState();

	private static final int MAX_REPLICATION = Configuration.getMaxReplication();
	private static final int MIN_REPLICATION = Configuration.getMinReplication();
	private static final long KEEP_ALIVE_INTERVAL = Configuration.getKeepAliveInterval();

	public MasterNode(Endpoint endpoint) {
		super(endpoint);
	}

	@Override
	protected void processEventInternal(Endpoint source, Object object) {
		Node.logger.log(Level.FINE, "master R: " + source + " / " + object);
		if (object instanceof WorkerInit)
			processWorkerInit(source, (WorkerInit) object);
		else if (object instanceof DeathNotification)
			processDeathNotification(source, (DeathNotification) object);
		else if (object instanceof GetInfoReply)
			processGetInfoReply(source, (GetInfoReply) object);
		else
			Node.die("Received unknown event: " + object);
		Node.logger.log(Level.FINEST,
				"active nodes:" + s.getWorkerList().size() + "     list: " + s.getWorkerList());
	}

	@Override
	public void init() {
		super.init();
		// nothing to be done on initialization
	}

	private void processWorkerInit(Endpoint source, WorkerInit e) {
		Group toJoin = null;
		if (s.getAllGroupsSize() == 0)
			toJoin = createSeedGroup(source);
		else {
			toJoin = getGroupToJoin();
			toJoin.addNode(source);
		}

		for (Endpoint it : toJoin.getFinger())
			sendMessage(it, new SetNeighbours(toJoin.getStaticGroup(),
					toJoin.getPredecessor(), toJoin.getSuccessor()));
		Node.logger.log(Level.INFO, "Joined node to Group: " + toJoin);

		if (s.addToWorkerList(source, toJoin) != null)
			Node.die("worker set contains " + source + " associated with group: " + toJoin);

		if (toJoin.size() > MasterNode.MAX_REPLICATION) {
			Identifier newIdentifier = null;
			{
				if (toJoin.getPredecessor() == null) // check if seed group or
					// not
					newIdentifier = Identifier.MIDDLE_POINT;
				else
					newIdentifier = toJoin.getKeys() != null ? MasterNode.calculateMiddlePoint(
							toJoin.getID(),
							toJoin.getPredecessor().getID(), toJoin.size(), toJoin.getKeys())
							: Identifier
							.calculateMiddlePoint(toJoin.getID(), toJoin.getPredecessor().getID());
			}
			Group newGroup = createGroup(newIdentifier, new TreeSet<Endpoint>());
			toJoin.divide(newGroup);

			for (Endpoint it : newGroup.getFinger())
				sendMessage(it, new Divide(newGroup.getStaticGroup(), toJoin.getStaticGroup()));
			for (Endpoint it : toJoin.getFinger())
				sendMessage(it, new Divide(newGroup.getStaticGroup(), toJoin.getStaticGroup()));

			Node.logger.log(Level.INFO, "divided into: " + newGroup + " and " + toJoin);
		}
	}

	private static Identifier calculateMiddlePoint(Identifier ID, Identifier predID, int groupSize,
			TreeSet<Key> keys) {
		int size = keys.size();
		int smallGroupSize = groupSize / 2;
		int middle = (size * smallGroupSize) / groupSize;
		if (middle == 0)
			return null;

		NavigableSet<Key> currentSet = keys.headSet(new Key(ID, 0), false);

		int counter = 0;
		for (Iterator<Key> it = currentSet.descendingIterator(); it.hasNext();) {
			counter++;
			Identifier value = it.next().getID();
			if (counter >= middle)
				return value;
		}

		currentSet = keys.tailSet(new Key(ID, 0), true);
		for (Iterator<Key> it = currentSet.descendingIterator(); it.hasNext();) {
			counter++;
			Identifier value = it.next().getID();
			if (counter >= middle)
				return value;
		}

		throw new RuntimeException("Unreacheable code! " + ID + " " + predID + " " + middle + " " + size
				+ " " + smallGroupSize + " " + counter);
	}

	private Group getGroupToJoin() {
		double maxLoad = 0;
		Group toReturn = null;

		for (Group it : s.getAllGroups()) {
			double load = (it.getKeys() != null ? ((double) it.getKeys().size()) : 1D) / (it.size());
			if (load > maxLoad) {
				maxLoad = load;
				toReturn = it;
			}
		}
		return toReturn;
	}

	private void processDeathNotification(Endpoint source, DeathNotification object) {
		Node.logger.finest("removing worker from list");
		Group oldGroup = s.removeWorkerFromList(source);
		Node.logger.finest("removed worker from list");
		if (oldGroup != null) {
			Node.logger.finest("removing node from oldGroup");
			oldGroup.removeNode(source);
			Node.logger.finest("removed node from oldGroup");
			if (oldGroup.size() < MasterNode.MIN_REPLICATION) {
				Node.logger.finest("merging oldGroup");
				oldGroup.merge();
				Node.logger.log(Level.INFO, "merged: " + oldGroup + " into " + oldGroup.getSuccessor());
			}
		}
	}

	private Group createSeedGroup(Endpoint node) {
		TreeSet<Endpoint> set = new TreeSet<Endpoint>();
		set.add(node);
		Group toReturn = createGroup(Identifier.ZERO, set);
		return toReturn;
	}

	private Group createGroup(Identifier id, TreeSet<Endpoint> setNew) {
		Group toReturn = new Group(this, id, setNew);
		s.addToAllGroups(toReturn);
		ScheduledFuture<?> schedule = executor.scheduleAtFixedRate(new CheckGroupConnections(toReturn),
				MasterNode.KEEP_ALIVE_INTERVAL, MasterNode.KEEP_ALIVE_INTERVAL, TimeUnit.SECONDS);
		toReturn.setSchedule(schedule);
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
		Node.logger.log(Level.INFO, "checking connections");
		for (Endpoint it : group.getFinger())
			sendMessage(it, new GetInfo());
		Node.logger.log(Level.INFO, "checked connections");
	}

	private void processGetInfoReply(Endpoint source, GetInfoReply object) {
		Group group = s.getWorkerList().get(source);
		group.setKeys(new TreeSet<Key>(Arrays.asList(object.getKeys())));
	}

	void moveAllFrom(Group group, Group newGroup, TreeSet<Endpoint> toMove) {
		for (Endpoint it : toMove) {
			Group res = s.addToWorkerList(it, newGroup);
			if (res != group)
				Node.die("Node was in wrong group! Should be in " + this + " but was in " + res);
		}
	}
}

class MasterNodeInternalState {
	private final Map<Endpoint, Group> workerList = new TreeMap<Endpoint, Group>();
	private final HashSet<Group> allGroups = new HashSet<Group>();

	public Group addToWorkerList(Endpoint e, Group g) {
		return workerList.put(e, g);
	}

	public Map<Endpoint, Group> getWorkerList() {
		return Collections.unmodifiableMap(workerList);
	}

	public Set<Group> getAllGroups() {
		return Collections.unmodifiableSet(allGroups);
	}

	public Group removeWorkerFromList(Endpoint e) {
		return workerList.remove(e);
	}

	public Map<Endpoint, Group> getWorkerSet() {
		return Collections.unmodifiableMap(workerList);
	}

	public int getAllGroupsSize() {
		return allGroups.size();
	}

	public boolean addToAllGroups(Group group) {
		return allGroups.add(group); // TODO: test return
	}

	public boolean removeFromAllGroups(Group group) {
		return allGroups.remove(group); // TODO: test return
	}
}
