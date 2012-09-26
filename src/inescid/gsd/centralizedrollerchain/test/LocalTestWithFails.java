package inescid.gsd.centralizedrollerchain.test;

import inescid.gsd.centralizedrollerchain.MasterNode;
import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.centralizedrollerchain.WorkerNode;
import inescid.gsd.centralizedrollerchain.internalevents.KillEvent;
import inescid.gsd.transport.Endpoint;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalTestWithFails {
	private static final int MAX_WORKERS = 20;

	public static void main(String[] args) {
		final Logger logger = Logger.getLogger(
				LocalTestWithFails.class.getName());
		Endpoint masterEndpoint = new Endpoint("localhost", 8090);

		final MasterNode masterNode = new MasterNode(masterEndpoint);
		Logger.getLogger(Node.class.getName()).setLevel(Level.ALL);
		LocalTestWithFails.setHandlerLevel();

		logger.log(Level.INFO, "Created master");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		ArrayList<WorkerNode> workers = new ArrayList<WorkerNode>();
		for (int it = 0; it < LocalTestWithFails.MAX_WORKERS; it++)
			workers.add(new WorkerNode(new Endpoint("localhost", 8090 + 1 + it), masterEndpoint));

		logger.log(Level.INFO, "Created all worker nodes");

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		int deadNodes = 0;
		Random r = new Random();
		for (WorkerNode it : workers)
			if (r.nextDouble() < 0.3) {
				it.processEvent(it.getEndpoint(), new KillEvent());
				deadNodes++;
			}
		System.out.println("Started deaths");

		masterNode.checkIntegrity();

		try {
			Thread.sleep(40000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		masterNode.checkIntegrity();

		ArrayList<WorkerNode> newWorkers = new ArrayList<WorkerNode>();
		for (int it = 0; it < deadNodes; it++)
			newWorkers.add(new WorkerNode(new Endpoint("localhost", 8090 + 1
					+ LocalTestWithFails.MAX_WORKERS + it), masterEndpoint));

		System.out.println("Started replacements");
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
