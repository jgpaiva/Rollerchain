package objectecho;

public class MyObject extends MyEvent {
	private int i;
	private double j;
	private String s;

	MyObject(int i, double j, String s, int id) {
		super(id);
		this.i = i;
		this.j = j;
		this.s = s;
	}

	public int getI() {
		return this.i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public double getJ() {
		return this.j;
	}

	public void setJ(double j) {
		this.j = j;
	}

	public String getS() {
		return this.s;
	}

	public void setS(String s) {
		this.s = s;
	}
}
