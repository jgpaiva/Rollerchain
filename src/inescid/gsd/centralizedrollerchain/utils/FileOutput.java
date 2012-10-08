package inescid.gsd.centralizedrollerchain.utils;

import inescid.gsd.transport.Endpoint;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class FileOutput {
	private final String name;
	private PrintStream out;

	public FileOutput(Endpoint e, Class<?> c) {
		String filename = c.getSimpleName() + e.getPort();
		filename = filename.toLowerCase() + ".out";
		name = FileOutput.pad(c.getSimpleName(), 15);
		try {
			out = new PrintStream(filename);
		} catch (FileNotFoundException e1) {
			throw new RuntimeException(e1);
		}
	}

	private static String pad(String string, int i) {
		if (string.length() == i)
			return string;
		if (string.length() > i)
			return string.substring(string.length() - i, string.length());
		return String.format("%" + i + "s", string);
	}

	public void status(String s) {
		out.println(Long.toString(System.currentTimeMillis(), Character.MAX_RADIX) + " " + name + ":"
				+ s);
	}

	public void update(String s) {
		out.println(Long.toString(System.currentTimeMillis(), Character.MAX_RADIX) + " " + name + " UPDT:"
				+ s);
	}

	static public void main(String[] args) {
		System.out.println();
	}
}
