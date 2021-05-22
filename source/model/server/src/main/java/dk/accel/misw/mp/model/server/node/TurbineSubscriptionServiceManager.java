package dk.accel.misw.mp.model.server.node;

import java.rmi.Remote;
import java.rmi.RemoteException;

import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

public interface TurbineSubscriptionServiceManager extends Remote {

	void registerNode(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService) throws RemoteException;
	
	void unRegisterNode(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService) throws RemoteException;
	
	boolean isRegistered(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService) throws RemoteException;
}
