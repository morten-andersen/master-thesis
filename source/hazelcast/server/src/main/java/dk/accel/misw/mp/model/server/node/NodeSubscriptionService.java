package dk.accel.misw.mp.model.server.node;

import java.rmi.Remote;
import java.rmi.RemoteException;

import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

/**
 * Must be implemented and exposed on all nodes.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public interface NodeSubscriptionService extends Remote {

	TurbineId getTurbineId() throws RemoteException;
	
	void updateSubscription(TurbineSubscriptionInfo subscriptionInfo) throws RemoteException;
}
