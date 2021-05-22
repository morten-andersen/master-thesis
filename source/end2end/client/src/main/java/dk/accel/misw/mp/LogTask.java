package dk.accel.misw.mp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Simple task that logs min/mean/max values to a file.
 * This is actively called just before updating values retrieved
 * from the server OR if this doesn't happen every second
 * by being scheduled.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class LogTask implements Runnable {

	private static final Logger LOG = Logger.getLogger(LogTask.class.getName());
	
	private final List<long[]> values = new ArrayList<long[]>();
	private final File logFile;
	private final Main.WsClientTask clientTask;
	
	private AtomicLong lastDumpTime = new AtomicLong();
	
	LogTask(Main.WsClientTask clientTask) {
		this.logFile = newLogFile();
		this.clientTask = clientTask;
	}
	
	@Override
	public void run() {
		dump(false);
	}
	
	synchronized void dump(boolean force) {
		while (true) {
			long last = lastDumpTime.get();
			long now = System.currentTimeMillis();
			if (force || (last + Main.INTERVAL < now)) {
				// a forced dump, or no active dumps for the last INTERVAL ms -> dump
				if (lastDumpTime.compareAndSet(last, now)) {
					break;
				}
			} else {
				return;
			}
		}
		
		values.add(clientTask.dumpSensorValueAges());
		if (values.size() >= 32) {
			LOG.info("Dumping to " + logFile.getName());
			// dump to file
			try {
				Writer writer = new BufferedWriter(new FileWriter(logFile, true));
				try {
					// only emit in steps of 1000L / INTERVAL -> only one sampling per second
					for (int i = 0, n = values.size(); i < n; i += (1000L / Main.INTERVAL)) {
						long[] val = values.get(i);
						writer.write(Long.toString(val[0]) + ";" + val[1] + ";" + val[2] + "\n");
					}
				} finally {
					writer.close();
				}
			} catch (IOException e) {
				throw new RuntimeException("Unable to write " + logFile, e);
			}
			values.clear();
		}
	}
	
	private File newLogFile() {
		String fileName = newFileName();
		try {
			File result = new File(fileName).getCanonicalFile();
			Writer writer = new BufferedWriter(new FileWriter(result));
			try {
				writer.write("\"min\";\"mean\";\"max\"\n");
			} finally {
				writer.close();
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Unable to create file " + fileName, e);
		}
	}
	
	private String newFileName() {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
		return "logs/client-timings-" + format.format(new Date()) + ".csv";
	}
}
