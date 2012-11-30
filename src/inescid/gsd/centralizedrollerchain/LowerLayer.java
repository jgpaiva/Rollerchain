package inescid.gsd.centralizedrollerchain;

import inescid.gsd.centralizedrollerchain.interfaces.Event;
import inescid.gsd.transport.Endpoint;

import java.util.concurrent.ScheduledExecutorService;

public interface LowerLayer {
	public abstract StaticGroup getGroup();

	public abstract Identifier getPredecessorID();

	public abstract Identifier getSuccessorID();

	public abstract StaticGroup getPredecessor();

	public abstract StaticGroup getSuccessor();

	public abstract ScheduledExecutorService getExecutor();

	public abstract Endpoint getEndpoint();

	public abstract void sendMessage(Endpoint dest, Event msg);

}