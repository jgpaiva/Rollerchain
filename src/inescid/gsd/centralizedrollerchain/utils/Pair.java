package inescid.gsd.centralizedrollerchain.utils;

public class Pair<X, Y> {
	private X fst;
	private Y snd;

	public Pair(X fst, Y snd) {
		this.fst = fst;
		this.snd = snd;
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
}
