package dk.accel.misw.mp.ec2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;

import dk.accel.misw.mp.ec2.ctrl.EC2ControllerSvc;
import dk.accel.misw.mp.ec2.util.IOUtils;
import dk.accel.misw.mp.ec2.util.ProcessUtils;

/**
 * For emitting measurements and creating graphs using the R-Script library
 * (http://www.r-project.org/).
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
class RGraphUtils {

	/**
	 * For registering the periods where a server is up
	 * 
	 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
	 */
	static class ServerUptimes {
		
		private final List<Event> events = new ArrayList<Event>();
		private final long start;
		
		ServerUptimes(long start) {
			this.start = start;
			this.events.add(Event.up(0));
		}
		
		void up() {
			if (Event.EventType.up.equals(events.get(events.size() - 1).type)) {
				throw new IllegalStateException("two up events in a row");
			}
			events.add(Event.up((int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)));
		}

		void down() {
			events.add(Event.down((int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)));
		}
		
		void end() {
			down();
		}

		void dumpToFile(File dest) throws IOException {
			Writer writer = new BufferedWriter(new FileWriter(dest));
			try {
				writer.write("from;to\n");
				int i = 0;
				while ((i + 1) < events.size()) {
					// we always start in up
					Event up = events.get(i);
					Event down = events.get(i + 1);
					if (!Event.EventType.up.equals(up.type)) {
						throw new IllegalStateException("expected up at " + i);
					}
					if (!Event.EventType.down.equals(down.type)) {
						throw new IllegalStateException("expected up at " + (i + 1));
					}
					writer.write(Integer.toString(up.time) + ";" + down.time + "\n");
					i += 2;
				}
			} finally {
				writer.close();
			}
		}
		
		private static class Event {
			
			private static enum EventType { up, down }
			
			// in seconds since start
			private final int time;
			private final EventType type;
			
			private Event(int time, EventType type) {
				this.time = time;
				this.type = type;
			}
			
			static Event up(int time) {
				return new Event(time, EventType.up);
			}
			
			static Event down(int time) {
				return new Event(time, EventType.down);
			}
		}
	}
	
	static void dumpNetProbeResults(Map<String, List<EC2ControllerSvc.NodeNetworkInfo>> data, File outputRoot, String testProject, String outputPostfix) throws IOException, InterruptedException {
		DateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		File dest = new File(outputRoot, sdf.format(new Date()) + "_net");
		IOUtils.mkdirs(dest);
		
		File graphRoot = new File(outputRoot, "graphs-" + testProject);
		if (!graphRoot.exists()) {
			IOUtils.mkdirs(graphRoot);
		}

		for (EC2ControllerSvc.NodeNetworkInfo.ResultType dataType : EC2ControllerSvc.NodeNetworkInfo.ResultType.values()) {
			File csv = new File(dest, "network_" + dataType + ".csv");
	
			Writer writer = new BufferedWriter(new FileWriter(csv, true));
			try {
				StringBuilder headerLine = new StringBuilder();
				headerLine.append("IP");
				for (String ip : data.keySet()) {
					headerLine.append(",").append(ip);
				}
				headerLine.append("\n");
				writer.write(headerLine.toString());
				
				for (Map.Entry<String, List<EC2ControllerSvc.NodeNetworkInfo>> entry : data.entrySet()) {
					StringBuilder line = new StringBuilder();
					String srcIp = entry.getKey();
					line.append(srcIp);
					for (EC2ControllerSvc.NodeNetworkInfo destData : entry.getValue()) {
						line.append(',').append(srcIp.equals(destData.ip) ? "0" : destData.toString(dataType));
					}
					line.append("\n");
					writer.write(line.toString());
				}
			} finally {
				writer.close();
			}
			
			visualizeNetProbeResults(csv, dataType, graphRoot, testProject, outputPostfix);
		}
	}
	
	/**
	 * @return int[2] - pos 0 = mean of the max values, pos 1 = max
	 */
	static int[] dumpClientTimingsResults(File srcDir, File outputRoot, String testProject, List<RGraphUtils.ServerUptimes> serversUptimes) throws IOException, InterruptedException {
		int maxMax = 0;
		int maxSum = 0;
		int count = 0;
		
		List<String> clients = new ArrayList<String>();
		File csvDir = srcDir.getParentFile();
		for (File f : srcDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".zip");
			}
		})) {
			String clientIp = Splitter.on('-').split(f.getName()).iterator().next();
			clients.add(clientIp);
			final ZipFile zipFile = new ZipFile(f);
			try {
				for (Enumeration<? extends ZipEntry> it = zipFile.entries(); it.hasMoreElements();) {
					final ZipEntry entry = it.nextElement();
					if (entry.getName().endsWith(".csv")) {
						InputSupplier<InputStream> is = new InputSupplier<InputStream>() {
							@Override
							public InputStream getInput() throws IOException {
								return zipFile.getInputStream(entry);
							}
						};
						File to = new File(csvDir, clientIp + ".csv");
						Files.copy(is, to);
						// pos 0 = max, pos 1 = sum, pos 2 = count
						int[] max = parseClientTimingsMax(to);
						maxMax = Math.max(maxMax, max[0]);
						maxSum += max[1];
						count += max[2];
					}
				}
			} finally {
				zipFile.close();
			}
		}
		
		// dump server uptimes
		for (int i = 0, n = serversUptimes.size(); i < n; i++) {
			File f = new File(csvDir, "server-" + i + ".csv");
			serversUptimes.get(i).dumpToFile(f);
		}
		
		File graphRoot = new File(outputRoot, "graphs-" + testProject);
		if (!graphRoot.exists()) {
			IOUtils.mkdirs(graphRoot);
		}
		visualizeClientTimingsResults(csvDir, serversUptimes.size(), clients, graphRoot, testProject);
		
		return new int[] { (count != 0 ? (maxSum / count) : 0), maxMax };
	}
	
	static void dumpAggregatedTimingResults(List<int[]> maxValues, File outputRoot, String testProject) throws IOException, InterruptedException {
		File graphRoot = new File(outputRoot, "graphs-" + testProject);
		if (!graphRoot.exists()) {
			IOUtils.mkdirs(graphRoot);
		}
		// dump the values to a csv file
		File csv = new File(graphRoot, testProject + "-aggregated.csv");
		dumpMaxValuesToFile(maxValues, csv);

		visualizeAggregatedGraph(csv, graphRoot, maxValues.size(), testProject);
	}

	/**
	 * Parse the max-column (column 2) of the csv to file.
	 * 
	 * @return int[3], where pos 0 = max, pos 1 = sum, pos 2 = count
	 */
	static int[] parseClientTimingsMax(File csv) throws IOException {
		int max = 0;
		int sum = 0;
		int count = 0;
		BufferedReader reader = new BufferedReader(new FileReader(csv));
		try {
			// skip the header line
			reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				int val = Integer.parseInt(line.split(Pattern.quote(";"))[2]);
				max = Math.max(max, val);
				sum += val;
				count++;
			}
		} finally {
			reader.close();
		}
		return new int[] { max, sum, count };
	}

	private static void visualizeNetProbeResults(File csv, EC2ControllerSvc.NodeNetworkInfo.ResultType dataType, File destDir, String outputPrefix, String outputPostfix) throws IOException, InterruptedException {
		// call a R statistical script for generating a distance matrix
		String outputName = outputPrefix + "-network-" + dataType + "-" + outputPostfix + ".pdf";
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", new File(System.getProperty("user.dir")).getCanonicalPath() + "/etc/r-scripts/network-" + dataType + ".r " + csv.getAbsolutePath() + " " + outputName).redirectErrorStream(false);
		processBuilder.directory(destDir);
		Process process = processBuilder.start();
		
		ExecutorService streamExecutor = ProcessUtils.processStreams(process, System.out, System.err);
		int errCode = process.waitFor();
		if (errCode != 0) {
			throw new IOException("Failed in generating distance matrix, return code = " + errCode);
		}
		streamExecutor.shutdown();
		streamExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
	}
	
	private static void visualizeClientTimingsResults(File csvSrcDir, int serverCount, List<String> clients, File destDir, String testProject) throws IOException, InterruptedException {
		// call a R statistical script for generating the graph
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", new File(System.getProperty("user.dir")).getCanonicalPath() + "/etc/r-scripts/client-timings.r " + serverCount + " " + Joiner.on(' ').join(clients)).redirectErrorStream(false);
		processBuilder.directory(csvSrcDir);
		Process process = processBuilder.start();
		
		ExecutorService streamExecutor = ProcessUtils.processStreams(process, System.out, System.err);
		int errCode = process.waitFor();
		if (errCode != 0) {
			throw new IOException("Failed in generating client timings graph, return code = " + errCode);
		}
		streamExecutor.shutdown();
		streamExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
		
		// copy output pdf file(s)
		for (File f : csvSrcDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".pdf");
			}
		})) {
			Files.copy(f, new File(destDir, testProject + "-" + f.getName()));
			if (!f.delete()) {
				f.deleteOnExit();
			}
		}
	}
	
	private static void visualizeAggregatedGraph(File csv, File graphRoot, int runCount, String testProject) throws IOException, InterruptedException {
		// call a R statistical script for generating the graph
		String outputName = testProject + "-aggregated-timings.pdf";
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", new File(System.getProperty("user.dir")).getCanonicalPath() + "/etc/r-scripts/aggregated-timings.r " + csv.getAbsolutePath() + " " + outputName + " " + runCount).redirectErrorStream(false);
		processBuilder.directory(graphRoot);
		Process process = processBuilder.start();
		
		ExecutorService streamExecutor = ProcessUtils.processStreams(process, System.out, System.err);
		int errCode = process.waitFor();
		if (errCode != 0) {
			throw new IOException("Failed in generating aggregated client timings graph, return code = " + errCode);
		}
		streamExecutor.shutdown();
		streamExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
	}

	static void visualizeAggregatedCombinedGraph(File outputRoot, int runCount, String[] testProjects) throws IOException, InterruptedException {
		for (String testProject : testProjects) {
			File src = new File(outputRoot, "graphs-" + testProject + "/" + testProject + "-aggregated.csv");
			Files.copy(src, new File(outputRoot, src.getName()));
		}
		
		// call a R statistical script for generating the graph
		String outputName = "aggregated-timings-combined.pdf";
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", new File(System.getProperty("user.dir")).getCanonicalPath() + "/etc/r-scripts/aggregated-timings-combined.r " + outputName + " " + runCount + " " + Joiner.on(' ').join(testProjects)).redirectErrorStream(false);
		processBuilder.directory(outputRoot);
		Process process = processBuilder.start();
		
		ExecutorService streamExecutor = ProcessUtils.processStreams(process, System.out, System.err);
		int errCode = process.waitFor();
		if (errCode != 0) {
			throw new IOException("Failed in generating combined aggregated client timings graph, return code = " + errCode);
		}
		streamExecutor.shutdown();
		streamExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
	}

	private static void dumpMaxValuesToFile(List<int[]> values, File dest) throws IOException {
		Writer writer = new BufferedWriter(new FileWriter(dest));
		try {
			writer.write("mean;max\n");
			for (int[] v : values) {
				writer.write(Integer.toString(v[0]) + ";" + v[1] + "\n");
			}
		} finally {
			writer.close();
		}
	}
	
	private RGraphUtils() {
		throw new AssertionError();
	}
}
