package inescid.gsd.rollerchain.interfaces;

import java.io.Serializable;

public class Event implements Serializable {
	private static final long serialVersionUID = 7640809868859035097L;

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
