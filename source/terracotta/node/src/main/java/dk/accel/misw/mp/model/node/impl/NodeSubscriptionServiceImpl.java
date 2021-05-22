package dk.accel.misw.mp.model.node.impl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.api.TerracottaClient;
import org.terracotta.collections.ClusteredMap;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil;
import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.common.util.TerracottaUtil;
import dk.accel.misw.mp.model.node.mock.SensorMock;
import dk.accel.misw.mp.model.node.mock.SensorMockFactory;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

class NodeSubscriptionServiceImpl implements NodeSubscriptionService {

	private static final Logger LOG = Logger.getLogger(NodeSubscriptionServiceImpl.class.getName());
	
	private static final long INTERVAL = Long.getLong(NodeSubscriptionServiceImpl.class.getName() + ".interval", 500L);
	
	private final TurbineId turbine;
	private final String tcConfig;
	
	/**
	 * Maps serverId's to {@link TurbineSubscription} jobs.
	 */
	private final ConcurrentMap<String, ScheduledFuture<?>> turbineSubscriptionTaskMap = new ConcurrentHashMap<String, ScheduledFuture<?>>();
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(ExecutorsUtil.newNamedThreadFactory("data-generator", Executors.defaultThreadFactory()));
	private final ConcurrentMap<SensorId, SensorMock> knownSensors = new ConcurrentHashMap<SensorId, SensorMock>();
	
	/**
	 * This is the terracotta distributed shared memory. 
	 */
	private final AtomicReference<ClusteredMap<String, byte[]>> sensorValues = new AtomicReference<ClusteredMap<String,byte[]>>();
	
	NodeSubscriptionServiceImpl(TurbineId turbine, String tcConfig) {
		this.turbine = Preconditions.checkNotNull(turbine);
		this.tcConfig = Preconditions.checkNotNull(tcConfig);
		this.sensorValues.set(NodeSubscriptionServiceImpl.lookupClusteredMap(turbine, tcConfig));
	}

	/**
	 * Looks up the localhost terracotta server, and use the config from 
	 * it to find the shared map with sensor values.
	 */
	static ClusteredMap<String, byte[]> lookupClusteredMap(TurbineId turbine, String tcConfig) {
		TerracottaClient client = new TerracottaClient(tcConfig);
		ClusteringToolkit tcToolkit = client.getToolkit();
		return tcToolkit.getMap(NodeSubscriptionService.SHARED_MAP_ID + turbine.getTurbine());
	}
	
	@Override
	public TurbineId getTurbineId() {
		return turbine;
	}

	@Override
	public void stop() {
		for (ScheduledFuture<?> future : turbineSubscriptionTaskMap.values()) {
			future.cancel(true);
		}
		executor.shutdown();
		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void updateSubscription(TurbineSubscriptionInfo subscriptionInfo) {
		ScheduledFuture<?> f = null;
		if (!subscriptionInfo.getSensorIdList().isEmpty()) {
			addSensorList(subscriptionInfo.getSensorIdList());
			TurbineSubscription subscription = new TurbineSubscription(subscriptionInfo);
			f = executor.scheduleAtFixedRate(subscription, INTERVAL, INTERVAL, TimeUnit.MILLISECONDS);
		}
		LOG.info("Subscription from '" + subscriptionInfo.getServerId() + "' (gen = " + subscriptionInfo.getGeneration() + ")");
		this.sensorValues.set(NodeSubscriptionServiceImpl.lookupClusteredMap(turbine, tcConfig));
		ScheduledFuture<?> previous;
		if (f != null) {
			previous = turbineSubscriptionTaskMap.put(subscriptionInfo.getServerId(), f);
		} else {
			previous = turbineSubscriptionTaskMap.remove(subscriptionInfo.getServerId());
		}
		if (previous != null) {
			previous.cancel(true);
		}
	}

	private void addSensorList(List<SensorId> sensorIdList) {
		for (SensorId id : sensorIdList) {
			ConcurrentMapUtil.getItemCreateIfAbsent(id, knownSensors, SensorMockFactory.SENSOR_STANDARD_MOCK_FACTORY);
		}
	}
	
	/**
	 * Task that generates sensor mock data and puts them in the 
	 * terracotta distributed shared map.
	 * 
	 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
	 */
	private class TurbineSubscription implements Runnable {
		private final TurbineSubscriptionInfo subscriptionInfo;
		
		private TurbineSubscription(TurbineSubscriptionInfo subscriptionInfo) {
			this.subscriptionInfo = Preconditions.checkNotNull(subscriptionInfo);
		}

		@Override
		public void run() {
			System.err.print(".");
			try {
				for (SensorId id : subscriptionInfo.getSensorIdList()) {
					SensorMock mock = NodeSubscriptionServiceImpl.this.knownSensors.get(id);
					NodeSubscriptionServiceImpl.this.sensorValues.get().putNoReturn(id.getId(), TerracottaUtil.serialize(mock.getValue()));
				}
				LOG.log(Level.INFO, "wrote " + subscriptionInfo.getSensorIdList().size() + " sensorvalues to dsm");
			} catch (Throwable t) {
				LOG.log(Level.WARNING, "unable to publish to dsm", t);
			}
		}
	}
}
