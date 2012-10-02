package inescid.gsd.centralizedrollerchain.utils;

import inescid.gsd.transport.Endpoint;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class FileOutput {
	private final String name;
	private PrintStream out;

	public FileOutput(Endpoint e, Class<?> c) {
		name = c.getSimpleName() + e.getPort();
		try {
			out = new PrintStream(name);
		} catch (FileNotFoundException e1) {
			throw new RuntimeException(e1);
		}
	}

	public void status(String s) {
		out.println(System.currentTimeMillis() + " STATUS: " + s);
	}
}
