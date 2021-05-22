package dk.accel.misw.mp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.ws.WebServiceException;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.client.WsClient;
import dk.accel.misw.mp.model.common.util.Constants;
import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.common.util.MainUtil;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorId;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorValue;
import dk.accel.misw.mp.model.server.client.wstypes.XmlToken;

public class Main {

	static {
		File logDir = new File("logs");
		if (!logDir.isDirectory()) {
			logDir.mkdirs();
		}

		try {
			LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("logging.properties"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static final Logger LOG = Logger.getLogger(Main.class.getName());
	
	public static final long INTERVAL = Long.getLong(Main.class.getName() + ".interval", 500L);
	
	private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2, ExecutorsUtil.newNamedThreadFactory("client-log-task", Executors.defaultThreadFactory()));
	
	private final String clientId;
	private final List<String> hosts;
	private final List<XmlSensorId> sensors = Main.generateRandomSensorList();
	
	private final WsClientTask clientTask; 
	
	private Main(String clientId, List<String> hosts) {
		this.clientId = Preconditions.checkNotNull(clientId);
		this.hosts = Collections.unmodifiableList(hosts);
		this.clientTask = new WsClientTask();
	}
	
	private void start() {
		LOG.info("Starting client " + clientId);
		EXECUTOR.scheduleAtFixedRate(clientTask, INTERVAL, INTERVAL, TimeUnit.MILLISECONDS);

	}
	
	private void stop() {
		LOG.info("Stopping client " + clientId);
		clientTask.unsubscribe();
	}
	
	public static void main(String[] args) throws Exception {
		args = MainUtil.parseArgs(args, 2, 32, "[CLIENT] java dk.accel.misw.mp.Main <clientId> <server-ip>+");
		String clientId = args[0];
		List<String> hosts = new ArrayList<String>();
		for (int i = 1; i < args.length; i++) {
			hosts.add(args[i]);
		}
		final Main main = new Main(clientId, hosts);
		main.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				LOG.info("Stopping client");
				try {
					EXECUTOR.shutdownNow();
					main.stop();
				} finally {
					MainUtil.deleteSignalFile(false);
				}
			}
		}));
		
		MainUtil.deleteSignalFile(true);
	}

	private static List<XmlSensorId> generateRandomSensorList() {
		Random random = new Random();
		List<XmlSensorId> sensorList = new ArrayList<XmlSensorId>();
		
		for (int i = 0; i < Constants.TURBINE_COUNT; i++) {
			String turbine = "turbine-" + i;
			for (int j = 0; j < (Constants.SUBSCRIPTION_COUNT_PER_CLIENT / Constants.TURBINE_COUNT); j++) {
				sensorList.add(new XmlSensorId(turbine, "sensor-" + random.nextInt(Constants.SENSORS_PER_TURBINE)));
			}
		}
		return Collections.unmodifiableList(sensorList);
	}
	
	class WsClientTask implements Runnable {

		private final AtomicReference<WsClient> client = new AtomicReference<WsClient>();
		private final AtomicReference<XmlToken> token = new AtomicReference<XmlToken>();
		private final ConcurrentMap<XmlSensorId, XmlSensorValue> values = new ConcurrentHashMap<XmlSensorId, XmlSensorValue>();
		private final LogTask logTask = new LogTask(this);
		
		private WsClientTask() {
			EXECUTOR.scheduleWithFixedDelay(logTask, 1000L, 10L, TimeUnit.MILLISECONDS);
		}
		
		@Override
		public void run() {
			WsClient client = this.client.get();
			XmlToken token = this.token.get();
			if (client == null) {
				while (true) {
//					String host = hosts.get(rnd.nextInt(hosts.size()));
					try {
						// fill the values map with empty values with 'now' as time for timekeeping
						long now = System.currentTimeMillis();
						for (XmlSensorId sensor : sensors) {
							values.putIfAbsent(sensor, new XmlSensorValue(sensor, 0.0, now));
						}
						
						// attempt to connect to all hosts at the same time, the first response wins
						List<Future<WsClient>> futures = new ArrayList<Future<WsClient>>(hosts.size());
						for (final String host : hosts) {
							futures.add(EXECUTOR.submit(new Callable<WsClient>() {
								@Override
								public WsClient call() throws IOException, InterruptedException {
									return new WsClient(host, 2, 500L);
								}
							}));
						}
						// convoluted construction, clean up on a rainy day
						boolean[] isFailed = new boolean[futures.size()];
						Arrays.fill(isFailed, false);
						while (client == null) {
							for (int i = 0, n = futures.size(); i < n; i++) {
								Future<WsClient> f = futures.get(i);
								if ((!isFailed[i]) && f.isDone()) {
									try {
										client = f.get();
										LOG.info("WsClient Connected to '" + hosts.get(i) + "'");
										break;
									} catch (ExecutionException e) {
										isFailed[i] = true;
										if (e.getCause() instanceof WebServiceException) {
											LOG.info("Unable to connect to '" + hosts.get(i) + "'");
										} else if (e.getCause() instanceof IOException) {
											throw new RuntimeException("host ip address error", e.getCause());
										}
									}
								}
							}
							boolean allFailed = true;
							for (int i = 0, n = isFailed.length; i < n; i++) {
								allFailed = isFailed[i];
								if (!allFailed) {
									break;
								}
							}
							if (allFailed) {
								// all failed, break to try with new clients
								return;
							}
						}
						
						// subscribe
						token = client.subscribe(sensors);
						this.client.set(client);
						this.token.set(token);
						LOG.info("WsClient subscribed");
						break;
					} catch (WebServiceException ignored) {
						LOG.info("Unable to subscribe (ignored)");
						client = null;
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			}
			try {
				getValues(client, token);
			} catch (WebServiceException e) {
				LOG.info("Error on getValues()");
				this.client.set(null);
				this.token.set(null);
			}
		}
		
		public void getValues(WsClient client, XmlToken token) {
			List<XmlSensorValue> newValues = client.getValues(token);
			// before updating we dump the timestamps, ie. the max age
			logTask.dump(true);
			LOG.info("Received " + newValues.size() + " values");
			for (XmlSensorValue val : newValues) {
				values.put(val.getSensor(), val);
				LOG.fine(val.toString());
			}
		}
		
		void unsubscribe() {
			WsClient client = this.client.getAndSet(null);
			XmlToken token = this.token.getAndSet(null);
			
			if ((client != null) && (token != null)) {
				client.unsubscribe(token);
			}
		}
		
		/**
		 * @return long[3] where ix0 = min, ix1 = mean, ix2 = max
		 */
		long[] dumpSensorValueAges() {
			long min = Long.MAX_VALUE;
			long max = 0;
			long sum = 0;
			int count = 0;
			
			for (XmlSensorValue val : values.values()) {
				long age = val.getAge();
				min = Math.min(min, age);
				max = Math.max(max, age);
				sum += age;
				count++;
			}
			if (count == 0) {
				min = 0;
			}
			long mean = (count == 0 ? 0 : sum / count);
			return new long[] { min, mean, max };
		}
	}
}
