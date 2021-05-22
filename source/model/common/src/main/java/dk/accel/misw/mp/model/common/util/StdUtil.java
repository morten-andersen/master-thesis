package dk.accel.misw.mp.model.common.util;

public class StdUtil {

	public static <T> T checkForNull(T t) {
		if (t == null) {
			throw new NullPointerException();
		}
		return t;
	}
	
	/**
	 * No instances
	 */
	private StdUtil() {
		throw new AssertionError();
	}
}
