package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

import java.util.Arrays;

public class NodeGroupPairListReply extends UpperLayerMessage {
	private static final long serialVersionUID = 1L;
	private final NodeGroupPair[] sample;

	public NodeGroupPairListReply(NodeGroupPair[] array) {
		sample = array;
	}

	public NodeGroupPair[] getSample() {
		return sample;
	}

	@Override
	public String toString() {
		return super.toString() + " contains: " + Arrays.asList(sample);
	}
}
