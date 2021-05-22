package dk.accel.misw.mp.model.server.impl;

import java.util.List;

import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.Token;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

interface SubscriptionMultiplexer {

	Token subscribe(List<SensorId> sensorList);
	
	boolean unsubscribe(Token token);
	
	List<SensorValue> getChangedValues(Token token);
	
	TurbineSubscriptionInfo getTurbineSubscriptionInfo(TurbineId turbine) throws InterruptedException;
	
	void updateSensorValues(TurbineId turbine, List<SensorValue> sensorValues);
	
	void markTurbineNodeDirty(TurbineId turbine);
}
