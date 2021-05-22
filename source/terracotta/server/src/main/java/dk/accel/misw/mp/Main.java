package dk.accel.misw.mp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.tc.admin.TCStop;
import com.tc.server.TCServerMain;

import dk.accel.misw.mp.model.common.util.MainUtil;
import dk.accel.misw.mp.model.common.util.TerracottaUtil;
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
	
	public static void main(String[] args) throws IOException, InterruptedException {
		args = MainUtil.parseArgs(args, 2, 32, "[SERVER] java dk.accel.misw.mp.Main <server-id> localhost-ip [other-servers-ip]*");

		String name = args[0];
		final String thisIp = args[1];
		List<String> serverIpList = new ArrayList<String>();
		for (int i = 2; i < args.length; i++) {
			serverIpList.add(args[i]);
		}
		System.setProperty("java.rmi.server.hostname", thisIp);
		LOG.info("Starting server with id '" + name + "'");

		final String tcConfig = TerracottaUtil.createConfigFile(serverIpList);
		// start embedded terracotta server
		long start = System.currentTimeMillis();
		ExecutorService es = Executors.newSingleThreadExecutor();
		es.execute(new Runnable() {
			@Override
			public void run() {
				try {
					TCServerMain.main(new String[] { "-f", tcConfig, "-n", thisIp });
				} catch (Throwable e) {
					LOG.warning("Unable to start terracotta server");
					throw new RuntimeException(e);
				}
			}
		});
		Thread.sleep(20L * 1000);
		LOG.info("Terracotta server started in " + (System.currentTimeMillis() - start) + " ms");
		
		final ServerImplFactory factory = new ServerImplFactory(name, tcConfig);
		final TurbineSubscriptionServiceManagerAdmin nodeManager = factory.newTurbineSubscriptionServiceManager();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				LOG.info("Stopping server");
				try {
					try {
						TCStop.main(new String[] { "-f", tcConfig, "-n", thisIp });
					} catch (Throwable e) {
						LOG.warning("Unable to stop terracotta server");
					}
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
