package inescid.gsd.centralizedrollerchain.events;

import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class DivideIDUpdate extends UpperLayerMessage {

	private static final long serialVersionUID = 7321857140415186143L;

	private StaticGroup oldGroup;
	private StaticGroup currentGroup;
	private StaticGroup predecessorGroup;

	public DivideIDUpdate(StaticGroup oldGroup, StaticGroup group, StaticGroup predecessorGroup) {
		this.oldGroup = oldGroup;
		currentGroup = group;
		this.predecessorGroup = predecessorGroup;
	}

	public StaticGroup getOldGroup() {
		return oldGroup;
	}

	public void setOldGroup(StaticGroup oldGroup) {
		this.oldGroup = oldGroup;
	}

	public StaticGroup getCurrentGroup() {
		return currentGroup;
	}

	public void setCurrentGroup(StaticGroup currentGroup) {
		this.currentGroup = currentGroup;
	}

	public StaticGroup getPredecessorGroup() {
		return predecessorGroup;
	}

	public void setPredecessorGroup(StaticGroup predecessorGroup) {
		this.predecessorGroup = predecessorGroup;
	}

}
