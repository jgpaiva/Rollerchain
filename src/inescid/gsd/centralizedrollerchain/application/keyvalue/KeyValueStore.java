package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.Configuration;
import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.WorkerNode;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.AllKeysReply;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.AllKeysRequest;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.GossipKeys;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.KeysRequest;
import inescid.gsd.centralizedrollerchain.events.DivideIDUpdate;
import inescid.gsd.centralizedrollerchain.events.GetInfo;
import inescid.gsd.centralizedrollerchain.events.GroupUpdate;
import inescid.gsd.centralizedrollerchain.events.JoinedNetwork;
import inescid.gsd.centralizedrollerchain.events.MergeIDUpdate;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.interfaces.EventReceiver;
import inescid.gsd.utils.Utils;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyValueStore implements EventReceiver {

	private final ConnectionManager connectionManager;
	private final WorkerNode owner;
	private final ScheduledExecutorService executor;
	static final Logger logger = Logger.getLogger(KeyValueStore.class.getName());
	private final KeyStorage keys;
	private final KeyMessageManager keyMessageManager;
	private final KeyRemovalManager keyRemovalManager;

	KeyValueStore(ConnectionManager manager, WorkerNode owner) {
		connectionManager = manager;
		keyMessageManager = new KeyMessageManager();
		keys = new KeyStorage();
		keyRemovalManager = new KeyRemovalManager(keys);
		this.owner = owner;
		executor = this.owner.getExecutor();
		executor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				nextRound();
			}
		}, Configuration.getRoundTime(), Configuration.getRoundTime(), TimeUnit.SECONDS);
		KeyValueStore.logger.log(Level.INFO, "Created new KeyValueStore node");
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
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
		else if (message instanceof AllKeysRequest)
			processAllKeysRequest(source, (AllKeysRequest) message);
		else if (message instanceof AllKeysReply)
			processAllKeysReply(source, (AllKeysReply) message);
		else if (message instanceof GossipKeys)
			processGossipKeys(source, (GossipKeys) message);
		else if (message instanceof GetInfo)
			processGetInfo(source, (GetInfo) message);
		else
			KeyValueStore.logger.log(Level.SEVERE, "Received unknown event: " + message);
	}

	private void processJoinedNetwork(Endpoint source, JoinedNetwork message) {
		if (message.getGroup().size() == 1)
			// I'm seed node
			createKeys();
		else {
			Endpoint randomDest = Utils.getRandomEl(message.getGroup(), owner.getEndpoint());
			sendMessage(randomDest, new AllKeysRequest(message.getGroup().getID(), message
					.getPredecessorID()));
		}
	}

	private void processGroupUpdate(Endpoint source, GroupUpdate message) {
		// IGNORE: the other guy should ask me for stuff
	}

	private void processDivideIDUpdate(Endpoint source, DivideIDUpdate message) {
		// IGNORE: keys will be deleted in time
	}

	private void processMergeIDUpdate(Endpoint source, MergeIDUpdate message) {
		// Force obtainal of keys by pushing a gossip
	}

	private void processAllKeysRequest(Endpoint source, AllKeysRequest message) {
		KeyContainer container = keys.getKeys(message.getPredecessorID(), message.getGroupID());
		sendMessage(source, new AllKeysReply(container));
	}

	private void processAllKeysReply(Endpoint source, AllKeysReply message) {
		keys.addAll(message.getContainer());
	}

	/**
	 * cyclic communication step
	 */
	private void nextRound() {
		StaticGroup myGroup = owner.getGroup();

		if (myGroup.getID() == null)
			return;

		Identifier predecessorID = owner.getPredecessorID();
		if (myGroup.size() > 1) {
			Endpoint randomDest = Utils.getRandomEl(myGroup, owner.getEndpoint());
			sendMessage(randomDest,
					new GossipKeys(myGroup.getID(), predecessorID, keys.getKeyListing()));
		}

		keyMessageManager.nextRound();
		keyRemovalManager.nextRound(predecessorID, myGroup.getID());
	}

	private void processGossipKeys(Endpoint source, GossipKeys message) {
		if (owner.getGroup() == null)
			return;

		KeyStorage otherKeys = message.getKeyListing().getKeyStorage();

		keyMessageManager.removeRequestedKeys(otherKeys);
		otherKeys.removeAll(keys);
		otherKeys.filter(owner.getGroup(), owner.getPredecessorID());

		if (otherKeys.size() > 0) {
			KeyListing toRequest = otherKeys.getKeyListing();
			KeysRequest requestMessage = new KeysRequest(toRequest);
			keyMessageManager.registerMessage(requestMessage);
			sendMessage(source, requestMessage);
		}
	}

	private void processGetInfo(Endpoint source, GetInfo message) {
		owner.sendMessage(
				source,
				new GetInfoReply((Key[]) keys.getKeys(owner.getPredecessorID(), owner
						.getGroup().getID()).getKeys().toArray()));
	}

	private void createKeys() {
		keys.init();
	}

	private void sendMessage(Endpoint dest, UpperLayerMessage msg) {
		owner.sendMessage(dest, msg);
	}
}
