package inescid.gsd.centralizedrollerchain.test;

import inescid.gsd.centralizedrollerchain.MasterNode;
import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.centralizedrollerchain.WorkerNode;
import inescid.gsd.transport.Endpoint;

import java.io.IOException;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DistributedTest {
	public static void main(String[] args) {
		if (args.length != 4) {
			System.err.println("Invalid parameters!");
			System.err
					.println("usage: DistributedTest <local hostname> <local port> <master hostname> <master port>");
			System.exit(-1);
		}

		// Logger logger =
		// Logger.getLogger("inescid.gsd.centralizedrollerchain");
		Logger logger = Logger.getLogger("inescid.gsd");
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
		logger.addHandler(fileHandler);
		logger.setLevel(Level.ALL);
		fileHandler.setLevel(Level.ALL);
		fileHandler.setFormatter(new java.util.logging.SimpleFormatter());

		String myHostname = args[0];
		int myPort = Integer.parseInt(args[1]);
		Endpoint myEndpoint = new Endpoint(myHostname, myPort);

		Endpoint masterEndpoint = null;
		String masterHostname = args[2];
		int masterPort = Integer.parseInt(args[3]);
		masterEndpoint = new Endpoint(masterHostname, masterPort);

		Node myNode;
		if (masterEndpoint.equals(myEndpoint)) {
			myNode = new MasterNode(myEndpoint); // is master
			logger.log(Level.INFO, "Now starting server");
		} else {
			DistributedTest.sleep(2000);
			myNode = new WorkerNode(myEndpoint, masterEndpoint); // is worker

			System.out.println("Starting Node");

			Random r = new Random();
			DistributedTest.sleep(60 * 1000);
			while (true) {
				DistributedTest.sleep(1000 + r.nextInt(10000));
				if (r.nextDouble() < 0.1) {
					logger.log(Level.INFO, "Now terminating");
					System.exit(0);
				}
			}
		}
	}

	private static void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
