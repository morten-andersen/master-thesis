package dk.accel.misw.mp.model.node.impl;

import dk.accel.misw.mp.model.node.BroadcastClient;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

/**
 * Factory for creating package scoped classes in the node.
 * 
 * Although public this is not part of the public API for the node.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class NodeImplFactory {

	public static BroadcastClient newBroadcaster(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService, NodeSubscriptionService exportedNodeSubscriptionService, String fixedServerUrl) {
		return new BroadcastClientImpl(turbineId, nodeSubscriptionService, exportedNodeSubscriptionService, fixedServerUrl);
	}
	
	public static NodeSubscriptionService newNodeSubscriptionService(TurbineId turbineId, String tcConfig) {
		return new NodeSubscriptionServiceImpl(turbineId, tcConfig);
	}
	
	/**
	 * For "out of band" handling of communication errors with the observer object.
	 * 
	 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
	 */
	public static interface ErrorHandler {
		void onError(String serverId, Throwable t);
	}
	
	private NodeImplFactory() {
		throw new AssertionError();
	}
}
