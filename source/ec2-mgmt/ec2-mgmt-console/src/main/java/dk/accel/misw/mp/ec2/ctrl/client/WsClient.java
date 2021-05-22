package dk.accel.misw.mp.ec2.ctrl.client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.ec2.ctrl.EC2ControllerSvc;

public class WsClient implements EC2ControllerSvc {
	
	private static final Logger LOG = Logger.getLogger(EC2ControllerSvc.class.getName());
	
	public static enum State { 
		stopped, started;
	
		public State not() {
			return State.not(this);
		}
		
		public static State not(State state) {
			return (started.equals(state) ? stopped : stopped);
		}
	}
	
	private final String host;
	private final EC2ControllerSvc controllerService;
	private final AtomicReference<State> state = new AtomicReference<State>(State.stopped);
	private final AtomicInteger periodsSinceStateChange = new AtomicInteger();
	
	public WsClient(String host) throws IOException, InterruptedException {
		this.host = Preconditions.checkNotNull(host);
		this.controllerService = EC2ControllerSvc.Factory.newClient(host + ":8080");
	}
	
	@Override
	public void init(String s3key, EC2ControllerSvc.Environment env) throws IOException {
		controllerService.init(s3key, env);
	}

	@Override
	public NodeNetworkInfoList probeNetwork(List<String> ipAddresses) throws IOException {
		return controllerService.probeNetwork(ipAddresses);
	}

	@Override
	public void start() throws IOException {
		long start = System.currentTimeMillis();
		controllerService.start();
		state.set(State.started);
		periodsSinceStateChange.set(0);
		LOG.info("started program on " + host + " in " + (System.currentTimeMillis() - start) + " ms");
		
	}

	@Override
	public void stop() throws IOException {
		long start = System.currentTimeMillis();
		controllerService.stop();
		state.set(State.stopped);
		periodsSinceStateChange.set(0);
		LOG.info("stopped program on " + host + " in " + (System.currentTimeMillis() - start) + " ms");
	}
	
	@Override
	public String uploadData(String ignored) throws IOException {
		return controllerService.uploadData(host);
	}
	
	@Override
	public String toString() {
		return "[Client for '" + host + "']";
	}
	
	public int incrementAndGetPeriodCount() {
		return periodsSinceStateChange.incrementAndGet();
	}
	
	public State getState() {
		return state.get();
	}
}
