package dk.accel.misw.mp.model.server.impl;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;

class SensorImpl implements Sensor {

	private final SensorId sensorId;
	private final TurbineSubscription turbineSubscription;

	SensorImpl(SensorId sensorId, TurbineSubscription turbineSubscription) {
		this.sensorId = Preconditions.checkNotNull(sensorId);
		this.turbineSubscription = Preconditions.checkNotNull(turbineSubscription);
	}

	@Override
	public SensorValue getValue() {
		return turbineSubscription.getSensorValue(sensorId);
	}
	
	@Override
	public SensorId getSensorId() {
		return sensorId;
	}
}
