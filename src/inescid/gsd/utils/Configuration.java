package inescid.gsd.utils;

public class Configuration {
	/**
	 * Get the time between garbage collection of connections. In seconds.
	 */
	final static int CONNECTION_TIMEOUT = 60;

	public static int getConnectionTimeout() {
		return Configuration.CONNECTION_TIMEOUT;
	}
}
