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
		host = addr.getHostName();
		port = addr.getPort();
	}

	@Override
	public boolean equals(Object t) {
		return (t instanceof Endpoint) && (port == ((Endpoint) t).port) && host.equals(((Endpoint) t).host);
	}

	@Override
	public int hashCode() {
		return host.hashCode() + port;
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}

	public SocketAddress getInetAddress() {
		return new InetSocketAddress(host, port);
	}

	@Override
	public int compareTo(Endpoint o) {
		if (port == o.port)
			return host.compareTo(o.host);
		else
			return port - o.port;
	}

	public int getPort() {
		return port;
	}
}
