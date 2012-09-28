package inescid.gsd.centralizedrollerchain.test;

import inescid.gsd.centralizedrollerchain.MasterNode;
import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.centralizedrollerchain.WorkerNode;
import inescid.gsd.transport.Endpoint;

import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalTest {
	private static final int MAX_WORKERS = 50;

	public static void main(String[] args) {
		final Logger logger = Logger.getLogger(
				LocalTest.class.getName());

		Logger.getLogger(Node.class.getName()).setLevel(Level.INFO);
		LocalTest.setHandlerLevel();

		final Endpoint masterEndpoint = new Endpoint("localhost", 8090);
		final MasterNode masterNode = new MasterNode(masterEndpoint);
		masterNode.init();
		logger.log(Level.INFO, "Created master");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		ArrayList<WorkerNode> workers = new ArrayList<WorkerNode>();
		for (int it = 0; it < LocalTest.MAX_WORKERS; it++)
			workers.add(new WorkerNode(new Endpoint("localhost", 8090 + 1 + it), masterEndpoint));
		for (WorkerNode it : workers)
			it.init();

		logger.log(Level.INFO, "Created all worker nodes");
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
