package dk.accel.misw.mp.model.node.mock;

import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;

public interface SensorMock {

	SensorId getSensorId();

	SensorValue getValue();
}
