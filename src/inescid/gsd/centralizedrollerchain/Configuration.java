package inescid.gsd.centralizedrollerchain;

import java.util.Properties;

public class Configuration {
	private static final Properties properties = new Properties();

	public static final String PAR_MAX_REPLICATION = "maxreplication";
	private static final String PAR_MIN_REPLICATION = "minreplication";
	private static final String PAR_KEEP_ALIVE_INTERVAL = "keepalive";
	private static final String PAR_N_KEYS = "nkeys";
	private static final String PAR_KEYS_RANDOM_SEED = "keysrandomseed";
	private static final String PAR_MIN_KEY_SIZE = "minkeysize";
	private static final String PAR_MAX_KEY_SIZE = "maxkeysize";
	private static final String PAR_ID_SIZE = "idsize";
	private static final String PAR_ROUND_TIME = "roundtime";
	private static final String PAR_ROUNDS_TO_KEEP_MESSAGES = "messagerounds";
	private static final String PAR_ROUNDS_TO_KEEP_KEYS = "keyrounds";
	private static final String PAR_MAX_SHUFFLE_SIZE = "shufflesize";
	private static final String PAR_SHUFFLE_TTL = "shufflettl";

	private static final int MAX_REPLICATION =
			Configuration.readInt(Configuration.PAR_MAX_REPLICATION, "8");
	private static final int MIN_REPLICATION =
			Configuration.readInt(Configuration.PAR_MIN_REPLICATION, "4");
	private static final int KEEP_ALIVE_INTERVAL =
			Configuration.readInt(Configuration.PAR_KEEP_ALIVE_INTERVAL, "30");
	private static final int N_KEYS =
			Configuration.readInt(Configuration.PAR_N_KEYS, "100");
	private static final int KEYS_RANDOM_SEED =
			Configuration.readInt(Configuration.PAR_KEYS_RANDOM_SEED, "100");
	private static final int MIN_KEY_SIZE =
			Configuration.readInt(Configuration.PAR_MIN_KEY_SIZE, "1024");
	private static final int MAX_KEY_SIZE =
			Configuration.readInt(Configuration.PAR_MAX_KEY_SIZE, "2024");
	private static final int ID_SIZE =
			Configuration.readInt(Configuration.PAR_ID_SIZE, "128");
	private static final int ROUND_TIME =
			Configuration.readInt(Configuration.PAR_ROUND_TIME, "10");
	private static final int ROUNDS_TO_KEEP_MESSAGES =
			Configuration.readInt(Configuration.PAR_ROUNDS_TO_KEEP_MESSAGES, "6");
	private static final int ROUNDS_TO_KEEP_KEYS =
			Configuration.readInt(Configuration.PAR_ROUNDS_TO_KEEP_KEYS, "6");
	private static final int MAX_SHUFFLE_SIZE =
			Configuration.readInt(Configuration.PAR_MAX_SHUFFLE_SIZE, "10");
	private static final int SHUFFLE_TTL =
			Configuration.readInt(Configuration.PAR_SHUFFLE_TTL, "15");

	private static final int readInt(String par, String defaultVal) {
		String temp = Configuration.properties.getProperty(par, defaultVal);
		return Integer.valueOf(temp);
	}

	/**
	 * Maximum size for groups. Will divide when size>MAX_REPLICATION.
	 */
	public static int getMaxReplication() {
		return Configuration.MAX_REPLICATION;
	}

	/**
	 * Minimum size for groups. Will merge when size>MAX_REPLICATION.
	 */
	public static int getMinReplication() {
		return Configuration.MIN_REPLICATION;
	}

	/**
	 * Master will gossip with nodes with this frequency
	 */
	public static long getKeepAliveInterval() {
		return Configuration.KEEP_ALIVE_INTERVAL;
	}

	/**
	 * Number of keys the system is initialized with
	 */
	public static int getNKeys() {
		return Configuration.N_KEYS;
	}

	/**
	 * Random seed for creating keys
	 */
	public static int getKeysRandomSeed() {
		return Configuration.KEYS_RANDOM_SEED;
	}

	/**
	 * Keys will always be larger or equal to this size
	 */
	public static int getMinKeySize() {
		return Configuration.MIN_KEY_SIZE;
	}

	/**
	 * Keys will always be smaller than this size
	 */
	public static int getMaxKeySize() {
		return Configuration.MAX_KEY_SIZE;
	}

	/**
	 * Identifier size to be used for keys and overlay group identifiers
	 */
	public static int getIDSize() {
		return Configuration.ID_SIZE;
	}

	/**
	 * Time between rounds (in seconds)
	 */
	public static int getRoundTime() {
		return Configuration.ROUND_TIME;
	}

	/**
	 * Key requests are cached for this number of rounds
	 */
	public static int getRoundsToKeepMessages() {
		return Configuration.ROUNDS_TO_KEEP_MESSAGES;
	}

	/**
	 * Keys are only deleted when this amount of rounds as gone by and they were
	 * not requested in the meanwhile
	 */
	public static int getRoundsToKeepKeys() {
		return Configuration.ROUNDS_TO_KEEP_KEYS;
	}

	/**
	 * No more than this number of pointer will be shuffled between nodes
	 */
	public static int getMaxShuffleSize() {
		return Configuration.MAX_SHUFFLE_SIZE;
	}

	public static int getShuffleTTL() {
		return Configuration.SHUFFLE_TTL;
	}
}
