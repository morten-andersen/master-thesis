package dk.accel.misw.mp.ec2;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.model.Instance;

import dk.accel.misw.mp.ec2.ctrl.EC2ControllerSvc;
import dk.accel.misw.mp.ec2.ctrl.client.WsClient;
import dk.accel.misw.mp.ec2.util.ExecutorsUtils;
import dk.accel.misw.mp.ec2.util.IOUtils;
import dk.accel.misw.mp.ec2.util.InstanceStorage;

public class Main implements Closeable {

	private static final Logger LOG = Logger.getLogger(Main.class.getName());

	private static enum Command { start, stop, probenet, runtests, fulltests }
	
	/**
	 * Initialize with fixed seed, so the server crash sequence are repeatable.
	 */
	private final Random rnd = new Random(37L);
	
	private final File root = new File(System.getProperty(Main.class.getCanonicalName() + ".root", "../..")).getCanonicalFile();
	private final File outputRoot;
	
	private final int count = CLIENT_COUNT + TURBINE_COUNT + SERVER_COUNT;
	private final S3Uploader s3;
	private final EC2Launcher ec2;
	
	private Main() throws IOException {
		if (!root.exists()) {
			throw new IllegalStateException("The software root '" + root + "' does not exist, please specify -D" + Main.class.getCanonicalName() + ".root");
		}
		outputRoot = Main.getOutputRoot();
		
		this.ec2 = new EC2Launcher();
		this.s3 = new S3Uploader();
	}
	
	static File getOutputRoot() throws IOException {
		File root = new File(System.getProperty(Main.class.getCanonicalName() + ".root", "../..")).getCanonicalFile();
		if (!root.exists()) {
			throw new IllegalStateException("The software root '" + root + "' does not exist, please specify -D" + Main.class.getCanonicalName() + ".root");
		}
		File result = new File(root, "run");
		if (!result.exists()) {
			IOUtils.mkdirs(result);
		}
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			LOG.warning("Usage Main <" + Arrays.toString(Command.values()) + ">");
			System.exit(1);
		}
		Command cmd = Command.valueOf(args[0]);
		
