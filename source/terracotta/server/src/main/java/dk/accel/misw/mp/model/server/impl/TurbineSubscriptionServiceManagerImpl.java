package dk.accel.misw.mp.model.server.impl;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.server.TurbineSubscriptionServiceManagerAdmin;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.TurbineSubscriptionServiceManager;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

class TurbineSubscriptionServiceManagerImpl implements TurbineSubscriptionServiceManager, TurbineSubscriptionServiceManagerAdmin {

	private static final Logger LOG = Logger.getLogger(TurbineSubscriptionServiceManagerImpl.class.getName());
	
	private final SubscriptionMultiplexer subscriptionMultiplexer;
	private final BroadcastServer broadcastServer;
	private final ConcurrentMap<TurbineId, TurbineSubscriptionSvc> turbineSubscriptionServiceMap = new ConcurrentHashMap<TurbineId, TurbineSubscriptionSvc>();
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(32, ExecutorsUtil.newNamedThreadFactory("TurbineSubscriptionService", Executors.defaultThreadFactory()));
	
	TurbineSubscriptionServiceManagerImpl(SubscriptionMultiplexer subscriptionMultiplexer, BroadcastServer broadcastServer) {
		this.subscriptionMultiplexer = Preconditions.checkNotNull(subscriptionMultiplexer);
		this.broadcastServer = Preconditions.checkNotNull(broadcastServer);
	}

	@Override
	public void registerNode(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService) throws RemoteException {
		TurbineSubscriptionSvc oldService = turbineSubscriptionServiceMap.remove(turbineId);
		if (oldService != null) {
			LOG.info("Unregistered node for " + turbineId);
			oldService.stop();
		}
		TurbineSubscriptionSvc newService = new TurbineSubscriptionSvc(turbineId, nodeSubscriptionService, subscriptionMultiplexer);
		if (turbineSubscriptionServiceMap.putIfAbsent(turbineId, newService) != null) {
			throw new IllegalStateException("concurrent registrations for " + turbineId);
		}
		newService.start(executor);
		LOG.info("Registered node for " + turbineId);
	}

	@Override
	public void unRegisterNode(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService) {
		TurbineSubscriptionSvc oldService = turbineSubscriptionServiceMap.get(turbineId);
		if (oldService.getNodeSubscriptionService().equals(nodeSubscriptionService)) {
			// we have hit and tries to remove it
			if (turbineSubscriptionServiceMap.remove(turbineId, oldService)) {
				LOG.info("Unregistered node for " + turbineId);
				oldService.stop();
			}
		}
	}
	
	@Override
	public void start() {
		try {
			LOG.info("Starting RMI registry - localhost = " + System.getProperty("java.rmi.server.hostname"));
			LocateRegistry.createRegistry(1099);
			LocateRegistry.getRegistry("127.0.0.1").bind("server", UnicastRemoteObject.exportObject(this, 0));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		broadcastServer.start();
	}
	
	@Override
	public void stop() {
		broadcastServer.stop();
		for (Iterator<Map.Entry<TurbineId, TurbineSubscriptionSvc>> it = turbineSubscriptionServiceMap.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<TurbineId, TurbineSubscriptionSvc> entry = it.next();
			entry.getValue().stop();
			it.remove();
		}
		try {
			LocateRegistry.getRegistry().unbind("server");
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Unable to unbind", e);
		}
	}

	@Override
	public boolean isRegistered(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService) {
		TurbineSubscriptionSvc service = turbineSubscriptionServiceMap.get(turbineId);
		return service.getNodeSubscriptionService().equals(nodeSubscriptionService);
		
	}
}
