package inescid.gsd.utils;

import java.util.Arrays;
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
		for (int i = 0; i < randomNumber; i++)
			it.next();
		T toReturn = it.next();
		return toReturn;
	}

	public static final <T> T getRandomEl(Collection<T> col, T toIgnore) {
		if ((col.size() == 1) && col.contains(toIgnore))
			throw new IndexOutOfBoundsException("this should never happen!");

		boolean removed = false;
		if (col.contains(toIgnore)) {
			col.remove(toIgnore);
			removed = true;
		}

		int randomNumber = Utils.R.nextInt(col.size());
		Iterator<T> it = col.iterator();
		for (int i = 0; i < randomNumber; i++)
			it.next();
		T toReturn = it.next();

		if (removed)
			col.add(toReturn);

		return toReturn;
	}

	public static final <T> T removeRandomEl(Collection<T> col) {
		int randomNumber = Utils.R.nextInt(col.size());
		Iterator<T> it = col.iterator();
		for (int i = 0; i < randomNumber; i++)
			it.next();
		T toReturn = it.next();
		it.remove();
		return toReturn;
	}

	public static Iterable<Integer> range(final int upTo) {
		return new MyIterable(upTo);
	}

	public static Iterable<Integer> range(Collection<?> upTo) {
		return new MyIterable(upTo.size());
	}

	public static String stackTraceFormat() {
		List<StackTraceElement> tempList = Arrays.asList(Thread.currentThread().getStackTrace());
		String toReturn = "";
		for (StackTraceElement it : tempList)
			toReturn += it + "\n";
		return toReturn;
	}
}

class MyIterable implements Iterable<Integer> {
	final int upTo;

	MyIterable(int upTo) {
		this.upTo = upTo;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new MyIterableIterator();
	}

	class MyIterableIterator implements Iterator<Integer> {
		int counter = 0;

		@Override
		public boolean hasNext() {
			return counter < upTo;
		}

		@Override
		public Integer next() {
			return counter++;
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
		}
	}
}
