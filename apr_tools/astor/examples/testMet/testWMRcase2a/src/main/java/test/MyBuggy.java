package test;

public class MyBuggy {

	public Integer operation(int i1, int i2, String type) {

		if ("*".equals(type)) {
			return i1 * i2;

		}

		if ("+".equals(type)) {
			return i1 + i2;

		}

		if ("-".equals(type)) {
			return i1 - i2;

		}

		if ("gr".equals(type)) {

			if (i2 > i1)
				return i2;

			if (i1 >= i2)
				return i1;

		}
		if ("absmult".equals(type)) {
			// Returns the multiplication of two positive values
			return toPositive(i1) * toNegative(i2);// Buggy method invocation
		}

		return null;
	}

	public int toPositive(int n) {
		if (n >= 0)
			return n;
		else
			return n * -1;

	}

	public int toNegative(int n) {
		if (n <= 0)
			return n;
		else
			return n * -1;

	}

}
