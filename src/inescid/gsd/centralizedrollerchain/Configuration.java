package inescid.gsd.centralizedrollerchain;

public class Configuration {
	// TODO: do me

	private static final int MAX_REPLICATION = 8;
	private static final long KEEP_ALIVE_INTERVAL = 30;
	private static final int MIN_REPLICATION = 4;
	private static final int N_KEYS = 10000;
	private static final int RANDOM_SEED = 123456;
	private static final int MIN_KEY_SIZE = 1024;
	private static final int MAX_KEY_SIZE = 2024;
	private static final int ID_SIZE = 128;
	private static final long ROUND_TIME = 10;
	private static final int ROUNDS_TO_KEEP_MESSAGES = 10;
	private static final int ROUNDS_TO_KEEP_KEYS = 10;

	public static int getMaxReplication() {
		return Configuration.MAX_REPLICATION;
	}

	public static long getKeepAliveInterval() {
		return Configuration.KEEP_ALIVE_INTERVAL;
	}

	public static int getMinReplication() {
		return Configuration.MIN_REPLICATION;
	}

	public static int getNKeys() {
		return Configuration.N_KEYS;
	}

	public static int getRandomSeed() {
		return Configuration.RANDOM_SEED;
	}

	public static int getMinKeySize() {
		return Configuration.MIN_KEY_SIZE;
	}

	public static int getMaxKeySize() {
		return Configuration.MAX_KEY_SIZE;
	}

	public static int getIDSize() {
		return Configuration.ID_SIZE;
	}

	public static long getRoundTime() {
		return Configuration.ROUND_TIME;
	}

	public static int getRoundsToKeepMessages() {
		return Configuration.ROUNDS_TO_KEEP_MESSAGES;
	}

	public static int getRoundsToKeepKeys() {
		return Configuration.ROUNDS_TO_KEEP_KEYS;
	}
}
