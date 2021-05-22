package dk.accel.misw.mp.model.server.client;

import java.util.List;

import javax.jws.WebService;

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
}
