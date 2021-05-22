package dk.accel.misw.mp.model.client;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import dk.accel.misw.mp.model.server.client.ClientPublishSvc;
import dk.accel.misw.mp.model.server.client.ClientSubscriptionSvc;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorId;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorValue;
import dk.accel.misw.mp.model.server.client.wstypes.XmlToken;

public class WsClient {

	private static final Logger LOG = Logger.getLogger(WsClient.class.getName());

	private final ClientSubscriptionSvc subscriptionService;
	private final ClientPublishSvc publishService;
	
	public WsClient(String host, int connectAttempts, long connectFailSleep) throws IOException, InterruptedException {
		this.subscriptionService = ClientSubscriptionSvc.Factory.newClient(host, connectAttempts, connectFailSleep);
		this.publishService = ClientPublishSvc.Factory.newClient(host, connectAttempts, connectFailSleep);
	}
	
	public XmlToken subscribe(List<XmlSensorId> sensors) {
		XmlToken token = subscriptionService.subscribe(sensors);
		LOG.info("Subscribe " + token);
		return token;
	}
	
	public void unsubscribe(XmlToken token) {
		LOG.info("Unsubscribe status = " + subscriptionService.unsubscribe(token));
	}
	
	public List<XmlSensorValue> getValues(XmlToken token) {
		return publishService.getChangedValues(token);
	}
}
