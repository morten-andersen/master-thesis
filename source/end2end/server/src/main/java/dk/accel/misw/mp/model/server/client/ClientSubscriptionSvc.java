package dk.accel.misw.mp.model.server.client;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

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
	
	public static class Factory {
		private static final Logger LOG = Logger.getLogger(ClientSubscriptionSvc.class.getName());
		
		public static ClientSubscriptionSvc newClient(String host, int connectAttempts, long connectFailSleep) throws IOException, InterruptedException {
			for (int i = 0; i < connectAttempts; i++) {
				try {
					Service service = Service.create(new URL("http://" + host + ":8090/" + WS_SERVICE_NAME + "?wsdl"), new QName(XmlConstants.WS_NAMESPACE, WS_SERVICE_NAME + "Service"));
					ClientSubscriptionSvc result = service.getPort(ClientSubscriptionSvc.class);
					LOG.info("Connected to " + host);
					return result;
				} catch (WebServiceException e) {
					if (i >= (connectAttempts - 1)) {
						throw e;
					} else {
						LOG.warning("Unable to connect to " + host + " (" + e.getMessage() + "), retrying in " + connectFailSleep + " milliseconds");
						Thread.sleep(connectFailSleep);
					}
				}
			}
			// we shouldn't get down here
			throw new AssertionError();
		}
	}
}
