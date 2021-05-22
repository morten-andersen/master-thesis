package dk.accel.misw.mp.ec2.ctrl.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dk.accel.misw.mp.ec2.ctrl.EC2ControllerSvc.NodeNetworkInfo;
import dk.accel.misw.mp.ec2.util.ExecutorsUtils;
import dk.accel.misw.mp.ec2.util.IOUtils;
import dk.accel.misw.mp.ec2.util.ProcessUtils;

class NetworkProber {

	static List<NodeNetworkInfo> probe(List<String> ipAddresses) throws InterruptedException, IOException {
		ExecutorService executor = Executors.newFixedThreadPool(ipAddresses.size());
		List<Future<NodeNetworkInfo>> futures = new ArrayList<Future<NodeNetworkInfo>>(ipAddresses.size());
		for (final String ip : ipAddresses) {
			futures.add(executor.submit(new Callable<NodeNetworkInfo>() {
				@Override
				public NodeNetworkInfo call() throws IOException, InterruptedException {
					return probeNetwork(ip);
				}
			}));
		}
		executor.shutdown();
		if (!executor.awaitTermination(2, TimeUnit.MINUTES)) {
			throw new IOException("Network probe tasks did not finish within 2 minutes");
		}
		return ExecutorsUtils.unwrapFutures(futures);
	}
	
	private static double ping(String ip) throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", Command.HPING3.command(ip)).redirectErrorStream(false);
		Process process = processBuilder.start();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ExecutorService streamExecutor = ProcessUtils.processStreams(process, baos, System.err);
			int errCode = process.waitFor();
			if (errCode != 0) {
				throw new IOException("Failed in pinging " + ip + " return code = " + errCode);
			}
			streamExecutor.shutdown();
			streamExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
			return Double.parseDouble(baos.toString());
		} finally {
			IOUtils.silentClose(baos);
		}
	}
	
	private static NodeNetworkInfo probeNetwork(String ip) throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", Command.TRACEPATH.command(ip)).redirectErrorStream(false);
		Process process = processBuilder.start();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String dom0ip;
		int hops;
		try {
			ExecutorService streamExecutor = ProcessUtils.processStreams(process, baos, System.err);
			int errCode = process.waitFor();
			if (errCode != 0) {
				throw new IOException("Failed in pinging " + ip + " return code = " + errCode);
			}
			streamExecutor.shutdown();
			streamExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
			
			String[] lines = baos.toString().split("\\n");
			dom0ip = lines[0];
			hops = Integer.parseInt(lines[1]);
		} finally {
			IOUtils.silentClose(baos);
		}

		
		double rtt = ping(ip);
		return new NodeNetworkInfo(ip, dom0ip, hops, rtt);
	}
	
	private NetworkProber() {
		throw new AssertionError();
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(probe(Arrays.asList("93.176.81.125", "192.168.1.1")));
	}
	
	private static final int PING_COUNT = 10;
	
	private static enum Command {
		/**
		 * the awk script will take out the 'avg' of lines like 'rtt min/avg/max/mdev = 1.002/1.002/1.002/0.000 ms'
		 */
		PING("ping -c" + PING_COUNT + " -q -n ", "| awk 'BEGIN { FS = \"/\" }/rtt/{print $5}'"),
		
		/**
		 * the awk script will take out the 'avg' of lines like 'round-trip min/avg/max = 26.3/26.3/26.3 ms'
		 */
		HPING3("sudo hping3 -c " + PING_COUNT + " -q -S -p 8080 ", " 2>&1 | awk 'BEGIN { FS = \"/\" }/round-trip/{print $4}'"),
		
		/**
		 * 	the awk script prints the ip addres on line 2 (i.e the dom0 node) and the hop count from the resume line
		 */
		TRACEPATH("tracepath -n ", "| awk 'NR == 2 {print $2};/Resume/{print $5}'");
		
		private final String preIp;
		private final String postIp;
		
		Command(String preIp, String postIp) {
			this.preIp = preIp;
			this.postIp = postIp;
		}
		
		String command(String ip) {
			return preIp + ip + postIp;
		}
	}
}
