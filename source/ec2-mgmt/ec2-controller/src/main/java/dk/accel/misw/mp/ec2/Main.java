package dk.accel.misw.mp.ec2;

import java.util.logging.Logger;

import dk.accel.misw.mp.ec2.ctrl.impl.EC2ControllerSvcImplFactory;

public class Main {

	static {
		// IPv4 necessary on dual net stack systems like Linux
		System.setProperty("java.net.preferIPv4Stack", Boolean.toString(true));
	}
	
	private static final Logger LOG = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) throws Exception {
		LOG.info("Starting ec2 controller");

		final EC2ControllerSvcImplFactory factory = new EC2ControllerSvcImplFactory();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				factory.stopServer();
			}
		}));

		factory.startServer();
	}
}
