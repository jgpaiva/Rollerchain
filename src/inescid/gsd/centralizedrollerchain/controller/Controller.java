package inescid.gsd.centralizedrollerchain.controller;

import inescid.gsd.centralizedrollerchain.Identifier;
import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.centralizedrollerchain.application.keyvalue.GetInfoReply;
import inescid.gsd.centralizedrollerchain.events.GetInfo;
import inescid.gsd.centralizedrollerchain.events.InstantDeath;
import inescid.gsd.centralizedrollerchain.internalevents.GetNodeList;
import inescid.gsd.centralizedrollerchain.internalevents.GetNodeListReply;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class Controller extends Node {
	private static final int START_PORT = 8081;
	private static final String SCRIPT_DIR = System.getProperty("user.home") + "/tmp/realimpl";
	private final Endpoint masterEndpoint;
	private final TreeSet<CopyableEndpoint> upEndpoints;
	private final TreeSet<CopyableEndpoint> downEndpoints = new TreeSet<CopyableEndpoint>();
	private final TreeMap<CopyableEndpoint, GetInfoReply> infomap = new TreeMap<CopyableEndpoint, GetInfoReply>();
	private final TreeMap<String, Set<CopyableEndpoint>> hostnames;
	private final String nowString;

	public Controller(Endpoint endpoint, Endpoint masterEndpoint, ArrayList<CopyableEndpoint> endpoints,
			ArrayList<String> hostnames, String nowString) {
		super(endpoint);
		upEndpoints = new TreeSet<CopyableEndpoint>();
		upEndpoints.addAll(endpoints);
		this.hostnames = new TreeMap<String, Set<CopyableEndpoint>>();
		for (String it : hostnames)
			this.hostnames.put(it, new TreeSet<CopyableEndpoint>());
		for (CopyableEndpoint it : endpoints)
			Controller.countEndpoint(it, this.hostnames);
		this.masterEndpoint = masterEndpoint;
		this.nowString = nowString;
	}

	private static void countEndpoint(CopyableEndpoint it, TreeMap<String, Set<CopyableEndpoint>> hostnames) {
		Set<CopyableEndpoint> retVal = hostnames.get(it.host);
		if (retVal == null) {
			retVal = new TreeSet<CopyableEndpoint>();
			hostnames.put(it.host, retVal);
		}
		retVal.add(it);
	}

	@Override
	protected void processEventInternal(Endpoint src, Object msg) {
		CopyableEndpoint source = Controller.toCEndpoint(src);
		if (msg instanceof GetInfoReply) {
			System.out.println("Node " + source + " has " + ((GetInfoReply) msg).getKeys().length + " keys");
			countKeys(source, (GetInfoReply) msg);
		} else if (msg instanceof DeathNotification) {
			infomap.remove(source);
			upEndpoints.remove(source);
			if (downEndpoints.add(source))
				System.out.println("found that " + source + " is dead");
		} else if (msg instanceof GetNodeListReply)
			processGetNodeListReply(source, (GetNodeListReply) msg);
	}

	private void processGetNodeListReply(CopyableEndpoint source, GetNodeListReply msg) {
		TreeSet<CopyableEndpoint> newUpEndpoints = new TreeSet<Controller.CopyableEndpoint>();
		for (Endpoint it : Arrays.asList(msg.getKeySet()))
			newUpEndpoints.add(new CopyableEndpoint(it.host, it.port));
		@SuppressWarnings("unchecked")
		TreeSet<CopyableEndpoint> deadNodes = (TreeSet<CopyableEndpoint>) upEndpoints.clone();
		deadNodes.removeAll(newUpEndpoints);
		upEndpoints.clear();
		upEndpoints.addAll(newUpEndpoints);
		downEndpoints.addAll(deadNodes);
		downEndpoints.removeAll(newUpEndpoints);
		for (CopyableEndpoint it : downEndpoints)
			it.setActive(false);
		for (CopyableEndpoint it : upEndpoints)
			it.setActive(true);
		for (CopyableEndpoint it : newUpEndpoints)
			Controller.countEndpoint(it, hostnames);
		System.out.println(upEndpoints.size() + "" + upEndpoints);
	}

	private void countKeys(CopyableEndpoint source, GetInfoReply msg) {
		infomap.put(source, msg);

		boolean done = true;
		for (Endpoint it : upEndpoints)
			if (!infomap.containsKey(it)) {
				done = false;
				break;
			}
		if (done)
			processInfomap();
	}

	private void processInfomap() {
		// average
		double average = 0;
		for (GetInfoReply it : infomap.values())
			average += it.getKeys().length;
		average /= infomap.size();

		TreeMap<Identifier, ArrayList<GetInfoReply>> groupMap = new TreeMap<Identifier, ArrayList<GetInfoReply>>();
		ArrayList<CopyableEndpoint> groupless = new ArrayList<CopyableEndpoint>();
		for (Entry<CopyableEndpoint, GetInfoReply> it : infomap.entrySet())
			if (it.getValue().getGroup() != null) {
				ArrayList<GetInfoReply> tmp = Utils.getOrCreate(groupMap, it.getValue().getGroup().getID());
				tmp.add(it.getValue());
			} else
				groupless.add(it.getKey());

		int perGroupSum = 0;
		for (Entry<Identifier, ArrayList<GetInfoReply>> it : groupMap.entrySet()) {
			int max = 0;
			int maxCount = 0;
			int min = Integer.MAX_VALUE;
			int minCount = 0;
			double avg = 0;
			int groupSize = it.getValue().size();
			for (GetInfoReply it2 : it.getValue()) {
				int keysLen = it2.getKeys().length;
				avg += keysLen;
				if (keysLen > max) {
					max = keysLen;
					maxCount = 1;
				} else if (keysLen == max)
					maxCount++;
				if (keysLen < min) {
					min = keysLen;
					minCount = 1;
				} else if (keysLen == min)
					minCount++;
			}
			avg /= groupSize;
			perGroupSum += max;

			String part2 = maxCount == groupSize ? "" : "min:" + min + "(" + minCount + ") avg:" + avg;
			System.out.println(it.getKey() + " (" + groupSize + ") max:" + max + "(" + maxCount + ") "
					+ part2);
		}

		System.out.println("average keys per node: " + average + " total sum per group: " + perGroupSum);
	}

	class KillCommand extends Command {
		private int toKill;

		@Override
		protected void init(StringTokenizer tokenizer) {
			toKill = readInt(tokenizer, 1);
		}

		@Override
		public void run() {
			for (int it : Utils.range(toKill)) {
				Endpoint currentKill = killRandomNode();
				System.out.println((it + 1) + "/" + toKill + ": killed " + currentKill);
			}
		}
	}

	public Endpoint killRandomNode() {
		if (upEndpoints.size() == 0)
			return null;

		CopyableEndpoint currentKill = Utils.removeRandomEl(upEndpoints);
		sendObject(currentKill, new InstantDeath());
		downEndpoints.add(currentKill);
		currentKill.setActive(false);
		return currentKill;
	}

	class KillWaitCommand extends CommandSeries {
		@Override
		protected void init(StringTokenizer tokenizer) {
		}

		@Override
		public void runCurrent() {
			Endpoint currentKill = killRandomNode();
			System.out.println((currentIter) + "/" + totalIter + ": killed " + currentKill);
		}
	}

	class StartCommand extends Command {
		private int toStart;

		@Override
		protected void init(StringTokenizer tokenizer) {
			toStart = readInt(tokenizer, 1);
		}

		@Override
		public void run() {
			for (int it : Utils.range(toStart)) {
				Endpoint started = startNode();
				System.out.println((it + 1) + "/" + toStart + ": started " + started);
			}
		}
	}

	class StartWaitCommand extends CommandSeries {
		@Override
		protected void init(StringTokenizer tokenizer) {
		}

		@Override
		public void runCurrent() {
			Endpoint started = startNode();
			System.out.println((currentIter) + "/" + totalIter + ": started " + started);
		}
	}

	class LoadCommand extends Command {
		@Override
		protected void init(StringTokenizer tokenizer) {
		}

		@Override
		public void run() {
			sendObject(masterEndpoint, new GetNodeList());
		}
	}

	public CopyableEndpoint startNode() {
		String hostname = "";
		int min = Integer.MAX_VALUE;
		int port = 0;
		for (Entry<String, Set<CopyableEndpoint>> it : hostnames.entrySet()) {
			int counter = 0;
			int maxPort = 0;
			for (CopyableEndpoint it2 : it.getValue()) {
				if (it2.isActive())
					counter++;
				if (it2.port > maxPort)
					maxPort = it2.port;
			}
			if (counter < min) {
				min = counter;
				hostname = it.getKey();
				if (maxPort != 0)
					port = maxPort + 1;
				else
					port = Controller.START_PORT;
			}
		}
		Endpoint newEndpoint = new Endpoint(hostname, port);
		CopyableEndpoint copyableEndpoint = new CopyableEndpoint(hostname, port);
		Controller.countEndpoint(copyableEndpoint, hostnames);
		upEndpoints.add(copyableEndpoint);

		try {
			Process result = Runtime.getRuntime().exec("./runSpecific.sh " + nowString + " "
					+ masterEndpoint.host + " " + masterEndpoint.port + " " + newEndpoint.host + " "
					+ newEndpoint.port, null,
					new File(Controller.SCRIPT_DIR));
			result.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(result.getErrorStream()));
			String line;
			while ((line = br.readLine()) != null)
				System.err.println(line);
			br = new BufferedReader(new InputStreamReader(result.getInputStream()));
			while ((line = br.readLine()) != null)
				System.err.println(line);
		} catch (Exception e) {
			System.err.println("oopsie");
			e.printStackTrace();
		}
		return copyableEndpoint;
	}

	class AverageCommand extends Command {
		@Override
		protected void init(StringTokenizer tokenizer) {
			// no initialization
		}

		@Override
		public void run() {
			infomap.clear();
			for (Endpoint it : upEndpoints)
				sendMessage(it, new GetInfo());
		}
	}

	class InfoCommand extends Command {
		private Endpoint endpoint;

		@Override
		protected void init(StringTokenizer tokenizer) {
			String copyableEndpoint = tokenizer.nextToken();
			endpoint = Controller.createCopyableEndpoint(copyableEndpoint);
		}

		@Override
		public void run() {
			sendMessage(endpoint, new GetInfo());
		}
	}

	abstract class Command implements Runnable {
		public void start(StringTokenizer tokenizer) {
			try {
				init(tokenizer);
				getExecutor().submit(this);
			} catch (Exception e) {
				System.out.println("Something went wrong with " + this.getClass().getSimpleName() + ": " + e);
				e.printStackTrace();
			}
		}

		protected abstract void init(StringTokenizer tokenizer);

		protected int readInt(StringTokenizer tokenizer, int defaultRetVal) {
			if (!tokenizer.hasMoreTokens())
				return defaultRetVal;
			else
				return Integer.valueOf(tokenizer.nextToken());
		}
	}

	abstract class CommandSeries extends Command {
		protected int totalIter;
		protected int currentIter = 0;
		protected final static int DELAY = 3;

		@Override
		public void start(StringTokenizer tokenizer) {
			try {
				totalIter = readInt(tokenizer, 1);
				init(tokenizer);
				getExecutor().submit(this);
			} catch (Exception e) {
				System.out.println("Something went wrong with " + this.getClass().getSimpleName() + ": " + e);
				e.printStackTrace();
			}
		}

		@Override
		public final void run() {
			currentIter++;
			if (currentIter < totalIter)
				getExecutor().schedule(this, CommandSeries.DELAY, TimeUnit.SECONDS);
			runCurrent();
		}

		protected abstract void runCurrent();
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("Wrong arguments. usage: "
					+ "Controller <node list file> <host list file> <NOW string>");
			System.exit(-1);
		}
		System.out.println(new Date() + "  ****** INIT *****  ");
		System.err.println(new Date() + "  ****** INIT *****  ");

		System.out.println("reading file " + args[0]);
		BufferedReader reader = null;
		ArrayList<CopyableEndpoint> endpoints = new ArrayList<CopyableEndpoint>();
		try {
			reader = new BufferedReader(new FileReader(args[0]));
			String line;
			while ((line = reader.readLine()) != null) {
				StringTokenizer tok = new StringTokenizer(line);
				String hostname = tok.nextToken();
				int port = Integer.valueOf(tok.nextToken());
				CopyableEndpoint e = new CopyableEndpoint(hostname, port);
				e.setActive(true);
				endpoints.add(e);
			}
			reader = new BufferedReader(new FileReader(args[1]));
			ArrayList<String> hostnames = new ArrayList<String>();
			while ((line = reader.readLine()) != null)
				hostnames.add(line);
			CopyableEndpoint master = endpoints.remove(0); // remove the master

			Controller c = new Controller(new Endpoint("kalium.gsd.inesc-id.pt", 9091), master, endpoints,
					hostnames, args[2]);
			c.init();

			c.keyboardWait();
		} catch (Exception e) {
			System.out.println("error reading file: " + e);
			e.printStackTrace(System.err);
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
			if (!tokenizer.hasMoreTokens())
				continue;

			String cmd = tokenizer.nextToken().trim().toLowerCase();

			if (cmd.equals("kill"))
				new KillCommand().start(tokenizer);
			else if (cmd.equals("killwait") || cmd.equals("k"))
				new KillWaitCommand().start(tokenizer);
			else if (cmd.equals("info") || cmd.equals("i"))
				new InfoCommand().start(tokenizer);
			else if (cmd.equals("list") || cmd.equals("l"))
				System.out.println(upEndpoints.size() + "" + upEndpoints);
			else if (cmd.equals("average") || cmd.equals("a"))
				new AverageCommand().start(tokenizer);
			else if (cmd.equals("start"))
				new StartCommand().start(tokenizer);
			else if (cmd.equals("startwait") || cmd.equals("s"))
				new StartWaitCommand().start(tokenizer);
			else if (cmd.equals("load"))
				new LoadCommand().start(tokenizer);
			else
				System.out.println("unrecognized command: " + cmd);
		}
	}

	@Override
	protected void nextRound() {
		// ignore next round
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
		public boolean active = true;

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

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
