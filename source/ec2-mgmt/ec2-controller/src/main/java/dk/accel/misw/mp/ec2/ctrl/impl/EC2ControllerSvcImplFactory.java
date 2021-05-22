package dk.accel.misw.mp.ec2.ctrl.impl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.ws.Endpoint;

import dk.accel.misw.mp.ec2.ctrl.EC2ControllerSvc;

/**
 * Factory for creating package scoped classes in the ec2 controller.
 * 
 * Although public this is not part of the public API.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class EC2ControllerSvcImplFactory {

	private final EC2ControllerSvc ec2ControllerSvc;
	private final AtomicReference<Endpoint> ec2ControllerSvcEndpoint = new AtomicReference<Endpoint>();
	
	public EC2ControllerSvcImplFactory() throws IOException {
		this.ec2ControllerSvc = new EC2ControllerSvcImpl();
	}
	
	public void startServer() {
		ec2ControllerSvcEndpoint.compareAndSet(null, Endpoint.publish(EC2ControllerSvc.BASE_URL + EC2ControllerSvc.WS_SERVICE_NAME, ec2ControllerSvc));
	}
	
	public void stopServer() {
		stopEndpoint(ec2ControllerSvcEndpoint.get());
	}
	
	private void stopEndpoint(Endpoint e) {
		if (e != null) {
			e.stop();
		}
	}
}
