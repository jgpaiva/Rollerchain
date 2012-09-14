package inescid.gsd.rollerchain.events;

import inescid.gsd.rollerchain.interfaces.Event;
import inescid.gsd.rollerchain.interfaces.EventReceiver;

public class WorkerInit extends Event {

	private EventReceiver worker;

	WorkerInit(EventReceiver worker) {
		this.worker = worker;
	}

	public EventReceiver getWorker() {
		return this.worker;
	}
}
