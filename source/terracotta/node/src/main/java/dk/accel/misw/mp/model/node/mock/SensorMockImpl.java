package dk.accel.misw.mp.model.node.mock;

import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil.ItemFactory;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;

/**
 * A simple implementation of {@link SensorMock} where a new value
 * is returned for each call to {@link #getValue()}.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
class SensorMockImpl implements SensorMock {

	private static final double STEP = 0.1;
	private static final long MAX = 10000L;

	private final SensorId id;
	private final AtomicReference<Double> value = new AtomicReference<Double>(Double.valueOf(0.0));

	private SensorMockImpl(SensorId id) {
		this.id = Preconditions.checkNotNull(id);
	}

	@Override
	public SensorId getSensorId() {
		return id;
	}

	@Override
	public SensorValue getValue() {
		while (true) {
			Double previous = value.get();
			double next = (previous + STEP) % MAX;
			if (!value.compareAndSet(previous, next)) {
				continue; // racing condition - retry
			}
			return new SensorValue(id, previous);
		}
	}
	
	static class SensorRandomMockFactory implements ItemFactory<SensorId, SensorMock> {
		@Override
		public SensorMock newInstance(SensorId key) {
			return new SensorMockImpl(key);
		}
	}

}
