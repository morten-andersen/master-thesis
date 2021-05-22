package dk.accel.misw.mp.model.node.mock;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import dk.accel.misw.mp.model.common.util.StdUtil;
import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil.ItemFactory;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;

public class SensorMock {

	private static final Random RND = new Random();
	
	private static final double STEP = 0.1;
	private static final long MAX = 10000L;

	private final SensorId id;
	private final AtomicReference<Double> value = new AtomicReference<Double>(Double.valueOf(0.0));

	public SensorMock(SensorId id) {
		this.id = StdUtil.checkForNull(id);
	}

	public SensorId getSensorId() {
		return id;
	}

	public SensorValue getValue() {
		while (true) {
			Double previous = value.get();
			if (RND.nextInt(100) >= 20) { // in 20% of the calls we don't change the value
				double next = (previous + STEP) % MAX;
				if (!value.compareAndSet(previous, next)) {
					continue; // racing condition - retry
				}
			}
			return new SensorValue(id, previous);
		}
	}
	
	private static class SensorMockFactory implements ItemFactory<SensorId, SensorMock> {
		@Override
		public SensorMock newInstance(SensorId key) {
			return new SensorMock(key);
		}
	}
	
	public static final ItemFactory<SensorId, SensorMock> SENSOR_MOCK_FACTORY = new SensorMockFactory();
}
