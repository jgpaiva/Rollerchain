package inescid.gsd.centralizedrollerchain.interfaces;

import inescid.gsd.centralizedrollerchain.Node;
import inescid.gsd.transport.interfaces.EventReceiver;

public interface UpperLayer extends EventReceiver {
	public void init(Node n);
}
