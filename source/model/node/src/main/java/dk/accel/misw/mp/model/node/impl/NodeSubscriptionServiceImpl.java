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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil;
import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.common.util.StdUtil;
import dk.accel.misw.mp.model.node.impl.NodeImplFactory.ErrorHandler;
import dk.accel.misw.mp.model.node.mock.SensorMock;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.TurbineObserverSvc;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

class NodeSubscriptionServiceImpl implements NodeSubscriptionService {

	private static final Logger LOG = Logger.getLogger(NodeSubscriptionServiceImpl.class.getName());
	
	private static final long INTERVAL = Long.getLong(NodeSubscriptionServiceImpl.class.getName() + ".interval", 1000L);
	
	private final TurbineId turbine;
	
	/**
	 * Can be null.
	 */
	private final ErrorHandler errorHandler;
	
	private final AtomicReference<ScheduledFuture<?>> turbineSubscriptionTask = new AtomicReference<ScheduledFuture<?>>();
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(ExecutorsUtil.newNamedThreadFactory("data-generator", Executors.defaultThreadFactory()));
	private final ConcurrentMap<SensorId, SensorMock> knownSensors = new ConcurrentHashMap<SensorId, SensorMock>();
	
	NodeSubscriptionServiceImpl(TurbineId turbine, ErrorHandler errorHandler) {
		this.turbine = StdUtil.checkForNull(turbine);
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
		ScheduledFuture<?> previous = turbineSubscriptionTask.getAndSet(f);
		if (previous != null) {
			previous.cancel(true);
		}
	}

	private void addSensorList(List<SensorId> sensorIdList) {
		for (SensorId id : sensorIdList) {
			ConcurrentMapUtil.getItemCreateIfAbsent(id, knownSensors, SensorMock.SENSOR_MOCK_FACTORY);
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
			this.subscriptionInfo = StdUtil.checkForNull(subscriptionInfo);
			this.observer = StdUtil.checkForNull(observer);
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
				if (errorHandler != null) {
					errorHandler.onError(e);
				} else {
					LOG.log(Level.WARNING, "unable to communicate with observer", e);
				}
			}
		}
	}
}
