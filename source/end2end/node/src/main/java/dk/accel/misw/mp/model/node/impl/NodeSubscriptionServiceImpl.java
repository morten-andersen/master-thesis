package dk.accel.misw.mp.model.node.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil;
import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.node.impl.NodeImplFactory.ErrorHandler;
import dk.accel.misw.mp.model.node.mock.SensorMock;
import dk.accel.misw.mp.model.node.mock.SensorMockFactory;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.TurbineObserverSvc;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

class NodeSubscriptionServiceImpl implements NodeSubscriptionService {

	private static final Logger LOG = Logger.getLogger(NodeSubscriptionServiceImpl.class.getName());
	
	private static final long INTERVAL = Long.getLong(NodeSubscriptionServiceImpl.class.getName() + ".interval", 500L);
	
	private final TurbineId turbine;
	
	/**
	 * Can be null.
	 */
	private final ErrorHandler errorHandler;
	
	/**
	 * Maps serverId's to {@link TurbineSubscription} jobs.
	 */
	private final ConcurrentMap<String, ScheduledFuture<?>> turbineSubscriptionTaskMap = new ConcurrentHashMap<String, ScheduledFuture<?>>();
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(ExecutorsUtil.newNamedThreadFactory("data-generator", Executors.defaultThreadFactory()));
	private final ConcurrentMap<SensorId, SensorMock> knownSensors = new ConcurrentHashMap<SensorId, SensorMock>();
	
	NodeSubscriptionServiceImpl(TurbineId turbine, ErrorHandler errorHandler) {
		this.turbine = Preconditions.checkNotNull(turbine);
		this.errorHandler = errorHandler;
	}

	@Override
	public TurbineId getTurbineId() {
		return turbine;
	}

	@Override
	public void updateSubscription(TurbineSubscriptionInfo subscriptionInfo, TurbineObserverSvc observer) {
		ScheduledFuture<?> f = null;
		if (!subscriptionInfo.getSensorIdList().isEmpty()) {
			addSensorList(subscriptionInfo.getSensorIdList());
			TurbineSubscription subscription = new TurbineSubscription(subscriptionInfo, observer);
			f = executor.scheduleAtFixedRate(subscription, INTERVAL, INTERVAL, TimeUnit.MILLISECONDS);
		}
		LOG.info("Subscription from '" + subscriptionInfo.getServerId() + "' (gen = " + subscriptionInfo.getGeneration() + ")");
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
	 * Task that generates sensor mock data and send them to the relevant {@link TurbineObserverSvc}.
	 * 
	 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
	 */
	private class TurbineSubscription implements Runnable {
		private final TurbineSubscriptionInfo subscriptionInfo;
		private final TurbineObserverSvc observer;
		
		private TurbineSubscription(TurbineSubscriptionInfo subscriptionInfo, TurbineObserverSvc observer) {
			this.subscriptionInfo = Preconditions.checkNotNull(subscriptionInfo);
			this.observer = Preconditions.checkNotNull(observer);
		}

		@Override
		public void run() {
			System.err.print(".");
			List<SensorValue> sensorValues = new ArrayList<SensorValue>();
			for (SensorId id : subscriptionInfo.getSensorIdList()) {
				SensorMock mock = NodeSubscriptionServiceImpl.this.knownSensors.get(id);
				sensorValues.add(mock.getValue());
			}
			try {
				observer.updateSensorValues(sensorValues);
			} catch (RemoteException e) {
				// cancel the subscription callback itself
				ScheduledFuture<?> future = NodeSubscriptionServiceImpl.this.turbineSubscriptionTaskMap.remove(subscriptionInfo.getServerId());
				if (future != null) {
					LOG.info("Error on callback to subscription from '" + subscriptionInfo.getServerId() + "' (gen = " + subscriptionInfo.getGeneration() + ")");
					future.cancel(false);
				}

				if (errorHandler != null) {
					errorHandler.onError(subscriptionInfo.getServerId(), e);
				} else {
					LOG.log(Level.WARNING, "unable to communicate with observer", e);
				}
			}
		}
	}
}
