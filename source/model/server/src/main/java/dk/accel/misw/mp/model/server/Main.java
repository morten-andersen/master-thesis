package dk.accel.misw.mp.model.server;

import java.util.logging.Logger;

import dk.accel.misw.mp.model.server.impl.ServerImplFactory;

public class Main {

	static {
		// we use specific IPv4 multicast adresses, therefore this is necessary on dual net stack systems like Linux
		System.setProperty("java.net.preferIPv4Stack", Boolean.toString(true));
	}
	
	private static final Logger LOG = Logger.getLogger(Main.class.getName());
	
	public static void main(String[] args) {
		if (args.length != 1) {
			printUsage();
		}
		
		String name = args[0];
		LOG.info("Starting server with id '" + name + "'");
		
		final ServerImplFactory factory = new ServerImplFactory();
		final TurbineSubscriptionServiceManagerAdmin nodeManager = factory.newTurbineSubscriptionServiceManager(name);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				nodeManager.stop();
				factory.stopClientServers();
			}
		}));
		
		nodeManager.start();
		factory.startClientServers();
	}

	private static void printUsage() {
		System.err.println("Usage: java dk.accel.misw.mp.model.server.Main <server-id>");
		System.exit(1);
	}

}
