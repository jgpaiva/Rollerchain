package inescid.gsd.rollerchain;

import inescid.gsd.rollerchain.events.SetNeighbours;
import inescid.gsd.rollerchain.events.WorkerNodeInit;
import inescid.gsd.rollerchain.interfaces.Event;
import inescid.gsd.rollerchain.interfaces.EventReceiver;

import java.util.Set;

public class WorkerNode implements EventReceiver {
	private WorkerNodeInternalState s;

	@Override
	public void processEvent(Event e) {
		if (e instanceof WorkerNodeInit) {
			this.processWorkerNodeInit((WorkerNodeInit) e);
		} else if (e instanceof SetNeighbours) {
			this.processSetNeighbours((SetNeighbours) e);
		}
	}

	private void processWorkerNodeInit(WorkerNodeInit e) {
		this.s.setController(e.getController());
	}

	private void processSetNeighbours(SetNeighbours e) {
		this.s.setGroup(e.getGroup());
		this.s.setSuccessorGroup(e.getSuccessorGroup());
		this.s.setPredecessorGroup(e.getPredecessorGroup());
	}
}

class WorkerNodeInternalState {
	private EventReceiver controller;
	private Set<EventReceiver> group;
	private Set<EventReceiver> successorGroup;
	private Set<EventReceiver> predecessorGroup;

	public void setController(EventReceiver controller) {
		this.controller = controller;
	}

	public EventReceiver getController() {
		return this.controller;
	}

	public void setGroup(Set<EventReceiver> group) {
		this.group = group;
	}

	public Set<EventReceiver> getGroup() {
		return this.group;
	}

	public Set<EventReceiver> getSuccessorGroup() {
		return this.successorGroup;
	}

	public void setSuccessorGroup(Set<EventReceiver> successorGroup) {
		this.successorGroup = successorGroup;
	}

	public Set<EventReceiver> getPredecessorGroup() {
		return this.predecessorGroup;
	}

	public void setPredecessorGroup(Set<EventReceiver> predecessorGroup) {
		this.predecessorGroup = predecessorGroup;
	}
}
