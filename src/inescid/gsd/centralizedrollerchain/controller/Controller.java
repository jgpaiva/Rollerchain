package inescid.gsd.centralizedrollerchain.controller;

import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.centralizedrollerchain.application.keyvalue.GetInfoReply;
import inescid.gsd.centralizedrollerchain.application.keyvalue.Key;
import inescid.gsd.centralizedrollerchain.events.GetInfo;
import inescid.gsd.centralizedrollerchain.events.InstantDeath;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.utils.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class Controller extends Node {
	private final ArrayList<Endpoint> endpoints;
	private final TreeMap<Endpoint, List<Key>> infomap = new TreeMap<Endpoint, List<Key>>();

	public Controller(Endpoint endpoint, ArrayList<Endpoint> endpoints) {
		super(endpoint);
		this.endpoints = endpoints;
	}

	@Override
	protected void processEventInternal(Endpoint src, Object msg) {
		CopyableEndpoint source = Controller.toCEndpoint(src);
		if (msg instanceof GetInfoReply) {
			System.out.println("Node " + source + " has " + ((GetInfoReply) msg).getKeys().length + " keys");
			addKeys(source, (GetInfoReply) msg);
		} else if (msg instanceof DeathNotification) {
			infomap.remove(source);
			endpoints.remove(source);
			System.out.println("found that " + source + " is dead");
		}
	}

	private void addKeys(CopyableEndpoint source, GetInfoReply msg) {
		List<Key> list = Arrays.asList(msg.getKeys());
		infomap.put(source, list);

		boolean done = true;
		for (Endpoint it : endpoints)
			if (!infomap.containsKey(it)) {
				done = false;
				break;
			}
		if (done) {
			double average = 0;
			for (List<Key> it : infomap.values())
				average += it.size();
			average /= infomap.size();
			System.out.println("average keys per node: " + average);
		}
	}

	@Override
	protected void nextRound() {
		// ignore next round
	}

	class KillCommand implements Runnable {
		private int toKill;
		private final int delay;

		public KillCommand(StringTokenizer tokenizer, int delay) {
			this.delay = delay;
			try {
				if (!tokenizer.hasMoreTokens())
					toKill = 1;
				else
					toKill = Integer.valueOf(tokenizer.nextToken());
				getExecutor().submit(this);
			} catch (Exception e) {
				System.out.println("Something went wrong with KillCommand: " + e);
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			for (int it : Utils.range(toKill)) {
				Endpoint currentKill = Utils.removeRandomEl(endpoints);
				sendObject(currentKill, new InstantDeath());
				System.out.println((it + 1) + ": killed " + currentKill);
				Utils.threadSleep(delay);
			}
			System.out.println("killed " + toKill + " nodes");
		}
	}

	class AverageCommand implements Runnable {
		public AverageCommand() {
			getExecutor().submit(this);
		}

		@Override
		public void run() {
			infomap.clear();
			for (Endpoint it : endpoints)
				sendMessage(it, new GetInfo());
		}
	}

	class InfoCommand implements Runnable {
		private Endpoint endpoint;

		public InfoCommand(StringTokenizer tokenizer) {
			try {
				String copyableEndpoint = tokenizer.nextToken();
				endpoint = Controller.createCopyableEndpoint(copyableEndpoint);
				getExecutor().submit(this);
			} catch (Exception e) {
				System.out.println("Something went wrong with KillCommand: " + e);
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			sendMessage(endpoint, new GetInfo());
		}

	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Wrong arguments. usage: " + "Controller <node list file>");
			System.exit(-1);
		}
		System.out.println(new Date() + "  ****** INIT *****  ");
		System.err.println(new Date() + "  ****** INIT *****  ");

		System.out.println("reading file " + args[0]);
		BufferedReader reader = null;
		ArrayList<Endpoint> endpoints = new ArrayList<Endpoint>();
		try {
			reader = new BufferedReader(new FileReader(args[0]));
			String line;
			while ((line = reader.readLine()) != null) {
				StringTokenizer tok = new StringTokenizer(line);
				String hostname = tok.nextToken();
				int port = Integer.valueOf(tok.nextToken());
				Endpoint e = new CopyableEndpoint(hostname, port);
				endpoints.add(e);
			}
			endpoints.remove(0); // remove the master

			Controller c = new Controller(new Endpoint("kalium.gsd.inesc-id.pt", 9091), endpoints);
			c.init();

			c.keyboardWait();
		} catch (Exception e) {
			System.out.println("error reading file: " + e);
			System.exit(-1);
		}
	}

	private void keyboardWait() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				System.out.println(new Date() + "  **** quitting ****");
				System.exit(0);
			}
			StringTokenizer tokenizer = new StringTokenizer(line);
			String cmd = tokenizer.nextToken().trim().toLowerCase();

			if (cmd.equals("kill") || cmd.equals("k"))
				new KillCommand(tokenizer, 0);
			else if (cmd.equals("killwait") || cmd.equals("kw"))
				new KillCommand(tokenizer, 5);
			else if (cmd.equals("info") || cmd.equals("i"))
				new InfoCommand(tokenizer);
			else if (cmd.equals("list") || cmd.equals("l"))
				System.out.println(endpoints.size() + "" + endpoints);
			else if (cmd.equals("average") || cmd.equals("a"))
				new AverageCommand();
		}
	}

	public static CopyableEndpoint createCopyableEndpoint(String hostPort) {
		int temp = hostPort.indexOf("_");
		String host = hostPort.substring(0, temp);
		int port = Integer.valueOf(hostPort.substring(temp + 1, hostPort.length()));
		return new CopyableEndpoint(host, port);
	}

	public static CopyableEndpoint toCEndpoint(Endpoint e) {
		return new CopyableEndpoint(e.host, e.port);
	}

	static class CopyableEndpoint extends Endpoint {
		public CopyableEndpoint(String host, int port) {
			super(host, port);
		}

		private static final long serialVersionUID = 1L;

		@Override
		public String toString() {
			return host + "_" + port;
		}
	}
}
