package inescid.gsd.centralizedrollerchain.test;

import inescid.gsd.centralizedrollerchain.events.InstantDeath;
import inescid.gsd.transport.ConnectionManager;
import inescid.gsd.transport.Endpoint;
import inescid.gsd.transport.events.DeathNotification;
import inescid.gsd.transport.interfaces.EventReceiver;

import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KillNode  implements EventReceiver{
	ConnectionManager manager;

	KillNode(Endpoint endpoint) {
		manager = new ConnectionManager(this, endpoint);
	}

	public static void main(String[] args) {
		Logger.getLogger("").setLevel(Level.ALL);
		KillNode.setHandlerLevel();

		Endpoint myEndpoint = new Endpoint("kalium.gsd.inesc-id.pt", 9090);
		KillNode obj = new KillNode(myEndpoint);
		try {
			Thread.sleep(2 * 1000); // give some time for initialization
		} catch (InterruptedException e) {
		}

		ArrayList<Endpoint> toKill = new ArrayList<Endpoint>();

		for (int it = 0; it < ((9119 - 9090) + 1); it++)
			toKill.add(new Endpoint("melos", 9090 + it));

		for (Endpoint it : toKill)
			obj.kill(it);
	}

	private void kill(Endpoint toKillEndpoint) {
		manager.getConnection(toKillEndpoint).sendMessage(new InstantDeath());
	}

	@Override
	public void processEvent(Endpoint source, Object message) {
		System.out.println(message);
		if (message instanceof DeathNotification) {
			((DeathNotification) message).getDetails().printStackTrace(System.err);
			System.out.println(((DeathNotification)message).getDetails());
		}
	}

	private static void setHandlerLevel() {
		// get the top Logger:
		Logger topLogger = java.util.logging.Logger.getLogger("");

		// Handler for console (reuse it if it already exists)
		Handler consoleHandler = null;
		// see if there is already a console handler
		for (Handler handler : topLogger.getHandlers())
			if (handler instanceof ConsoleHandler) {
				// found the console handler
				consoleHandler = handler;
				break;
			}

		if (consoleHandler == null) {
			// there was no console handler found, create a new one
			consoleHandler = new ConsoleHandler();
			topLogger.addHandler(consoleHandler);
		}
		// set the console handler to fine:
		consoleHandler.setLevel(Level.ALL);
	}
}
