package inescid.gsd.rollerchain.comm;

import inescid.gsd.rollerchain.interfaces.Event;
import inescid.gsd.rollerchain.interfaces.EventReceiver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class NodeProxy implements EventReceiver {
	String address;

	NodeProxy(String address) {
		this.address = address;
	}

	@Override
	public void processEvent(Event e) {
		// this event is to be sent to the node which this proxy stands for

		ByteArrayOutputStream fos = null;
		ObjectOutputStream out = null;

		fos = new ByteArrayOutputStream();
		try {
			out = new ObjectOutputStream(fos);
			out.writeObject(e);
			out.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
	}
}
