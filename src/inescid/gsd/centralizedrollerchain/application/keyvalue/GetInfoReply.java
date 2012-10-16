package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.StaticGroup;
import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class GetInfoReply extends UpperLayerMessage {
	private static final long serialVersionUID = 3871969048205446146L;
	private final Key[] keys;
	private final StaticGroup predecessor;
	private final StaticGroup successor;
	private final StaticGroup group;

	public GetInfoReply(Key[] keys, StaticGroup group, StaticGroup predecessor, StaticGroup successor) {
		this.keys = keys;
		this.group = group;
		this.successor = successor;
		this.predecessor = predecessor;
	}

	public StaticGroup getPredecessor() {
		return predecessor;
	}

	public StaticGroup getSuccessor() {
		return successor;
	}

	public StaticGroup getGroup() {
		return group;
	}

	public Key[] getKeys() {
		return keys;
	}

	@Override
	public String toString() {
		return super.toString() + " for " + keys.length + " keys";
	}
}
