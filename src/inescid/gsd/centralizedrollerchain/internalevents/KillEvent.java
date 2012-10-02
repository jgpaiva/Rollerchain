package inescid.gsd.centralizedrollerchain.internalevents;

import inescid.gsd.centralizedrollerchain.interfaces.InternalEvent;

import java.io.Serializable;

public class KillEvent implements Serializable, InternalEvent {
	private static final long serialVersionUID = -101341565939941821L;

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
