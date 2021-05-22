package dk.accel.misw.mp;

import java.io.File;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import dk.accel.misw.mp.model.common.util.MainUtil;
import dk.accel.misw.mp.model.server.TurbineSubscriptionServiceManagerAdmin;
import dk.accel.misw.mp.model.server.impl.ServerImplFactory;

public class Main {

	static {
		// we use specific IPv4 multicast adresses, therefore this is necessary on dual net stack systems like Linux
		System.setProperty("java.net.preferIPv4Stack", Boolean.toString(true));
		// System.setProperty("java.rmi.server.logCalls", Boolean.toString(true));

		File logDir = new File("logs");
		if (!logDir.isDirectory()) {
			logDir.mkdirs();
		}

		try {
			LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("logging.properties"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static final Logger LOG = Logger.getLogger(Main.class.getName());
	
	public static void main(String[] args) {
		args = MainUtil.parseArgs(args, 1, 2, "[SERVER] java dk.accel.misw.mp.Main <server-id> [localhost-ip]");

		String name = args[0];
		if (args.length == 2) {
			System.setProperty("java.rmi.server.hostname", args[1]);
		}
		LOG.info("Starting server with id '" + name + "'");
		
		final ServerImplFactory factory = new ServerImplFactory(name);
		final TurbineSubscriptionServiceManagerAdmin nodeManager = factory.newTurbineSubscriptionServiceManager();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				LOG.info("Stopping server");
				try {
					nodeManager.stop();
					factory.stopClientServers();
				} finally {
					MainUtil.deleteSignalFile(false);
				}
			}
		}));
		
		nodeManager.start();
		factory.startClientServers();
		MainUtil.deleteSignalFile(true);
	}
}
