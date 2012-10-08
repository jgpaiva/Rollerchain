package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Configuration;
import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.utils.FileOutput;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FiliationMngr {
	private final KeyValueStore owner;
	private long counter;
	private NodeGroupPair lastSentPair;

	private final TreeSet<NodeGroupPair> orderedSites = new TreeSet<NodeGroupPair>();
	private final TreeMap<Endpoint, NodeGroupPair> knownSites = new TreeMap<Endpoint, NodeGroupPair>();
	private final Logger logger = Logger.getLogger(FiliationMngr.class.getName());
	private final FileOutput writer;

	public FiliationMngr(KeyValueStore owner) {
		this.owner = owner;
		counter = 0;
		lastSentPair = null;
		writer = new FileOutput(owner.getEndpoint(), this.getClass());
	}

	public void nextRound(StaticGroup group, StaticGroup predecessor, StaticGroup successor) {
		if (group.size() > 1) {
			TreeSet<Endpoint> choices = new TreeSet<Endpoint>();
			for (Endpoint it : knownSites.keySet())
				choices.add(it);
			choices.add(Utils.getRandomEl(group));
			if (predecessor != null)
				choices.add(Utils.getRandomEl(predecessor));
			if (successor != null)
				choices.add(Utils.getRandomEl(successor));

			Endpoint randDest = Utils.getRandomEl(choices, owner.getEndpoint());

			if ((lastSentPair == null) || !lastSentPair.getGroupID().equals(group.getID())) {
				lastSentPair = new NodeGroupPair(owner.getEndpoint(), group.getID(), counter);
				counter++;
			}
			owner.sendMessage(randDest, new NodeGroupPairList(getSample()));
		}

		// delete old pointers
		ArrayList<NodeGroupPair> toRemove = new ArrayList<NodeGroupPair>();
		for (NodeGroupPair it : knownSites.values())
			if (it.decrTTL() <= 0)
				toRemove.add(it);
		if (toRemove.size() > 0) {
			for (NodeGroupPair it : toRemove)
				removeEndpoint(it.getEndpoint());
			logger.log(Level.FINE, "Removed pointers: " + toRemove);
		}
		logger.log(Level.INFO, owner.getEndpoint() + " knows " + knownSites.size() + knownSites);

		writer.status("known: " + knownSites.size() + " removed:" + toRemove.size() + " known detail:"
				+ knownSites + " removed detail:" + toRemove);
	}

	public void processNodeGroupPairList(Endpoint source, NodeGroupPairList message) {
		ArrayList<NodeGroupPair> toReply = new ArrayList<NodeGroupPair>();
		for (NodeGroupPair newPair : Arrays.asList(message.getSample())) {
			if (newPair.getEndpoint().equals(owner.getEndpoint())) {
				if (newPair.getVersion() < lastSentPair.getVersion())
					toReply.add(lastSentPair);
				continue;
			}

			NodeGroupPair temp = knownSites.get(newPair.getEndpoint());
			if (temp == null)
				addEndpoint(newPair);
			else if (temp.getVersion() < newPair.getVersion()) {
				removeEndpoint(temp.getEndpoint());
				addEndpoint(newPair);
			} else if (temp.getVersion() > newPair.getVersion())
				toReply.add(temp);
			else if (temp.getTTL() < newPair.getTTL())
				temp.setTTL(newPair.getTTL());
		}

		owner.sendMessage(source, new NodeGroupPairListReply(
				getBiasedSample(toReply, message.getSample(), source)));
	}

	public void processNodeGroupPairListReply(Endpoint source, NodeGroupPairListReply message) {
		for (NodeGroupPair newPair : Arrays.asList(message.getSample())) {
			if (newPair.getEndpoint().equals(owner.getEndpoint()))
				KeyValueStore.die("my endpoint was in a reply. should never happen");

			NodeGroupPair temp = knownSites.get(newPair.getEndpoint());
			if (temp == null)
				addEndpoint(newPair);
			else if (temp.getVersion() < newPair.getVersion()) {
				removeEndpoint(temp.getEndpoint());
				addEndpoint(newPair);
			} else if (temp.getTTL() < newPair.getTTL())
				temp.setTTL(newPair.getTTL());
		}
	}

	@SuppressWarnings("unused")
	NodeGroupPair[] getSample() {
		@SuppressWarnings("unchecked")
		TreeSet<NodeGroupPair> knownSitesCopy = (TreeSet<NodeGroupPair>) orderedSites.clone();
		ArrayList<NodeGroupPair> toSend = new ArrayList<NodeGroupPair>(Configuration.getMaxShuffleSize());
		for (int it : Utils.range(Math.min(Configuration.getMaxShuffleSize() - 1, knownSitesCopy.size()))) {
			// TODO: very inefficient
			NodeGroupPair el = Utils.removeRandomEl(knownSitesCopy);
			toSend.add(el);
		}
		toSend.add(lastSentPair);
		return toSend.toArray(new NodeGroupPair[0]);
	}

	@SuppressWarnings("unused")
	NodeGroupPair[] getBiasedSample(ArrayList<NodeGroupPair> toReply, NodeGroupPair[] nodeGroupPairs,
			Endpoint senderEndpoint) {
		@SuppressWarnings("unchecked")
		TreeSet<NodeGroupPair> knownSitesCopy = (TreeSet<NodeGroupPair>) orderedSites.clone();
		knownSitesCopy.removeAll(toReply);
		knownSitesCopy.removeAll(Arrays.asList(nodeGroupPairs));
		for (Iterator<NodeGroupPair> it = knownSitesCopy.iterator(); it.hasNext();)
			if (it.next().getEndpoint().equals(senderEndpoint)) {
				it.remove();
				break;
			}

		ArrayList<NodeGroupPair> toSend = new ArrayList<NodeGroupPair>(Configuration.getMaxShuffleSize());
		toSend.addAll(toReply);
		for (int it : Utils.range(Math.min(Configuration.getMaxShuffleSize() - toReply.size(),
				knownSitesCopy.size()))) {
			NodeGroupPair el = Utils.removeRandomEl(knownSitesCopy);
			toSend.add(el);
		}
		return toSend.toArray(new NodeGroupPair[0]);
	}

	public void processDeathNotification(Endpoint source, DeathNotification message) {
		removeEndpoint(source);
	}

	private void addEndpoint(NodeGroupPair newPair) {
		knownSites.put(newPair.getEndpoint(), newPair);
		orderedSites.add(newPair);
	}

	private void removeEndpoint(Endpoint it) {
		NodeGroupPair temp = knownSites.remove(it);
		if (temp != null)
			orderedSites.remove(temp);
	}
}

