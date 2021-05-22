package dk.accel.misw.mp.model.server.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.server.node.TurbineObserverSvc;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

class TurbineObserverSvcImpl implements TurbineObserverSvc {

	private final TurbineId turbine;
	private final SubscriptionMultiplexer subscriptionMultiplexer;
	private final TurbineObserverSvc remote;
	
	TurbineObserverSvcImpl(TurbineId turbine, SubscriptionMultiplexer subscriptionMultiplexer) throws RemoteException {
		this.turbine = Preconditions.checkNotNull(turbine);
		this.subscriptionMultiplexer = Preconditions.checkNotNull(subscriptionMultiplexer);
		this.remote = (TurbineObserverSvc) UnicastRemoteObject.exportObject(this, 0);
	}

	@Override
	public void updateSensorValues(List<SensorValue> sensorValues) {
		subscriptionMultiplexer.updateSensorValues(turbine, sensorValues);
	}

	TurbineObserverSvc getRemote() {
		return remote;
	}
}
