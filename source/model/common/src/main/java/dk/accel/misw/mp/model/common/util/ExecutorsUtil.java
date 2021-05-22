package dk.accel.misw.mp.model.common.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecutorsUtil {

	private static final Logger LOG = Logger.getLogger(ExecutorsUtil.class.getName());
	
	static {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread thread, Throwable t) {
				ExecutorsUtil.LOG.log(Level.WARNING, "Dead thread " + thread, t);
			}
		});
	}
	
	private static class DaemonThreadFactory implements ThreadFactory {
		private final ThreadFactory factory;
		
		private DaemonThreadFactory(ThreadFactory factory) {
			this.factory = StdUtil.checkForNull(factory);
		}
		
		@Override
		public Thread newThread(Runnable r) {
			Thread result = factory.newThread(r);
			result.setDaemon(true);
			return result;
		}
	}
	
	private static class NamedThreadFactory implements ThreadFactory {
		private final String prefix;
		private final ThreadFactory factory;
		private final AtomicInteger threadNumber = new AtomicInteger();
		
		private NamedThreadFactory(String prefix, ThreadFactory factory) {
			this.prefix = StdUtil.checkForNull(prefix);
			this.factory = StdUtil.checkForNull(factory);
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread result = factory.newThread(r);
			result.setName(prefix + threadNumber.getAndIncrement());
			return result;
		}
	}
	
	private static class UncaughtExceptionRunnable implements Runnable {
		private final Runnable runnable;
		
		private UncaughtExceptionRunnable(Runnable runnable) {
			this.runnable = StdUtil.checkForNull(runnable);
		}

		@Override
		public void run() {
			try {
				LOG.finest("run " + Thread.currentThread());
				runnable.run();
				LOG.finest("runned " + Thread.currentThread());
			} catch (Throwable t) {
				ExecutorsUtil.LOG.log(Level.WARNING, "Escaped throwable on thread " + Thread.currentThread(), t);
			}
		}
	}
	
	public static ThreadFactory newNamedThreadFactory(String prefix, ThreadFactory factory) {
		return new NamedThreadFactory(prefix, factory);
	}

	public static ThreadFactory newDaemonThreadFactory(ThreadFactory factory) {
		return new DaemonThreadFactory(factory);
	}

	public static ThreadFactory newDaemonThreadFactory(String prefix, ThreadFactory factory) {
		return new NamedThreadFactory(prefix, new DaemonThreadFactory(factory));
	}
	
	public static Runnable wrap(Runnable runnable) {
		return new UncaughtExceptionRunnable(runnable);
	}
	
	/**
	 * No instances.
	 */
	private ExecutorsUtil() {
		throw new AssertionError();
	}
}
