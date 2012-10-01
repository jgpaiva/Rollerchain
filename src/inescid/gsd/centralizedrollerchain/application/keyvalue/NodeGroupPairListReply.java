package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

public class NodeGroupPairListReply extends UpperLayerMessage {
	private static final long serialVersionUID = 1L;
	private final NodeGroupPair[] sample;

	public NodeGroupPairListReply(NodeGroupPair[] array) {
		sample = array;
	}

	public NodeGroupPair[] getSample() {
		return sample;
	}
}
