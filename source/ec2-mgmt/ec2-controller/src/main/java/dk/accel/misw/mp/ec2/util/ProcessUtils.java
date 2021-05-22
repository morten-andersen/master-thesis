package dk.accel.misw.mp.ec2.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class ProcessUtils {

	private static final Logger LOG = Logger.getLogger(ProcessUtils.class.getName());
	
	public static ExecutorService processStreams(Process process, OutputStream out, OutputStream err) {
		ExecutorService executor = Executors.newFixedThreadPool(2, ExecutorsUtils.newDeamonThreadFactory());

		executor.execute(new ProcessStreamConsumer(process.getInputStream(), out));
		executor.execute(new ProcessStreamConsumer(process.getErrorStream(), err));
		
		return executor;
	}

	private static class ProcessStreamConsumer implements Runnable {

		private final InputStream src;
		private final OutputStream dest;

		private ProcessStreamConsumer(InputStream src, OutputStream dest) {
			this.src = src;
			this.dest = dest;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[8192];
			int len;
			try {
				while ((len = src.read(buffer)) != -1) {
					dest.write(buffer, 0, len);
				}
			} catch (IOException e) {
				LOG.warning("Exception reading data from process " + e.getMessage());
			}

		}
	}
	
	private ProcessUtils() {
		throw new AssertionError();
	}
}
