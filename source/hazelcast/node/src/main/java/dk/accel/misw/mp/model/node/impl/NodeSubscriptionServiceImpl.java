package dk.accel.misw.mp.model.node.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.ITopic;

import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil;
import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.node.mock.SensorMock;
import dk.accel.misw.mp.model.node.mock.SensorMockFactory;
import dk.accel.misw.mp.model.server.HazelcastUtils;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

class NodeSubscriptionServiceImpl implements NodeSubscriptionService {

	private static final Logger LOG = Logger.getLogger(NodeSubscriptionServiceImpl.class.getName());
	
	private static final long INTERVAL = Long.getLong(NodeSubscriptionServiceImpl.class.getName() + ".interval", 500L);
	
	private final TurbineId turbine;
	
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(ExecutorsUtil.newNamedThreadFactory("data-generator", Executors.defaultThreadFactory()));
	private final ConcurrentMap<SensorId, SensorMock> knownSensors = new ConcurrentHashMap<SensorId, SensorMock>();
	
	private final HazelcastClient hazelcastClient;
	private final TurbineSubscription callbackTask;
	
	NodeSubscriptionServiceImpl(TurbineId turbine, List<String> serverIpList) {
		this.turbine = Preconditions.checkNotNull(turbine);
		this.hazelcastClient = HazelcastClient.newHazelcastClient(HazelcastUtils.GROUP_NAME, HazelcastUtils.GROUP_PASSWORD, serverIpList.toArray(new String[serverIpList.size()]));
		this.callbackTask = new TurbineSubscription();
		executor.scheduleAtFixedRate(callbackTask, INTERVAL, INTERVAL, TimeUnit.MILLISECONDS);
	}

	@Override
	public TurbineId getTurbineId() {
		return turbine;
	}

	@Override
	public void updateSubscription(TurbineSubscriptionInfo subscriptionInfo) {
		if (!subscriptionInfo.getSensorIdList().isEmpty()) {
			addSensorList(subscriptionInfo.getSensorIdList());
		}
		LOG.info("Subscription from '" + subscriptionInfo.getServerId() + "' (gen = " + subscriptionInfo.getGeneration() + ")");
	}

	private void addSensorList(List<SensorId> sensorIdList) {
		for (SensorId id : sensorIdList) {
			ConcurrentMapUtil.getItemCreateIfAbsent(id, knownSensors, SensorMockFactory.SENSOR_STANDARD_MOCK_FACTORY);
		}
	}
	
	/**
	 * Task that generates sensor mock data and publishes them to the 
	 * Hazelcast {@link ITopic} where all server nodes are listening. 
	 * 
	 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
	 */
	private class TurbineSubscription implements Runnable {
		private final ITopic<List<SensorValue>> topic;
		
		private TurbineSubscription() {
			this.topic = NodeSubscriptionServiceImpl.this.hazelcastClient.getTopic(HazelcastUtils.TOPIC_NAME);
		}

		@Override
		public void run() {
			if (NodeSubscriptionServiceImpl.this.knownSensors.isEmpty()) {
				return;
			}
			
			System.err.print(".");
			List<SensorValue> sensorValues = new ArrayList<SensorValue>();
			for (SensorMock mock : NodeSubscriptionServiceImpl.this.knownSensors.values()) {
				sensorValues.add(mock.getValue());
			}
			try {
				topic.publish(sensorValues);
			} catch (Throwable t) {
				LOG.log(Level.WARNING, "unable to publish to topic", t);
			}
		}
	}
}
