package dk.accel.misw.mp.ec2.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class ExecutorsUtils {

	public static <T> List<T> unwrapFutures(List<Future<T>> futures) throws IOException, InterruptedException {
		List<T> result = new ArrayList<T>(futures.size());
		for (Future<T> future : futures) {
			try {
				T t = future.get();
				if (t != null) {
					result.add(t);
				}
			} catch (ExecutionException e) {
				throw unwrapException(e, IOException.class);
			}
		}
		return result;
	}

	public static <K, V> Map<K, V> unwrapFutures(Map<K, Future<V>> futures) throws IOException, InterruptedException {
		Map<K, V> result = new LinkedHashMap<K, V>(futures.size());
		for (Map.Entry<K, Future<V>> entry : futures.entrySet()) {
			try {
				K k = entry.getKey();
				V v = entry.getValue().get();
				result.put(k, v);
			} catch (ExecutionException e) {
				throw unwrapException(e, IOException.class);
			}
		}
		return result;
	}

	private static <T extends Exception> T unwrapException(ExecutionException e, Class<T> cls) {
		Throwable t = e.getCause();
		if (cls.isInstance(t)) {
			return cls.cast(t);
		}

		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		if (t instanceof Error) {
			throw (Error) t;
		}
		throw new RuntimeException(t);
	}

	public static ThreadFactory newDeamonThreadFactory() {
		return new ThreadFactory() {
			private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

			@Override
			public Thread newThread(Runnable r) {
				Thread t = defaultThreadFactory.newThread(r);
				t.setDaemon(true);
				return t;
			}
		};
	}

	private ExecutorsUtils() {
		throw new AssertionError();
	}
}
