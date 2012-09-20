package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.transport.Endpoint;

public class WorkerInit extends Event {

	private static final long serialVersionUID = -1291960682006701684L;

	private Endpoint worker;

	WorkerInit(Endpoint worker) {
		this.worker = worker;
	}

	public Endpoint getWorker() {
		return this.worker;
	}
}