class NodeGroupPair implements Serializable, Comparable<NodeGroupPair> {
	private static final long serialVersionUID = 4110611505490552726L;
	private final Endpoint endpoint;
	private final Identifier groupID;
	private final long version;
	private int ttl;

	NodeGroupPair(Endpoint endpoint, Identifier groupID, long version) {
		this.endpoint = endpoint;
		this.groupID = groupID;
		this.version = version;
		setTTL(Configuration.getShuffleTTL());
	}

	public long getVersion() {
		return version;
	}

	public Identifier getGroupID() {
		return groupID;
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	@Override
	public int compareTo(NodeGroupPair arg0) {
		int res = groupID.compareTo(arg0.groupID);
		if (res == 0)
			return endpoint.compareTo(arg0.endpoint);
		else
			return res;
	}

	@Override
	public boolean equals(Object arg0) {
		return (arg0 instanceof NodeGroupPair) && groupID.equals(((NodeGroupPair) arg0).groupID)
				&& endpoint.equals(((NodeGroupPair) arg0).endpoint);
	}

	@Override
	public int hashCode() {
		return groupID.hashCode() + endpoint.hashCode();
	}
	public int decrTTL() {
		return --ttl;
	}

	public int getTTL() {
		return ttl;
	}

	public void setTTL(int ttl) {
		this.ttl = ttl;
	}

	@Override
	public String toString() {
		return endpoint + "=" + groupID + ";" + version;
	}
}
