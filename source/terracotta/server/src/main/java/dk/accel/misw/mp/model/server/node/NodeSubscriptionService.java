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

	/**
	 * Each turbine id has a distributed shared memory map
	 * named by this string, followed by the {@link TurbineId#getTurbine()}
	 * identifier.
	 */
	public static final String SHARED_MAP_ID = "SensorValueMap-";
	
	TurbineId getTurbineId() throws RemoteException;
	
	void updateSubscription(TurbineSubscriptionInfo subscriptionInfo) throws RemoteException;

	void stop() throws RemoteException;
}
