package dk.accel.misw.mp.model.server.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.server.client.ClientPublishSvc;
import dk.accel.misw.mp.model.server.client.wstypes.XmlConstants;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorId;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorValue;
import dk.accel.misw.mp.model.server.client.wstypes.XmlToken;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.Token;

@WebService(endpointInterface = ClientPublishSvc.FULL_NAME, name = ClientPublishSvc.WS_SERVICE_NAME, portName = ClientPublishSvc.WS_SERVICE_NAME
		+ "Port", serviceName = ClientPublishSvc.WS_SERVICE_NAME + "Service", targetNamespace = XmlConstants.WS_NAMESPACE)
public class ClientPublishSvcImpl implements ClientPublishSvc {

	private final SubscriptionMultiplexer subscriptionMultiplexer;

	ClientPublishSvcImpl(SubscriptionMultiplexer subscriptionMultiplexer) {
		this.subscriptionMultiplexer = Preconditions.checkNotNull(subscriptionMultiplexer);
	}

	@Override
	public List<XmlSensorValue> getChangedValues(XmlToken token) {
		List<SensorValue> values = subscriptionMultiplexer.getChangedValues(new Token(token.getToken()));
		List<XmlSensorValue> result = new ArrayList<XmlSensorValue>(values.size());
		for (SensorValue value : values) {
			result.add(new XmlSensorValue(new XmlSensorId(value.getSensor()), value.getValue(), value.getTime()));
		}
		return result;
	}
}
