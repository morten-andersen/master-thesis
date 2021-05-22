package dk.accel.misw.mp.model.server.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.server.client.ClientSubscriptionSvc;
import dk.accel.misw.mp.model.server.client.wstypes.XmlConstants;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorId;
import dk.accel.misw.mp.model.server.client.wstypes.XmlToken;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.Token;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

@WebService(endpointInterface = ClientSubscriptionSvc.FULL_NAME, name = ClientSubscriptionSvc.WS_SERVICE_NAME, portName = ClientSubscriptionSvc.WS_SERVICE_NAME
		+ "Port", serviceName = ClientSubscriptionSvc.WS_SERVICE_NAME + "Service", targetNamespace = XmlConstants.WS_NAMESPACE)
public class ClientSubscriptionSvcImpl implements ClientSubscriptionSvc {

	private final SubscriptionMultiplexer subscriptionMultiplexer;

	ClientSubscriptionSvcImpl(SubscriptionMultiplexer subscriptionMultiplexer) {
		this.subscriptionMultiplexer = Preconditions.checkNotNull(subscriptionMultiplexer);
	}

	@Override
	public XmlToken subscribe(List<XmlSensorId> xmlSensorList) {
		List<SensorId> sensorList = new ArrayList<SensorId>(xmlSensorList.size());
		for (XmlSensorId xmlSensorId : xmlSensorList) {
			sensorList.add(new SensorId(new TurbineId(xmlSensorId.getTurbine()), xmlSensorId.getSensor()));
		}

		return new XmlToken(subscriptionMultiplexer.subscribe(sensorList).getToken());
	}

	@Override
	public boolean unsubscribe(XmlToken token) {
		return subscriptionMultiplexer.unsubscribe(new Token(token.getToken()));
	}
}
