package objectecho;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

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

	public SocketAddress getInetAddress() {
		return new InetSocketAddress(this.host, this.port);
	}
}
