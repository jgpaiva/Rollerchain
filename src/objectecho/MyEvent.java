package objectecho;

import java.io.Serializable;

public class MyEvent implements Serializable {

	private static final long serialVersionUID = -8107125089785567535L;

	private int id;

	public MyEvent(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
