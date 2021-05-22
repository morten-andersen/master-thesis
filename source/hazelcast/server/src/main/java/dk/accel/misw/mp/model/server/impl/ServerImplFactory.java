package dk.accel.misw.mp.model.server.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.xml.ws.Endpoint;

import dk.accel.misw.mp.model.server.TurbineSubscriptionServiceManagerAdmin;
import dk.accel.misw.mp.model.server.client.ClientPublishSvc;
import dk.accel.misw.mp.model.server.client.ClientSubscriptionSvc;
import dk.accel.misw.mp.model.server.client.wstypes.XmlConstants;

/**
 * Factory for creating package scoped classes in the server.
 * 
 * Although public this is not part of the public API for the server.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class ServerImplFactory {

	private static final Logger LOG = Logger.getLogger(ServerImplFactory.class.getName());
	
	private final SubscriptionMultiplexer subscriptionMultiplexer;
	private final ClientPublishSvc clientPublishSvc;
	private final ClientSubscriptionSvc clientSubscriptionSvc;
	
	private final AtomicReference<Endpoint> clientPublishSvcEndpoint = new AtomicReference<Endpoint>();
	private final AtomicReference<Endpoint> clientSubscriptionSvcEndpoint = new AtomicReference<Endpoint>();
	
	public ServerImplFactory(String serverId) {
		this.subscriptionMultiplexer = new SubscriptionMultiplexerImpl(serverId);
		this.clientPublishSvc = new ClientPublishSvcImpl(subscriptionMultiplexer);
		this.clientSubscriptionSvc = new ClientSubscriptionSvcImpl(subscriptionMultiplexer);
	}
	
	public TurbineSubscriptionServiceManagerAdmin newTurbineSubscriptionServiceManager(String thisIp, List<String> serverIpList) {
		return new TurbineSubscriptionServiceManagerImpl(subscriptionMultiplexer, new BroadcastServer(), thisIp, serverIpList);
	}
	
	public void startClientServers() {
		LOG.info("Listening for clients on port: " + XmlConstants.BASE_URL);
		clientSubscriptionSvcEndpoint.compareAndSet(null, Endpoint.publish(XmlConstants.BASE_URL + ClientSubscriptionSvc.WS_SERVICE_NAME, clientSubscriptionSvc));
		clientPublishSvcEndpoint.compareAndSet(null, Endpoint.publish(XmlConstants.BASE_URL + ClientPublishSvc.WS_SERVICE_NAME, clientPublishSvc));
	}
	
	public void stopClientServers() {
		stopEndpoint(clientSubscriptionSvcEndpoint.get());
		stopEndpoint(clientPublishSvcEndpoint.get());
	}
	
	private void stopEndpoint(Endpoint e) {
		if (e != null) {
			e.stop();
		}
	}
}
