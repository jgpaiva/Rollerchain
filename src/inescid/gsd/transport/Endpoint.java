package inescid.gsd.transport;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class Endpoint implements Comparable<Endpoint>, Serializable {
	private static final long serialVersionUID = -6055286938930466164L;

	public final String host;
	public final int port;

	public Endpoint(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public Endpoint(InetSocketAddress addr) {
		this.host = addr.getHostName();
		this.port = addr.getPort();
	}

	public boolean equals(Endpoint t) {
		return this.port == t.port && this.host.equals(t.host);
	}

	@Override
	public String toString() {
		return this.host + ":" + this.port;
	}

	public SocketAddress getInetAddress() {
		return new InetSocketAddress(this.host, this.port);
	}

	@Override
	public int compareTo(Endpoint o) {
		if (this.port == o.port)
			return this.host.compareTo(o.host);
		else
			return this.port - o.port;
	}
}
