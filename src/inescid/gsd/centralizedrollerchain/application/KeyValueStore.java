package inescid.gsd.centralizedrollerchain.application;

import inescid.gsd.centralizedrollerchain.WorkerNode;
import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyContainer;
import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyStorage;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.GetAllKeys;
import inescid.gsd.centralizedrollerchain.application.keyvalue.events.KeyMessage;
import inescid.gsd.centralizedrollerchain.events.DivideIDUpdate;
import inescid.gsd.centralizedrollerchain.events.GroupUpdate;
import inescid.gsd.centralizedrollerchain.events.JoinedNetwork;
import inescid.gsd.centralizedrollerchain.events.MergeIDUpdate;
import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;
import inescid.gsd.centralizedrollerchain.utils.NamedThreadFactory;
import inescid.gsd.centralizedrollerchain.utils.PriorityPair;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.interfaces.EventReceiver;
import inescid.gsd.utils.Utils;

import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyValueStore implements EventReceiver {

	private final ConnectionManager connectionManager;
	private final WorkerNode owner;
	private final ScheduledExecutorService executor;
	private final PriorityBlockingQueue<PriorityPair<Endpoint, Object>> queue = new PriorityBlockingQueue<PriorityPair<Endpoint, Object>>();
	static final Logger logger = Logger.getLogger(KeyValueStore.class.getName());
	private final KeyStorage keys;

	KeyValueStore(ConnectionManager manager, WorkerNode owner) {
		connectionManager = manager;
		this.owner = owner;
		executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory(
				"KeyValueStoreThreadPool_"));
		KeyValueStore.logger.log(Level.INFO, "Created new Node");
		keys = new KeyStorage();
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		KeyValueStore.logger.log(Level.FINEST, "Queuing event from: " + source + " / " + message);
		if (message instanceof Event)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 0));
		else if (message instanceof UpperLayerMessage)
			queue.add(new PriorityPair<Endpoint, Object>(source, message, 0));
		else
			KeyValueStore.logger.log(Level.SEVERE, "Received unknown event: " + message);
		KeyValueStore.logger.log(Level.FINEST, "Queued event from: " + source + " / " + message);
		executor.submit(new Runnable() {
			@Override
			public void run() {
				takeFromQueue();
			}
		});
	}

	private void takeFromQueue() {
		PriorityPair<Endpoint, Object> el = null;
		if (queue.size() == 0) {
			KeyValueStore.logger.log(Level.SEVERE, "queue was empty. Quitting");
			System.exit(-1);
		}
		try {
			el = queue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			KeyValueStore.logger.log(Level.SEVERE, "error while taking from the queue. Quitting");
			System.exit(-1);
		}
		processEventInternal(el.getFst(), el.getSnd());
	}

	private void processEventInternal(Endpoint source, Object message) {
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
		else if (message instanceof GetAllKeys)
			processGetAllKeys(source, (GetAllKeys) message);
		else if (message instanceof KeyMessage)
			processKeyMessage(source, (KeyMessage) message);
		else
			KeyValueStore.logger.log(Level.SEVERE, "Received unknown event: " + message);
	}

	private void processJoinedNetwork(Endpoint source, JoinedNetwork message) {
		if (message.getGroup().size() == 1)
			// I'm seed node
			createKeys();
		else {
			Endpoint randomDest = Utils.getRandomEl(message.getGroup());
			sendMessage(randomDest, new GetAllKeys(message.getGroup().getID(), message
					.getPredecessorID()));
		}
	}

	private void processGroupUpdate(Endpoint source, GroupUpdate message) {
		// IGNORE: the other guy should ask me for stuff
	}

	private void processDivideIDUpdate(Endpoint source, DivideIDUpdate message) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not yet implemented");
	}

	private void processMergeIDUpdate(Endpoint source, MergeIDUpdate message) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not yet implemented");
	}

	private void processGetAllKeys(Endpoint source, GetAllKeys message) {
		KeyContainer container = keys.getKeys(message.getPredecessorID(), message.getID());
		sendMessage(source, new KeyMessage(container));
	}

	private void processKeyMessage(Endpoint source, KeyMessage message) {
		keys.addAll(message.getContainer());
	}

	private void createKeys() {
		keys.init();
	}

	private void sendMessage(Endpoint dest, UpperLayerMessage msg) {
		owner.sendMessage(dest, msg);
	}
}
