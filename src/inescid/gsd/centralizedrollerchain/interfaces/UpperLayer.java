package inescid.gsd.centralizedrollerchain.interfaces;

import inescid.gsd.centralizedrollerchain.LowerLayer;
import inescid.gsd.transport.interfaces.EventReceiver;

public interface UpperLayer extends EventReceiver {
	public void nextRound();

	void init(LowerLayer owner);
}
