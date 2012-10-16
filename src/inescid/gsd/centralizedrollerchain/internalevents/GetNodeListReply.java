package inescid.gsd.centralizedrollerchain.internalevents;

import inescid.gsd.centralizedrollerchain.interfaces.InternalEvent;
import inescid.gsd.transport.Endpoint;

import java.io.Serializable;
import java.util.Set;

public class GetNodeListReply implements InternalEvent, Serializable {
	private static final long serialVersionUID = 7805797562478440784L;
	private final Endpoint[] keySet;

	public GetNodeListReply(Set<Endpoint> keySet) {
		this.keySet = keySet.toArray(new Endpoint[0]);
	}

	public Endpoint[] getKeySet() {
		return keySet;
	}
}