		Main main = new Main();
		try {
			switch (cmd) {
			case start:
				main.start();
				break;
			case stop:
				main.terminate();
				break;
			case probenet:
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
				main.probeNet("test", format.format(new Date()));
				break;
			case runtests:
			{
				String testProject = System.getProperty("test-project");
				if (testProject == null) {
					throw new IllegalArgumentException("-Dtest-project not set");
				}
				if (!new File(main.root, testProject).isDirectory()) {
					throw new IllegalArgumentException("Test project '" + testProject + "' does not exist");
				}
				
				// start by uploading files to S3
				main.uploadFiles(testProject);
				
				main.runTests(testProject);
				break;
			}
			case fulltests:
			{
				String testProject = System.getProperty("test-project");
				if (testProject == null) {
					throw new IllegalArgumentException("-Dtest-project not set");
				}
				if (!new File(main.root, testProject).isDirectory()) {
					throw new IllegalArgumentException("Test project '" + testProject + "' does not exist");
				}
				
				// test network before test
				main.probeNet(testProject, "before");

				// start by uploading files to S3
				main.uploadFiles(testProject);
								
				List<int[]> maxValues = new ArrayList<int[]>();
				// run tests 100 times
				for (int i = 0; i < TEST_RUN_COUNT; i++) {
					maxValues.add(main.runTests(testProject));
				}
				// test network after test
				main.probeNet(testProject, "after");
				
				// finally generate aggregated graphs and report
				RGraphUtils.dumpAggregatedTimingResults(maxValues, main.outputRoot, testProject);
				
				LyxUtils.generateLyxReport(main.outputRoot, testProject);
				break;
			}
			default:
				throw new IllegalArgumentException("Unknown command " + cmd);
			}
		} finally {
			IOUtils.silentClose(main);
		}
	}
	
	private void start() throws IOException, InterruptedException {
		if (isRunning()) {
			LOG.info("Nodes already running");
			return;
		}
		
		String ec2group = ec2.createSecurityGroup();
		List<Instance> instances = startEC2VMs(count, ec2group);
		InstanceStorage.store(ec2group, instances, new File(outputRoot, ".instances.bin"));
		LOG.info("Launched and stored " + instances.size() + " nodes");
	}
	
	private void terminate() throws IOException, InterruptedException {
		if (!isRunning()) {
			return;
		}
		List<Instance> instances = new ArrayList<Instance>();
		String ec2group = loadInstances(instances);

		stopEC2VMs(instances);
		ec2.deleteSecurityGroup(ec2group);
		LOG.info("Loaded and stopped " + instances.size() + " nodes");
		new File(outputRoot, ".instances.bin").delete();
	}
	
	private void probeNet(String outputPrefix, String outputPostfix) throws IOException, InterruptedException {
		if (!isRunning()) {
			return;
		}
		List<Instance> instances = new ArrayList<Instance>();
		loadInstances(instances);
		
		LOG.info("Probing network on " + instances.size() + " nodes");
		final List<String> ipAddresses = Collections.unmodifiableList(unwrapIpAddresses(instances));
		
		// to avoid a problem where the WS stack fails in loading with a 
		// "Could not initialize class com.sun.xml.internal.bind.api.Messages"
		// error, when "bombarded" from several threads, we connect to the 
		// first host in the main thread to get everything loaded.
		new WsClient(instances.get(0).getPublicIpAddress());
		
		// we do it twice and throw away the first run
		Map<String, List<EC2ControllerSvc.NodeNetworkInfo>> probeData = null;
		for (int i = 0; i < 2; i++) {
			ExecutorService executor = Executors.newFixedThreadPool(instances.size());
			try {
				Map<String, Future<List<EC2ControllerSvc.NodeNetworkInfo>>> futures = new LinkedHashMap<String, Future<List<EC2ControllerSvc.NodeNetworkInfo>>>();
				for (final Instance instance : instances) {
					futures.put(instance.getPrivateIpAddress(), executor.submit(new Callable<List<EC2ControllerSvc.NodeNetworkInfo>>() {
						@Override
						public List<EC2ControllerSvc.NodeNetworkInfo> call() throws Exception {
							WsClient client = new WsClient(instance.getPublicIpAddress());
							return client.probeNetwork(ipAddresses).list;
						}
					}));
				}
				probeData = ExecutorsUtils.unwrapFutures(futures);
			} finally {
				executor.shutdownNow();
			}
		}
		RGraphUtils.dumpNetProbeResults(probeData, outputRoot, outputPrefix, outputPostfix);
	}
	
	private int[] runTests(String testProject) throws IOException, InterruptedException {
		if (!isRunning()) {
			return new int[] { 0, 0 };
		}
		List<Instance> instances = new ArrayList<Instance>();
		loadInstances(instances);
				
		List<WsClient> wsClients = new ArrayList<WsClient>();
		List<WsClient> wsServerClients = startNodes(createServerInstanceEnvironments(instances), SERVER_KEY);
		wsClients.addAll(wsServerClients);
		Thread.sleep(10000L);
		
		wsClients.addAll(startNodes(createTurbineInstanceEnvironments(instances), TURBINE_KEY));
		Thread.sleep(10000L);
		
		wsClients.addAll(startNodes(createClientInstanceEnvironments(instances), CLIENT_KEY));
		
		// for registering server up times
		long start = System.currentTimeMillis();
		List<RGraphUtils.ServerUptimes> serversUptimes = new ArrayList<RGraphUtils.ServerUptimes>();
		for (int i = 0; i < SERVER_COUNT; i++) {
			serversUptimes.add(new RGraphUtils.ServerUptimes(start));
		}
		
		// finally the up-down of servers algorithm
		for (int i = 0; i < ROUND_NUMBER; i++) {
			List<WsClient.State> nextStates = determineServerStatesInNextPeriod(wsServerClients);
			launchReappearingServers(nextStates, wsServerClients, serversUptimes);
			killCrashedServers(nextStates, wsServerClients, serversUptimes);
			long sleep = randomPeriodLength();
			LOG.info("Round; " + i + ", length: " + sleep);
			Thread.sleep(sleep);
		}
		
		List<String> s3Results = stopNodes(wsClients);
		for (RGraphUtils.ServerUptimes serverUptime: serversUptimes) {
			serverUptime.end();
		}
		
		return downloadResults(testProject, s3Results, serversUptimes);
	}

	/**
	 * Uploads the 3 archives with the node software.
	 * <ul>
	 * <li>client</li>
	 * <li>turbine node</li>
	 * <li>windfarm server</li>
	 * </ul>
	 */
	private void uploadFiles(String testProject) throws IOException {
		LOG.info("Uploading '" + testProject + "' files to S3");
		File projectDir = new File(root, testProject);
		s3.put(CLIENT_KEY, new File(projectDir, "client/build/distributions/client-1.0.zip"));
		s3.put(TURBINE_KEY, new File(projectDir, "node/build/distributions/node-1.0.zip"));
		s3.put(SERVER_KEY, new File(projectDir, "server/build/distributions/server-1.0.zip"));
	}
	
	private int[] downloadResults(String testProject, List<String> s3keys, List<RGraphUtils.ServerUptimes> serversUptimes) throws IOException, InterruptedException {
		if (s3keys.isEmpty()) {
			return new int[] { 0, 0 };
		}
		
		DateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		File dest = new File(new File(outputRoot, testProject), sdf.format(new Date()) + "_timings");
		IOUtils.mkdirs(dest);
		LOG.info("Downloading " + s3keys.size() + " result files from S3 to " + dest);
		
		downloadResultsSubSet(s3keys.subList(0, SERVER_COUNT), dest, "server");
		downloadResultsSubSet(s3keys.subList(SERVER_COUNT, SERVER_COUNT + TURBINE_COUNT), dest, "turbine");
		File clientResultDir = downloadResultsSubSet(s3keys.subList(SERVER_COUNT + TURBINE_COUNT, SERVER_COUNT + TURBINE_COUNT + CLIENT_COUNT), dest, "client");
		
		return RGraphUtils.dumpClientTimingsResults(clientResultDir, outputRoot, testProject, serversUptimes);
	}
	
	private File downloadResultsSubSet(List<String> s3keys, File destRoot, String group) throws IOException {
		File dest = new File(destRoot, group);
		dest.mkdirs();
		for (String s3key : s3keys) {
			File f = new File(dest, s3key);
			s3.get(s3key, f);
			s3.delete(s3key);
		}
		return dest;
	}
	
	/**
	 * Launch {@code count} number of EC2 virtual machines.
	 */
	private List<Instance> startEC2VMs(int count, String ec2group) throws IOException, InterruptedException {
		return ec2.launch(count, ec2group);
	}
	
	/**
	 * Stops the EC2 virtual machines in the {@code instances} list.
	 */
	private void stopEC2VMs(List<Instance> instances) throws IOException, InterruptedException {
		List<String> instanceIds = new ArrayList<String>();
		for (Instance instance : instances) {
			instanceIds.add(instance.getInstanceId());
		}
		ec2.terminate(instanceIds);
	}
	
	/**
	 * Initialise and start the {@code s3key} software on all the nodes in the {@code instances}
	 * list, by downloading the software archive from S3.
	 */
	private List<WsClient> startNodes(List<InstanceEnvironment> instances, final String s3key) throws IOException, InterruptedException {
		LOG.info("Init " + instances.size() + " nodes with '" + s3key + "'");
		// to avoid a problem where the WS stack fails in loading with a 
		// "Could not initialize class com.sun.xml.internal.bind.api.Messages"
		// error, when "bombarded" from several threads, we connect to the 
		// first host in the main thread to get everything loaded.
		new WsClient(instances.get(0).instance.getPublicIpAddress());
		
		ExecutorService executor = Executors.newFixedThreadPool(instances.size());
		try {
			List<Future<WsClient>> futures = new ArrayList<Future<WsClient>>();
			for (final InstanceEnvironment instance : instances) {
				futures.add(executor.submit(new Callable<WsClient>() {
					@Override
					public WsClient call() throws Exception {
						WsClient client = new WsClient(instance.instance.getPublicIpAddress());
						client.init(s3key, new EC2ControllerSvc.Environment(instance.env));
						client.start();
						return client;
					}
				}));
			}
			return ExecutorsUtils.unwrapFutures(futures);
		} finally {
			executor.shutdownNow();
		}
	}
	
	/**
	 * Stops the MP software on all the nodes in the {@code wsClients} list.
	 * 
	 * @return a list of s3keys with results that has been uploaded to S3.
	 */
	private List<String> stopNodes(List<WsClient> wsClients) throws IOException, InterruptedException {
		LOG.info("Stopping " + wsClients.size() + " nodes");
		ExecutorService executor = Executors.newFixedThreadPool(wsClients.size());
		try {
			List<Future<String>> futures = new ArrayList<Future<String>>();
			// we stop the nodes in reverse order
			List<WsClient> wsClientsCopy = new ArrayList<WsClient>(wsClients);
			Collections.reverse(wsClientsCopy);
			for (final WsClient client : wsClientsCopy) {
				futures.add(executor.submit(new Callable<String>() {
					@Override
					public String call() throws Exception {
						if (WsClient.State.started.equals(client.getState())) {
							client.stop();
						}
						return client.uploadData(null);
					}
				}));
			}
			List<String> result = ExecutorsUtils.unwrapFutures(futures);;
			Collections.reverse(result);
			return result;
		} finally {
			executor.shutdownNow();
		}
	}
	
	private List<WsClient.State> determineServerStatesInNextPeriod(List<WsClient> wsClients) {
		assert (SERVER_COUNT == wsClients.size());
		
		List<WsClient.State> futureStates = new ArrayList<WsClient.State>(wsClients.size());
		int startedCount = 0;
		for (WsClient client : wsClients) {
			WsClient.State newState = client.getState();
			if (client.incrementAndGetPeriodCount() > MAX_SAME_STATE_ROUNDS) {
				// forced state change due to max period count of 5 reached
				newState = newState.not();
			} else if (rnd.nextInt(100) < STATE_CHANGE_PERCENT) {
				// otherwise in 30% of the case -> switch
				newState = newState.not();
			}
			futureStates.add(newState);
			if (WsClient.State.started.equals(newState)) {
				startedCount++;
			}
		}
		
		// check that the condition that at least one server must be up is fullfilled
		if (startedCount == 0) {
			futureStates.set(rnd.nextInt(SERVER_COUNT), WsClient.State.started);
		}
		return futureStates;
	}
	
	private void launchReappearingServers(List<WsClient.State> futureStates, List<WsClient> wsClients, List<RGraphUtils.ServerUptimes> serversUptimes) throws IOException {
		assert (SERVER_COUNT == futureStates.size());
		assert (SERVER_COUNT == wsClients.size());
		assert (SERVER_COUNT == serversUptimes.size());
		
		for (int i = 0, n = futureStates.size(); i < n; i++) {
			WsClient client = wsClients.get(i);
			WsClient.State futureState = futureStates.get(i);
			if (!futureState.equals(client.getState())) {
				if (WsClient.State.started.equals(futureState)) {
					client.start();
					serversUptimes.get(i).up();
					LOG.info("Restarted server node " + i + " (" + client + ")");
				}
			}
		}
	}

	private void killCrashedServers(List<WsClient.State> futureStates, List<WsClient> wsClients, List<RGraphUtils.ServerUptimes> serversUptimes) throws IOException {
		assert (SERVER_COUNT == futureStates.size());
		assert (SERVER_COUNT == wsClients.size());
		assert (SERVER_COUNT == serversUptimes.size());
		
		for (int i = 0, n = futureStates.size(); i < n; i++) {
			WsClient client = wsClients.get(i);
			WsClient.State futureState = futureStates.get(i);
			if (!futureState.equals(client.getState())) {
				if (WsClient.State.stopped.equals(futureState)) {
					client.stop();
					serversUptimes.get(i).down();
					LOG.info("Stopped server node " + i + " (" + client + ")");
				}
			}
		}
	}
	
	private long randomPeriodLength() {
		return (rnd.nextInt(ROUND_MAX_LENGTH - ROUND_MIN_LENGTH) + ROUND_MIN_LENGTH);
	}
	
	@Override
	public void close() throws IOException {
		LOG.info("Deleting files from S3");
		try {
			for (String s : Arrays.asList(CLIENT_KEY, TURBINE_KEY, SERVER_KEY)) {
				s3.delete(s);
			}
		} finally {
			IOUtils.silentClose(s3);
			IOUtils.silentClose(ec2);
		}
	}
	
	private boolean isRunning() {
		File src = new File(outputRoot, ".instances.bin");
		if (!src.exists()) {
			LOG.warning("No " + src + " file, nothing to run tests on");
			return false;
		}
		return true;
	}
	
	private String loadInstances(List<Instance> dest) throws IOException {
		File src = new File(outputRoot, ".instances.bin");
		String ec2group = InstanceStorage.load(src, dest);
		LOG.info("Loaded " + dest.size() + " nodes");
		return ec2group;
	}

	private List<String> unwrapIpAddresses(List<Instance> instances) {
		List<String> result = new ArrayList<String>();
		for (Instance instance : instances) {
			result.add(instance.getPrivateIpAddress());
		}
		return result;
	}
	
	private static List<InstanceEnvironment> createServerInstanceEnvironments(List<Instance> instances) {
		List<InstanceEnvironment> result = new ArrayList<Main.InstanceEnvironment>();
		for (int i = 0; i < SERVER_COUNT; i++) {
			HashMap<String, String> env = new HashMap<String, String>();
			env.put("arg0", "server-" + i);
			// the ip-address of the host is necessary to get correctly registered in the RMI registry
			env.put("arg1", instances.get(i).getPrivateIpAddress());
			// the list of server instance ip addresses - some of the prototypes needs this list
			for (int j = 0; j < SERVER_COUNT; j++) {
				env.put("arg" + (j + 2), instances.get(j).getPrivateIpAddress());
			}
			result.add(new InstanceEnvironment(instances.get(i), env));
		}
		return result;
	}

	private static List<InstanceEnvironment> createTurbineInstanceEnvironments(List<Instance> instances) {
		List<InstanceEnvironment> result = new ArrayList<Main.InstanceEnvironment>();
		int offset  = SERVER_COUNT;
		for (int i = 0; i < TURBINE_COUNT; i++) {
			HashMap<String, String> env = new HashMap<String, String>();
			env.put("arg0", "turbine-" + i);
			// the list of server instance ip addresses
			for (int j = 0; j < SERVER_COUNT; j++) {
				env.put("arg" + (j + 1), instances.get(j).getPrivateIpAddress());
			}
			result.add(new InstanceEnvironment(instances.get(offset + i), env));
		}
		return result;
	}

	private static List<InstanceEnvironment> createClientInstanceEnvironments(List<Instance> instances) {
		List<InstanceEnvironment> result = new ArrayList<Main.InstanceEnvironment>();
		int offset = SERVER_COUNT + TURBINE_COUNT;
		for (int i = 0; i < CLIENT_COUNT; i++) {
			HashMap<String, String> env = new HashMap<String, String>();
			env.put("arg0", "client-" + i);
			// the list of server instance ip addresses
			for (int j = 0; j < SERVER_COUNT; j++) {
				env.put("arg" + (j + 1), instances.get(j).getPrivateIpAddress());
			}
			result.add(new InstanceEnvironment(instances.get(offset + i), env));
		}
		return result;
	}

	private static class InstanceEnvironment {
		final Instance instance;
		final HashMap<String, String> env;
		
		public InstanceEnvironment(Instance instance, HashMap<String, String> env) {
			this.instance = instance;
			this.env = env;
		}
	}
	
	private static final String CLIENT_KEY = "client.zip";
	private static final String TURBINE_KEY = "turbine.zip";
	private static final String SERVER_KEY = "server.zip";
	
	private static final int CLIENT_COUNT = 5; // 5; 1;
	private static final int TURBINE_COUNT = 10; // 10; 2;
	private static final int SERVER_COUNT = 3; // 3; 1;
	
	private static final int MAX_SAME_STATE_ROUNDS = 4;
	private static final int STATE_CHANGE_PERCENT = 30;
	
	/**
	 * milliseconds.
	 */
	private static final int ROUND_MIN_LENGTH = 20000;
	private static final int ROUND_MAX_LENGTH = 40000;
	private static final int ROUND_NUMBER = 20;
	
	static final int TEST_RUN_COUNT = 100;
}
