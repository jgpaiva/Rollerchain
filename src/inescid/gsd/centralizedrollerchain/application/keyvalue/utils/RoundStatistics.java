package inescid.gsd.centralizedrollerchain.application.keyvalue.utils;

public class RoundStatistics {
	private int receivedKeysCounter = 0;
	private int divisionCount = 0;
	private int mergeCount = 0;
	private int keysRequestCount = 0;
	private int groupUpdateCount = 0;
	private int keysReplyCount = 0;
	private int allKeysRequest = 0;
	private int receivedGossipCount = 0;

	public void reset() {
		receivedKeysCounter = 0;
		divisionCount = 0;
		mergeCount = 0;
		keysRequestCount = 0;
		groupUpdateCount = 0;
		keysReplyCount = 0;
		allKeysRequest = 0;
		receivedGossipCount = 0;
	}

	@Override
	public String toString() {
		return "receivedKeys:" + receivedKeysCounter + " division:" + divisionCount + " merge:" + mergeCount
				+ " keysRequest:" + keysRequestCount + " keysReply:" + keysReplyCount + " allKeysRequest:"
				+ allKeysRequest + " groupUpdate:" + groupUpdateCount + " receivedGossip:"
				+ receivedGossipCount;
	}

	public void receivedKeys(int prevSize, int size) {
		receivedKeysCounter += (size - prevSize);
	}

	public int getReceivedKeysCounter() {
		return receivedKeysCounter;
	}

	public void incrDivisionCount() {
		divisionCount++;
	}

	public int getDivisionCount() {
		return divisionCount;
	}

	public void incrMergeCount() {
		mergeCount++;
	}

	public int getMergeCount() {
		return mergeCount;
	}

	public void incrKeysRequestCount() {
		keysRequestCount++;
	}

	public int getKeysRequestCount() {
		return keysRequestCount;
	}

	public void incrGroupUpdateCount() {
		groupUpdateCount++;
	}

	public int getGroupUpdateCount() {
		return groupUpdateCount;
	}

	public void incrKeysReplyCount() {
		keysReplyCount++;
	}

	public int getKeysReplyCount() {
		return keysReplyCount;
	}

	public void incrAllKeysRequestCount() {
		allKeysRequest++;
	}

	public int getAllKeysRequest() {
		return allKeysRequest;
	}

	public void incrReceivedGossipCount() {
		receivedGossipCount++;
	}

	public int getReceivedGossipCount() {
		return receivedGossipCount;
	}
}
