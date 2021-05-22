package dk.accel.misw.mp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
		args = MainUtil.parseArgs(args, 2, 32, "[SERVER] java dk.accel.misw.mp.Main <server-id> localhost-ip [other-servers-ip]*");

		String name = args[0];
		String thisIp = args[1];
		List<String> serverIpList = new ArrayList<String>();
		for (int i = 2; i < args.length; i++) {
			if (!thisIp.equals(args[i])) {
				serverIpList.add(args[i]);
			}
		}
		System.setProperty("java.rmi.server.hostname", thisIp);
		LOG.info("Starting server with id '" + name + "'");
		
		final ServerImplFactory factory = new ServerImplFactory(name);
		final TurbineSubscriptionServiceManagerAdmin nodeManager = factory.newTurbineSubscriptionServiceManager(thisIp, serverIpList);
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
