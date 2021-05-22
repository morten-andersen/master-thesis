package dk.accel.misw.mp.model.client;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import dk.accel.misw.mp.model.server.client.ClientPublishSvc;
import dk.accel.misw.mp.model.server.client.ClientSubscriptionSvc;
import dk.accel.misw.mp.model.server.client.wstypes.XmlConstants;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorId;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorValue;
import dk.accel.misw.mp.model.server.client.wstypes.XmlToken;

class WsClient {

	private static final Logger LOG = Logger.getLogger(WsClient.class.getName());
	
	private final int turbineCount;
	private final int sensorCount;
	
	private final ClientSubscriptionSvc subscriptionService;
	private final ClientPublishSvc publishService;
	
	WsClient(int turbineCount, int sensorCount) throws IOException {
		this.turbineCount = turbineCount;
		this.sensorCount = sensorCount;
		
		this.subscriptionService = getService(ClientSubscriptionSvc.class, ClientSubscriptionSvc.WS_SERVICE_NAME);
		this.publishService = getService(ClientPublishSvc.class, ClientPublishSvc.WS_SERVICE_NAME);
	}
	
	XmlToken subscribe() {
		List<XmlSensorId> sensorList = new ArrayList<XmlSensorId>();
		
		for (int i = 0; i < turbineCount; i++) {
			String turbine = "turbine-" + i;
			for (int j = 0; j < sensorCount; j++) {
				sensorList.add(new XmlSensorId(turbine, "sensor-" + j));
			}
		}
		
		XmlToken token = subscriptionService.subscribe(sensorList);
		LOG.info("Subscribe " + token);
		return token;
	}
	
	void unsubscribe(XmlToken token) {
		LOG.info("Unsubscribe status = " + subscriptionService.unsubscribe(token));
	}
	
	void getValues(XmlToken token) {
		List<XmlSensorValue> values = publishService.getChangedValues(token);
		LOG.info("Received " + values.size() + " values");
		for (XmlSensorValue val : values) {
			LOG.fine(val.toString());
		}
	}
	
	private static <T> T getService(Class<T> cls, String name) throws IOException {
		Service service = Service.create(new URL(XmlConstants.BASE_URL + name + "?wsdl"), new QName(XmlConstants.WS_NAMESPACE, name + "Service"));
		return service.getPort(cls);
	}
}
