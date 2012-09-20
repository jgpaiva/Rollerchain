package inescid.gsd.common;

import inescid.gsd.transport.Endpoint;

public interface EventReceiver {
	public void processEvent(Endpoint source, Object message);
}
