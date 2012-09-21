package inescid.gsd.centralizedrollerchain.utils;

import java.util.concurrent.atomic.AtomicLong;

public class PriorityPair<X, Y> implements Comparable<PriorityPair<X, Y>> {
	private X fst;
	private Y snd;
	private final int priority;
	private final long id;

	private static AtomicLong seq = new AtomicLong();

	public PriorityPair(X fst, Y snd, int priority) {
		this.fst = fst;
		this.snd = snd;
		this.priority = priority;
		id = PriorityPair.seq.incrementAndGet();
	}

	@Override
	public String toString() {
		return "(" + this.fst + "," + this.snd + ")";
	}

	public X getFst() {
		return this.fst;
	}

	public void setFst(X fst) {
		this.fst = fst;
	}

	public Y getSnd() {
		return this.snd;
	}

	public void setSnd(Y snd) {
		this.snd = snd;
	}

	public int getPriority() {
		return priority;
	}

	@Override
	public int compareTo(PriorityPair<X, Y> o) {
		if (o.priority == this.priority) {
			long val = -(o.id - this.id);
			if (val > 0L)
				return 1;
			else if (val == 0L) return 0;
			return -1;
		} else
			return o.priority - this.priority;
	}
}
