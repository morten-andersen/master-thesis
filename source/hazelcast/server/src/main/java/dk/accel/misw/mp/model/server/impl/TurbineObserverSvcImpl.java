package dk.accel.misw.mp.model.server.impl;

import java.rmi.RemoteException;
import java.util.List;

import com.google.common.base.Preconditions;
import com.hazelcast.core.MessageListener;

import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

/**
 * A hazelcast topic listener. One instance of this exists for each node in the system.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
class TurbineObserverSvcImpl implements MessageListener<List<SensorValue>> {

	private final TurbineId turbine;
	private final SubscriptionMultiplexer subscriptionMultiplexer;
	
	TurbineObserverSvcImpl(TurbineId turbine, SubscriptionMultiplexer subscriptionMultiplexer) throws RemoteException {
		this.turbine = Preconditions.checkNotNull(turbine);
		this.subscriptionMultiplexer = Preconditions.checkNotNull(subscriptionMultiplexer);
	}

	@Override
	public void onMessage(List<SensorValue> sensorValues) {
		subscriptionMultiplexer.updateSensorValues(turbine, sensorValues);
	}
}
