package inescid.gsd.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Utils {
	private final static int RANDOM_SEED = 123456; // TODO: maybe make this
	// configurable?

	private final static Random R = new Random(Utils.RANDOM_SEED);

	public static final <T> T removeRandomEl(List<T> list) {
		int randomNumber = Utils.R.nextInt(list.size());
		return list.remove(randomNumber);
	}

	public static final <T> T getRandomEl(List<T> list) {
		int randomNumber = Utils.R.nextInt(list.size());
		return list.get(randomNumber);
	}

	public static final <T> T getRandomEl(Collection<T> col) {
		int randomNumber = Utils.R.nextInt(col.size());
		Iterator<T> it = col.iterator();
		for (int i = 0; i < randomNumber; i++) {
			it.next();
		}
		T toReturn = it.next();
		return toReturn;
	}

	public static final <T> T removeRandomEl(Collection<T> col) {
		int randomNumber = Utils.R.nextInt(col.size());
		Iterator<T> it = col.iterator();
		for (int i = 0; i < randomNumber; i++) {
			it.next();
		}
		T toReturn = it.next();
		it.remove();
		return toReturn;
	}
}
