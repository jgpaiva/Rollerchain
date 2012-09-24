package inescid.gsd.transport.interfaces;

import inescid.gsd.transport.Endpoint;

public interface EventReceiver {
	public void processEvent(Endpoint source, Object message);
}
