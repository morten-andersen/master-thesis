package dk.accel.misw.mp.model.server.impl;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.api.TerracottaClient;
import org.terracotta.collections.ClusteredMap;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil;
import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil.ItemFactory;
import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.common.util.TerracottaUtil;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

class TurbineSubscription {

	private static final Logger LOG = Logger.getLogger(TurbineSubscription.class.getName());
	
	private static final Integer ONE = Integer.valueOf(1);

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final String serverId;
	private final TurbineId turbine;

	/**
	 * This is the terracotta distributed shared memory. 
	 */
	private final ClusteredMap<String, byte[]> sensorValues;

	private final ConcurrentMap<SensorId, WeakReference<Sensor>> knownSensors = new ConcurrentHashMap<SensorId, WeakReference<Sensor>>();

	/**
	 * Keeps information about numbers of subscriptions for each sensor.
	 * 
	 * Protected by {@link #rwLock}.
	 */
	private final Map<Sensor, Integer> sensorSubscriptionCount = new HashMap<Sensor, Integer>();

	/**
	 * Protected by {@link #rwLock}.
	 */
	private int generation = 0;

	private final Condition changedCondition = rwLock.writeLock().newCondition();

	/**
	 * Protected by {@link #rwLock}.
	 */
	private boolean changed = false;

	TurbineSubscription(String serverId, TurbineId turbine, String tcConfig) {
		this.serverId = Preconditions.checkNotNull(serverId);
		this.turbine = Preconditions.checkNotNull(turbine);
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(ExecutorsUtil
				.newDaemonThreadFactory("TurbineSubscr-" + turbine, Executors.defaultThreadFactory()));
		executor.scheduleWithFixedDelay(ConcurrentMapUtil.newWeakMapCleanerTask(knownSensors), 1000L, 1000L,
				TimeUnit.MILLISECONDS);
		this.sensorValues = TurbineSubscription.lookupClusteredMap(turbine, tcConfig);
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
	
	void waitForChanged() throws InterruptedException {
		rwLock.writeLock().lock();
		try {
			while (!changed) {
				changedCondition.await();
			}
			changed = false;
		} finally {
			rwLock.writeLock().unlock();
		}
	}

	TurbineSubscriptionInfo getSubscriptionInfo() {
		rwLock.readLock().lock();
		try {
			List<SensorId> sensorIdList = new ArrayList<SensorId>();
			for (Sensor sensor : sensorSubscriptionCount.keySet()) {
				sensorIdList.add(sensor.getSensorId());
			}
			return new TurbineSubscriptionInfo(serverId, generation, sensorIdList);
		} finally {
			rwLock.readLock().unlock();
		}
	}

	TurbineId getTurbine() {
		return turbine;
	}

	Sensor getSensor(SensorId sensorId) {
		return ConcurrentMapUtil.getItemCreateIfAbsentWeakMap(sensorId, knownSensors, sensorFactory);
	}
	
	SensorValue getSensorValue(SensorId sensorId) {
		byte[] val = sensorValues.get(sensorId.getId());
		if (val == null) {
			LOG.warning("no sensor data for " + sensorId.getId());
			return null;
		}
		try {
			SensorValue result = TerracottaUtil.deserialize(val, SensorValue.class);
			LOG.info("sensor age " + result.getAge() + " for " + result.getSensor().getId());
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	int getGeneration() {
		rwLock.readLock().lock();
		try {
			return generation;
		} finally {
			rwLock.readLock().unlock();
		}
	}

	boolean addAll(Collection<Sensor> sensorList) {
		boolean result = false;
		rwLock.writeLock().lock();
		try {
			for (Sensor s : sensorList) {
				assert this.turbine.equals(s.getSensorId().getTurbine());

				Integer old = sensorSubscriptionCount.get(s);
				if (old == null) {
					sensorSubscriptionCount.put(s, TurbineSubscription.ONE);
					result = true;
				} else {
					sensorSubscriptionCount.put(s, Integer.valueOf(old + 1));
				}
			}
			if (result) {
				generation++;
				changed = true;
				changedCondition.signal();
			}
		} finally {
			rwLock.writeLock().unlock();
		}
		return result;
	}

	boolean removeAll(Collection<Sensor> sensorList) {
		boolean result = false;
		rwLock.writeLock().lock();
		try {
			for (Sensor s : sensorList) {
				assert this.turbine.equals(s.getSensorId().getTurbine());

				Integer old = sensorSubscriptionCount.get(s);
				if (old == null) {
					throw new IllegalStateException("trying to remove sensor never subscribed (" + s + ")");
				} else {
					int i = old - 1;
					if (i == 0) {
						sensorSubscriptionCount.remove(s);
						result = true;
					} else if (i > 0) {
						sensorSubscriptionCount.put(s, i);
					} else {
						// negative should never happen
						throw new IllegalStateException("subscription count for sensor (" + s + ") is negative (" + i
								+ ")");
					}
				}
			}
			if (result) {
				generation++;
				changed = true;
				changedCondition.signal();
			}
		} finally {
			rwLock.writeLock().unlock();
		}
		return result;
	}

	private class SensorFactory implements ItemFactory<SensorId, Sensor> {
		@Override
		public Sensor newInstance(SensorId key) {
			return new SensorImpl(key, TurbineSubscription.this);
		}
	}

	private final SensorFactory sensorFactory = new SensorFactory();

	public void markDirty() {
		rwLock.writeLock().lock();
		try {
			changed = true;
		} finally {
			rwLock.writeLock().unlock();
		}
	}
}
