package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Configuration;
import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.WorkerNode;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.AllKeysRequest;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.GossipKeys;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.KeysReply;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.KeysRequest;
import inescid.gsd.centralizedrollerchain.events.DivideIDUpdate;
import inescid.gsd.centralizedrollerchain.events.GetInfo;
import inescid.gsd.centralizedrollerchain.events.GroupUpdate;
import inescid.gsd.centralizedrollerchain.events.JoinedNetwork;
import inescid.gsd.centralizedrollerchain.events.MergeIDUpdate;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayer;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.utils.Utils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyValueStore implements UpperLayer {

	private WorkerNode owner;
	private ScheduledExecutorService executor;
	private static final Logger logger = Logger.getLogger(KeyValueStore.class.getName());
	private final KeyStorage keys;
	private final KeyMessageMngr keyMessageManager;
	private KeyRemovalMngr keyRemovalManager;
	private FiliationMngr filiationManager;

	public KeyValueStore() {
		keyMessageManager = new KeyMessageMngr();
		keys = new KeyStorage();
		owner = null;
		executor = null;
		keyRemovalManager = null;
		KeyValueStore.logger.log(Level.INFO, "Created new KeyValueStore node");
	}

	@Override
	public void init(Node owner) {
		this.owner = (WorkerNode) owner;
		executor = owner.getExecutor();
		filiationManager = new FiliationMngr(this);
		KeyValueStore.logger.log(Level.INFO, "Initialized KeyValueStore");
		keyRemovalManager = new KeyRemovalMngr(keys, owner.getEndpoint());
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		if (executor == null)
			KeyValueStore.die("KeyValueStore not initialized");

		KeyValueStore.logger.log(Level.FINE, "KeyValue " + " R: " + source + " / "
				+ message);
		if (message instanceof JoinedNetwork)
			processJoinedNetwork(source, (JoinedNetwork) message);
		else if (message instanceof GroupUpdate)
			processGroupUpdate(source, (GroupUpdate) message);
		else if (message instanceof DivideIDUpdate)
			processDivideIDUpdate(source, (DivideIDUpdate) message);
		else if (message instanceof MergeIDUpdate)
			processMergeIDUpdate(source, (MergeIDUpdate) message);
		else if (message instanceof KeysRequest)
			processKeysRequest(source, (KeysRequest) message);
		else if (message instanceof AllKeysRequest)
			processAllKeysRequest(source, (AllKeysRequest) message);
		else if (message instanceof KeysReply)
			processKeysReply(source, (KeysReply) message);
		else if (message instanceof GossipKeys)
			processGossipKeys(source, (GossipKeys) message);
		else if (message instanceof GetInfo)
			processGetInfo(source, (GetInfo) message);
		else if (message instanceof NodeGroupPairList)
			filiationManager.processNodeGroupPairList(source, (NodeGroupPairList) message);
		else if (message instanceof NodeGroupPairListReply)
			filiationManager.processNodeGroupPairListReply(source, (NodeGroupPairListReply) message);
		else if (message instanceof DeathNotification)
			filiationManager.processDeathNotification(source, (DeathNotification) message);
		else
			KeyValueStore.die("Received unknown event: " + message);
	}

	private void processJoinedNetwork(Endpoint source, JoinedNetwork message) {
		if (message.getGroup().size() == 1) {
			createSeedKeys();// I'm seed node, create seed keys
			KeyValueStore.logger.log(Level.INFO, "seed: created " + keys.size() + " keys");
		} else {
			Endpoint randomDest = Utils.getRandomEl(message.getGroup(), owner.getEndpoint());
			sendMessage(randomDest, new AllKeysRequest(message.getGroup().getID(), message
					.getPredecessorID()));
			KeyValueStore.logger.log(Level.INFO, "Requested keys from: " + randomDest);
		}
	}

	private void processGroupUpdate(Endpoint source, GroupUpdate message) {
		// IGNORE: the other guy should ask me for stuff
	}

	private void processDivideIDUpdate(Endpoint source, DivideIDUpdate message) {
		// IGNORE: keys will be deleted in time
	}

	private void processMergeIDUpdate(Endpoint source, MergeIDUpdate message) {
		// Speed up obtainal of keys by pushing a gossip
		if (message.getPredecessorGroup() != null)
			sendGossip(message.getCurrentGroup(), message.getPredecessorGroup().getID());
	}

	private void processKeysRequest(Endpoint source, KeysRequest message) {
		KeyContainer toReturn = keys.get(message.getRequested());
		sendMessage(source, new KeysReply(toReturn, message));
	}

	private void processAllKeysRequest(Endpoint source, AllKeysRequest message) {
		KeyContainer container = keys.getKeys(message.getPredecessorID(), message.getGroupID());
		sendMessage(source, new KeysReply(container, message));
	}

	private void processKeysReply(Endpoint source, KeysReply message) {
		keys.addAll(message.getContainer());
		keyMessageManager.deRegisterMessage(message.getMessageID());
	}

	private void processGetInfo(Endpoint source, GetInfo message) {
		KeyValueStore.logger.log(Level.INFO, "STATUS: " + keys.size() + " keys. id:"
				+ owner.getGroup().getID() + " predID:" + owner.getPredecessorID());

		sendMessage(source, new GetInfoReply(keys.getKeys(owner.getPredecessorID(), owner
				.getGroup().getID()).getKeys().toArray(new Key[0]), owner.getGroup(), owner.getPredecessor(),
				owner.getSuccessor()));
	}

	/**
	 * Cyclic communication step. Repeated every
	 * {@link Configuration#getRoundTime()}.
	 */
	@Override
	public void nextRound() {
		KeyValueStore.logger.log(Level.INFO, owner.getEndpoint() + " Entering nextRound()");
		StaticGroup myGroup = owner.getGroup();
		if (myGroup == null)
			return;

		Identifier predecessorID = owner.getPredecessorID();

		filiationManager.nextRound(myGroup, owner.getPredecessor(), owner.getSuccessor());

		if (myGroup.getID() != null) {
			if (predecessorID != null)
				keyRemovalManager.nextRound(predecessorID, myGroup.getID());
			sendGossip(myGroup, predecessorID);
			keyMessageManager.nextRound();
		}
		KeyValueStore.logger.log(Level.INFO, owner.getEndpoint() + " Finished nextRound(). Group was "
				+ myGroup.getID() + " predecessorID was " + predecessorID);
	}

	public void sendGossip(StaticGroup myGroup, Identifier predecessorID) {
		if (myGroup.size() > 1) {
			Endpoint randomDest = Utils.getRandomEl(myGroup, owner.getEndpoint());
			KeyListing temp = keys.getKeyListing();
			GossipKeys msg = new GossipKeys(myGroup.getID(), predecessorID, temp);
			sendMessage(randomDest, msg);
		}
	}

	private void processGossipKeys(Endpoint source, GossipKeys message) {
		if (owner.getGroup() == null)
			return;

		KeyStorage otherKeys = message.getKeyListing().getKeyStorage();

		keyMessageManager.removeRequestedKeys(otherKeys);
		otherKeys.removeAll(keys);
		if (owner.getPredecessorID() != null)
			otherKeys.filter(owner.getGroup(), owner.getPredecessorID());

		if (otherKeys.size() > 0) {
			KeyListing toRequest = otherKeys.getKeyListing();
			KeysRequest requestMessage = new KeysRequest(toRequest);
			keyMessageManager.registerMessage(requestMessage);
			sendMessage(source, requestMessage);
		}
	}

	private void createSeedKeys() {
		keys.init();
	}

	void sendMessage(Endpoint dest, UpperLayerMessage msg) {
		owner.sendMessage(dest, msg);
	}

	public static void die(String string) {
		Thread.dumpStack();
		System.out.println("SEVERE ERROR: " + string);
		System.err.println("SEVERE ERROR: " + string);
		KeyValueStore.logger.log(Level.SEVERE, string);
		System.exit(-29);
	}

	protected static void die(Throwable t) {
		t.printStackTrace();
		KeyValueStore.die("Exception found when processing job: " + t);
	}

	public Endpoint getEndpoint() {
		if (owner == null)
			KeyValueStore.die("Shoule never happen");
		return owner.getEndpoint();
	}
}
