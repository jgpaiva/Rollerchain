package inescid.gsd.centralizedrollerchain;

import java.math.BigInteger;
import java.util.Random;

public class Identifier extends BigInteger {
	public static final Identifier ZERO = new Identifier(BigInteger.ZERO);
	private static final BigInteger TWO = BigInteger.ONE.add(BigInteger.ONE);
	private static final BigInteger RINGSIZE = new Identifier(BigInteger.ONE
			.add(BigInteger.ONE).pow(Configuration.getIDSize()));
	private static final Identifier MIDDLE_POINT = new Identifier(Identifier.RINGSIZE.divide(BigInteger.ONE
			.add(BigInteger.ONE)));
	private static final long serialVersionUID = 4305665313496090598L;

	public Identifier(String val, int radix) {
		super(val, radix);
		// TODO Auto-generated constructor stub
	}

	public Identifier(String val) {
		super(val);
		// TODO Auto-generated constructor stub
	}

	public Identifier(int numBits, Random rnd) {
		super(numBits, rnd);
		// TODO Auto-generated constructor stub
	}

	public Identifier(int bitLength, int certainty, Random rnd) {
		super(bitLength, certainty, rnd);
		// TODO Auto-generated constructor stub
	}

	public Identifier(int signum, byte[] magnitude) {
		super(signum, magnitude);
		// TODO Auto-generated constructor stub
	}

	public Identifier(byte[] val) {
		super(val);
		// TODO Auto-generated constructor stub
	}

	public Identifier(BigInteger value) {
		super(value.toByteArray());
	}

	@Override
	public String toString() {
		return super.toString(Character.MAX_RADIX);
	}

	public static Identifier calculateMiddlePoint(Identifier id, Identifier predecessor) {
		if (predecessor == null)
			return Identifier.MIDDLE_POINT;
		BigInteger ret = id.add(predecessor).divide(Identifier.TWO).mod(Identifier.RINGSIZE);
		return new Identifier(ret);
	}

	public static boolean isBetween(Identifier id, Identifier intervalStart,
			Identifier intervalEnd) {
		if (intervalStart.compareTo(intervalEnd) <= 0)
			return (id.compareTo(intervalStart) > 0)
					&& (id.compareTo(intervalEnd) <= 0);
		// else
		return (id.compareTo(intervalStart) > 0) || (id.compareTo(intervalEnd) < 0);
	}
}
