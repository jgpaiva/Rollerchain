package inescid.gsd.centralizedrollerchain.test;

import inescid.gsd.centralizedrollerchain.MasterNode;
import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.centralizedrollerchain.WorkerNode;
import inescid.gsd.transport.Endpoint;

public class DistributedTest {
	public static void main(String[] args) {
		if ((args.length < 2) || (args.length > 4) || (args.length == 3)) {
			System.err.println("Invalid parameters!");
			System.err
					.println("usage: DistributedTest <local hostname> <local port> [<master hostname> <master port>]");
			System.exit(-1);
		}

		String myHostname = args[0];
		int myPort = Integer.parseInt(args[1]);
		Endpoint myEndpoint = new Endpoint(myHostname, myPort);

		Endpoint masterEndpoint = null;
		if (args.length == 4) {
			String masterHostname = args[0];
			int masterPort = Integer.parseInt(args[1]);
			masterEndpoint = new Endpoint(masterHostname, masterPort);
		}

		Node myNode;
		if (masterEndpoint == null)
			// is master
			myNode = new MasterNode(myEndpoint);
		else
			// is worker
			myNode = new WorkerNode(myEndpoint, masterEndpoint);

		System.out.println("Starting Node");
		myNode.start();
		System.out.println("Node terminated");
	}
}
