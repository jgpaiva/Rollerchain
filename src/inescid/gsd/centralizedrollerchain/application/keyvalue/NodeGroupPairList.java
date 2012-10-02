package inescid.gsd.centralizedrollerchain.application.keyvalue;

import inescid.gsd.centralizedrollerchain.interfaces.UpperLayerMessage;

import java.util.Arrays;

public class NodeGroupPairList extends UpperLayerMessage {
	private static final long serialVersionUID = 3021215139084443301L;
	private final NodeGroupPair[] sample;

	public NodeGroupPairList(NodeGroupPair[] sample) {
		this.sample = sample;
	}

	public NodeGroupPair[] getSample() {
		return sample;
	}

	@Override
	public String toString() {
		return super.toString() + " contains: " + Arrays.asList(sample);
	}
}
