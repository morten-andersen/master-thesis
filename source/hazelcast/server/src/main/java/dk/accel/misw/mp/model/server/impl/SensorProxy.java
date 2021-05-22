package dk.accel.misw.mp.model.server.impl;

import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;

class SensorProxy {

	private final Sensor sensor;
	private final AtomicReference<SensorValue> lastValue = new AtomicReference<SensorValue>();
	
	SensorProxy(Sensor sensor) {
		this.sensor = Preconditions.checkNotNull(sensor);
	}
	
	/**
	 * Return the sensor value, if changed since last call to this method.
	 * If not changed (or not accessible yet) {@code null} is returned.
	 * <p>
	 * Implementation note: Thread safe implementation using spinning and CAS
	 * so values returned from calls to this method are strictly linearized.
	 */
	SensorValue getValueIfChanged() {
		while (true) {
			SensorValue lval = lastValue.get();
			SensorValue nval = sensor.getValue();
			
			if (lval == null) {
				if (lastValue.compareAndSet(null, nval)) {
					return nval;
				}
			} else if (lval.equals(nval)) {
				// no changes
				return null;			
			} else {
				if (lastValue.compareAndSet(lval, nval)) {
					return nval;
				}
			}
		}
	}
	
	Sensor getSensor() {
		return sensor;
	}
}
