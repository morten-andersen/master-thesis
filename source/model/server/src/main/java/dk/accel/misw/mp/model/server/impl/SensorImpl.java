package dk.accel.misw.mp.model.server.impl;

import java.util.concurrent.atomic.AtomicReference;

import dk.accel.misw.mp.model.common.util.StdUtil;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;

class SensorImpl implements Sensor {

	private final SensorId sensorId;
	private final AtomicReference<SensorValue> value = new AtomicReference<SensorValue>();

	SensorImpl(SensorId sensorId) {
		this.sensorId = StdUtil.checkForNull(sensorId);
	}

	@Override
	public SensorValue getValue() {
		return value.get();
	}
	
	@Override
	public void setValue(SensorValue val) {
		assert sensorId.equals(val.getSensor());
		
		value.set(val);
	}

	@Override
	public SensorId getSensorId() {
		return sensorId;
	}
}
