package dk.accel.misw.mp.model.server.node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;

public interface TurbineObserverSvc extends Remote {

	void updateSensorValues(List<SensorValue> sensorValues) throws RemoteException;
}
