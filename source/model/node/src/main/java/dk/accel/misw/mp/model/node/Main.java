package dk.accel.misw.mp.model.node;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	}
	
	private static final Logger LOG = Logger.getLogger(Main.class.getName());
	
	public static void main(String[] args) {
		if (args.length > 1) {
			printUsage();
		}
		
		// if no turbine is specified we generate a random one
		TurbineId turbineId = (args.length == 0 ? new TurbineId(Integer.toString(new Random().nextInt(10000))) : new TurbineId(args[0]));
		LOG.info("Starting node for " + turbineId);
		ReconnectErrorHandler errorHandler = new ReconnectErrorHandler();
		NodeSubscriptionService nodeSubscriptionService = NodeImplFactory.newNodeSubscriptionService(turbineId, errorHandler);
		BroadcastClient broadcaster = NodeImplFactory.newBroadcaster(turbineId, nodeSubscriptionService);
		errorHandler.setBroadcaster(broadcaster);
		
		// finally broadcast and start
		broadcaster.connect(true);
	}
	
	private static void printUsage() {
		System.err.println("Usage: java dk.accel.misw.mp.model.node.Main [turbineid]");
		System.exit(1);
	}

	private static class ReconnectErrorHandler implements ErrorHandler {
		
		private final AtomicReference<BroadcastClient> broadcasterRef = new AtomicReference<BroadcastClient>();
		
		@Override
		public void onError(Throwable t) {
			LOG.log(Level.WARNING, "Node callback error", t);
			BroadcastClient broadcaster = broadcasterRef.get();
			if (broadcaster != null) {
				LOG.info("Reconnecting...");
				broadcaster.connect(true);
			}
		}
		
		private void setBroadcaster(BroadcastClient broadcaster) {
			if (!broadcasterRef.compareAndSet(null, broadcaster)) {
				throw new IllegalStateException("already initialized");
			}
		}
	}

}
