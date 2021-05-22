package dk.accel.misw.mp;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import dk.accel.misw.mp.model.common.util.MainUtil;
import dk.accel.misw.mp.model.common.util.TerracottaUtil;
import dk.accel.misw.mp.model.node.BroadcastClient;
import dk.accel.misw.mp.model.node.impl.NodeImplFactory;
import dk.accel.misw.mp.model.node.impl.NodeImplFactory.ErrorHandler;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

/**
 * Main class for starting a node.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
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
	
	public static void main(String[] args) throws Exception{
		args = MainUtil.parseArgs(args, 1, 32, "[TURBINE] java dk.accel.misw.mp.Main <turbineid> [server-ip]*");
		
		TurbineId turbineId = new TurbineId(args[0]);
		LOG.info("Starting node for " + turbineId);
		
		String tcConfig = TerracottaUtil.createConfigFile(Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
		
		ReconnectErrorHandler errorHandler = new ReconnectErrorHandler();
		final NodeSubscriptionService nodeSubscriptionService = NodeImplFactory.newNodeSubscriptionService(turbineId, tcConfig);
		NodeSubscriptionService exportedNodeSubscriptionService;
		try {
			exportedNodeSubscriptionService = (NodeSubscriptionService) UnicastRemoteObject.exportObject(nodeSubscriptionService, 0);
		} catch (RemoteException e) {
			// if this happens there is a code error in NodeSubscriptionService implementation so it can't be exported.
			throw new RuntimeException(e);
		}

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				LOG.info("Stopping server");
				try {
					nodeSubscriptionService.stop();
				} catch (RemoteException e) {
					// ignore
				} finally {
					MainUtil.deleteSignalFile(false);
				}
			}
		}));
		
		if (args.length < 2) {
			// broadcast - availability takeover does not work in this mode
			BroadcastClient broadcaster = NodeImplFactory.newBroadcaster(turbineId, nodeSubscriptionService, exportedNodeSubscriptionService, null);
			errorHandler.setBroadcaster("broadcast", broadcaster);
		
			// finally broadcast and start
			Future<?> future = broadcaster.connectInSeparateThread(true);
			// wait to connected
			future.get();
			MainUtil.deleteSignalFile(true);
		} else {
			List<Future<?>> futures = new ArrayList<Future<?>>();
			for (int i = 1, n = args.length; i < n; i++) {
				BroadcastClient broadcaster = NodeImplFactory.newBroadcaster(turbineId, nodeSubscriptionService, exportedNodeSubscriptionService, "//" + args[i] + "/server");
				errorHandler.setBroadcaster("server-" + (i - 1), broadcaster);
			
				// finally broadcast and start
				futures.add(broadcaster.connectInSeparateThread(true));
			}
			// construct to wait until we are connected to at least one server.
			// when this happens the non-daemon thread in the NodeSubscriptionServiceImpl
			// will kick in and keep the jvm alive
			MainUtil.deleteSignalFile(true);
			while (true) {
				Thread.sleep(1000L);
			}
		}
	}
	
	/**
	 * TODO - Terracotta don't give us any indications of dsm errors, 
	 * so we can use the reconnect error handler.
	 * 
	 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
	 */
	private static class ReconnectErrorHandler implements ErrorHandler {
		
		private final ConcurrentMap<String, BroadcastClient> broadcasterMap = new ConcurrentHashMap<String, BroadcastClient>();
		
		@Override
		public void onError(String serverId, Throwable t) {
			LOG.log(Level.WARNING, "Node callback error", t);
			BroadcastClient broadcaster = broadcasterMap.get(serverId != null ? serverId : "broadcast");
			if (broadcaster != null) {
				LOG.info("Reconnecting to " + serverId + " ...");
				broadcaster.connectInSeparateThread(true);
			}
		}
		
		private void setBroadcaster(String serverId, BroadcastClient broadcaster) {
			broadcasterMap.put(serverId, broadcaster);
		}
	}
}
