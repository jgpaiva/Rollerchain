package inescid.gsd.rollerchain.events;

import inescid.gsd.rollerchain.interfaces.Event;
import inescid.gsd.rollerchain.interfaces.EventReceiver;

public class WorkerNodeInit implements Event {
	private EventReceiver controller;

	public WorkerNodeInit(EventReceiver controller) {
		this.controller = controller;
	}

	public EventReceiver getController() {
		return this.controller;
	}
}
