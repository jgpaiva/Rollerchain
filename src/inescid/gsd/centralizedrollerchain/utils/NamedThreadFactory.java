package inescid.gsd.centralizedrollerchain.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
	private final String threadPre;
	private final ThreadFactory realFactory = Executors.defaultThreadFactory();

	public NamedThreadFactory(String string) {
		threadPre = string;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread temp = realFactory.newThread(r);
		temp.setName(threadPre + temp.getName());
		return temp;
	}
}
