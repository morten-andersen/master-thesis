package dk.accel.misw.mp.model.server.client;

import java.util.List;

import javax.jws.WebService;

import dk.accel.misw.mp.model.server.client.wstypes.XmlConstants;
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorId;
import dk.accel.misw.mp.model.server.client.wstypes.XmlToken;

/**
 * Interface for the client subscription service on the central server node.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
@WebService(name=ClientSubscriptionSvc.WS_SERVICE_NAME, targetNamespace=XmlConstants.WS_NAMESPACE)
public interface ClientSubscriptionSvc {

	public static final String WS_SERVICE_NAME = "ClientSubscription";
	public static final String FULL_NAME = "dk.accel.misw.mp.model.server.client.ClientSubscriptionSvc";
	
	/**
	 * Subscribe to a list of sensors. The returned token can be used in
	 * calls to {@link ClientPublishSvc#getChangedValues(XmlToken)} and
	 * of course in calls to {@link #unsubscribe(XmlToken)}.
	 */
	XmlToken subscribe(List<XmlSensorId> sensorList);

	/**
	 * Unsubscribes the subscription identified by the given token.
	 * 
	 * @return true if successfully unsubscribed. False is returned if
	 * no subscription exists for the specified token.
	 */
	boolean unsubscribe(XmlToken token);
}
