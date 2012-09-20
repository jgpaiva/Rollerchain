package inescid.gsd.transport.events;

import inescid.gsd.transport.Endpoint;

import java.io.Serializable;

public class EndpointInfo implements Serializable, TransportEvent {

	private static final long serialVersionUID = 7634304803550106405L;

	public final Endpoint endpoint;

	public EndpointInfo(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

}
