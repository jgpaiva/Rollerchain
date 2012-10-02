package inescid.gsd.centralizedrollerchain.application.keyvalue.test;

import inescid.gsd.centralizedrollerchain.MasterNode;
import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.centralizedrollerchain.WorkerNode;
import inescid.gsd.centralizedrollerchain.application.keyvalue.KeyValueStore;
import inescid.gsd.transport.Endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalTest {
	private static final int MAX_WORKERS = 20;

	public static void main(String[] args) {
		final Logger logger = Logger.getLogger(
				LocalTest.class.getName());

		Logger.getLogger(Node.class.getName()).setLevel(Level.INFO);
		Logger.getLogger(KeyValueStore.class.getPackage().getName()).setLevel(Level.ALL);
		LocalTest.setHandlerLevel();

		Logger otherLogger = Logger.getLogger("inescid.gsd");
		FileHandler fileHandler = null;
		try {
			fileHandler = new FileHandler("node.log", true);
		} catch (SecurityException e1) {
			e1.printStackTrace();
			System.exit(-2);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(-3);
		}
		otherLogger.addHandler(fileHandler);
		otherLogger.setLevel(Level.ALL);
		fileHandler.setLevel(Level.ALL);
		fileHandler.setFormatter(new java.util.logging.SimpleFormatter());

		final Endpoint masterEndpoint = new Endpoint("localhost", 8090);
		final MasterNode masterNode = new MasterNode(masterEndpoint);
		masterNode.init();
		logger.log(Level.INFO, "Created master");
		LocalTest.sleep(2);

		ArrayList<WorkerNode> workers = new ArrayList<WorkerNode>();
		for (int it = 0; it < LocalTest.MAX_WORKERS; it++)
			workers.add(new WorkerNode(new Endpoint("localhost", 8090 + 1 + it), masterEndpoint,
					new KeyValueStore()));

		for (WorkerNode it : workers) {
			it.init();
			LocalTest.sleep(3);
		}

		logger.log(Level.INFO, "Created all worker nodes");
	}

	private static void sleep(int time) {
		try {
			Thread.sleep(time * 1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	private static void setHandlerLevel() {
		// get the top Logger:
		Logger topLogger = java.util.logging.Logger.getLogger("");

		// Handler for console (reuse it if it already exists)
		Handler consoleHandler = null;
		// see if there is already a console handler
		for (Handler handler : topLogger.getHandlers())
			if (handler instanceof ConsoleHandler) {
				// found the console handler
				consoleHandler = handler;
				break;
			}

		if (consoleHandler == null) {
			// there was no console handler found, create a new one
			consoleHandler = new ConsoleHandler();
			topLogger.addHandler(consoleHandler);
		}
		// set the console handler to fine:
		consoleHandler.setLevel(Level.ALL);
	}
}
