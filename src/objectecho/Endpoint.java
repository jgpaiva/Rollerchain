package objectecho;

public class Endpoint {
	public final String host;
	public final int port;

	Endpoint(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public String toString() {
		return this.host + ":" + this.port;
	}
}
