package inescid.gsd.centralizedrollerchain;

public class Configuration {
	// TODO: do me

	private static final int MAX_REPLICATION = 8;
	private static final long KEEP_ALIVE_INTERVAL = 30;
	private static final int MIN_REPLICATION = 4;

	public static int getMaxReplication() {
		return Configuration.MAX_REPLICATION;
	}

	public static long getKeepAliveInterval() {
		return Configuration.KEEP_ALIVE_INTERVAL;
	}

	public static int getMinReplication() {
		return Configuration.MIN_REPLICATION;
	}
}
