package dk.accel.misw.mp.model.server.impl;

import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;

interface Sensor {

	SensorId getSensorId();
	
	SensorValue getValue();
	
	void setValue(SensorValue value);
}
