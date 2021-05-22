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
import dk.accel.misw.mp.model.server.client.wstypes.XmlSensorValue;
import dk.accel.misw.mp.model.server.client.wstypes.XmlToken;

/**
 * Interface for the client publish service "publishing" values for subscriptions
 * to the subscribing clients. Due to the request-response nature of HTTP this
 * is actually not real "publishing" of values, but instead the servlet where
 * clients polls for changed values.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
@WebService(name=ClientPublishSvc.WS_SERVICE_NAME, targetNamespace=XmlConstants.WS_NAMESPACE)
public interface ClientPublishSvc {

	public static final String WS_SERVICE_NAME = "ClientPublish";
	public static final String FULL_NAME = "dk.accel.misw.mp.model.server.client.ClientPublishSvc";
	
	/**
	 * A list with <b>changed</b> sensor readings since last call will be returned
	 * for the specified token.
	 */
	List<XmlSensorValue> getChangedValues(XmlToken token);
	
	public static class Factory {
		private static final Logger LOG = Logger.getLogger(ClientPublishSvc.class.getName());
		
		public static ClientPublishSvc newClient(String host, int connectAttempts, long connectFailSleep) throws IOException, InterruptedException {
			for (int i = 0; i < connectAttempts; i++) {
				try {
					Service service = Service.create(new URL("http://" + host + ":8090/" + WS_SERVICE_NAME + "?wsdl"), new QName(XmlConstants.WS_NAMESPACE, WS_SERVICE_NAME + "Service"));
					ClientPublishSvc result = service.getPort(ClientPublishSvc.class);
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
