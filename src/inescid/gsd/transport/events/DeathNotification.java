package inescid.gsd.transport.events;


public class DeathNotification implements TransportEvent {
	private final Throwable details;

	public DeathNotification(Throwable e) {
		details = e;
	}

	public Throwable getDetails() {
		return details;
	}
}
