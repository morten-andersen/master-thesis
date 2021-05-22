package dk.accel.misw.mp.model.node.impl;

import java.io.IOException;
import java.rmi.NotBoundException;

import org.junit.Test;

import dk.accel.misw.mp.model.node.BroadcastClient;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

public class ManualBroadcasterTest {

	@Test
	public void testBroadcaster() throws IOException, NotBoundException {
		TurbineId turbineId = new TurbineId("turbine");
		NodeSubscriptionService nodeSubscriptionService = new NodeSubscriptionServiceImpl(turbineId, null);
		BroadcastClient target = new BroadcastClientImpl(turbineId, nodeSubscriptionService);
		target.connect(false);
	}
}
