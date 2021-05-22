package dk.accel.misw.mp.model.node.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import dk.accel.misw.mp.model.common.register.RegisterProtocol;
import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.common.util.StdUtil;
import dk.accel.misw.mp.model.node.BroadcastClient;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.TurbineSubscriptionServiceManager;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;

class BroadcastClientImpl implements BroadcastClient {

	private static final Logger LOG = Logger.getLogger(BroadcastClientImpl.class.getName());
	
	private final SocketAddress broadcastAddress = RegisterProtocol.getBroadCastAddress();
	private final int broadcastSoTimeout = Integer.getInteger(BroadcastClientImpl.class.getName() + ".timeout", 10 * 1000);
	
	private final ScheduledExecutorService aliveTaskExecutor = Executors.newSingleThreadScheduledExecutor(ExecutorsUtil.newDaemonThreadFactory("alive-task", Executors.defaultThreadFactory()));
	private final AtomicReference<ScheduledFuture<?>> aliveTask = new AtomicReference<ScheduledFuture<?>>();
	
	private final TurbineId turbineId;
	private final NodeSubscriptionService nodeSubscriptionService;
	
	public BroadcastClientImpl(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService) {
		this.turbineId = StdUtil.checkForNull(turbineId);
		try {
			this.nodeSubscriptionService = (NodeSubscriptionService) UnicastRemoteObject.exportObject(StdUtil.checkForNull(nodeSubscriptionService), 0);
		} catch (RemoteException e) {
			// if this happens there is a code error in NodeSubscriptionService implementation so it can't be exported.
			throw new RuntimeException(e);
		}
	}

	@Override
	public void connect(boolean retry) {
		do {
			ScheduledFuture<?> f = aliveTask.getAndSet(null);
			if (f != null) {
				f.cancel(true);
			}
			
			LOG.info("Connecting...");
			try {
				AliveTask task = broadcast();
				f = aliveTaskExecutor.scheduleWithFixedDelay(ExecutorsUtil.wrap(task), 1, 1, TimeUnit.SECONDS);
				if (!aliveTask.compareAndSet(null, f)) {
					f.cancel(true);
					throw new IllegalStateException("concurrent connect attempts");
				}
				LOG.info("Connected");
				return;
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Connect error", e);
			} catch (NotBoundException e) {
				LOG.log(Level.WARNING, "Connect error (naming)", e);
			}
		} while (retry);
	}
	
	private AliveTask broadcast() throws IOException, NotBoundException {
		DatagramSocket socket = new DatagramSocket();
		socket.setSoTimeout(broadcastSoTimeout);
		socket.setBroadcast(true);
		
		// broadcast the init message
		sendInitMessage(socket);
		
		// wait for an ack
		String destUrl = receiveAckMessage(socket);
		return register(destUrl);
	}
	
	private void sendInitMessage(DatagramSocket socket) throws IOException {
		byte[] msg = RegisterProtocol.initMessage();
		DatagramPacket packet = new DatagramPacket(msg, msg.length, broadcastAddress);
		socket.send(packet);
	}
	
	private String receiveAckMessage(DatagramSocket socket) throws IOException {
		byte[] buf = new byte[64 * 1024];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		byte[] srvAck = RegisterProtocol.serverAck();
		if (packet.getLength() <= RegisterProtocol.ackLength()) {
			// garbage - wait for another ack
			return receiveAckMessage(socket);
		}
		
		for (int i = 0; i < RegisterProtocol.ackLength(); i++) {
			if (srvAck[i] != buf[packet.getOffset() + i]) {
				// garbage - wait for another ack
				return receiveAckMessage(socket);
			}
		}
		// if we get here we have an ack and extract the RMI string for the server/responder
		return new String(buf, packet.getOffset() + RegisterProtocol.ackLength(), packet.getLength() - RegisterProtocol.ackLength(), RegisterProtocol.UTF8);
	}
	
	private AliveTask register(String destUrl) throws MalformedURLException, RemoteException, NotBoundException {
		TurbineSubscriptionServiceManager manager = (TurbineSubscriptionServiceManager) Naming.lookup(destUrl);
		manager.registerNode(turbineId, nodeSubscriptionService);
		return new AliveTask(manager);
	}
	
	private class AliveTask implements Runnable {

		private final TurbineSubscriptionServiceManager manager;
		
		private AliveTask(TurbineSubscriptionServiceManager manager) {
			this.manager = StdUtil.checkForNull(manager);
		}
		
		@Override
		public void run() {
			try {
				if (!manager.isRegistered(BroadcastClientImpl.this.turbineId, BroadcastClientImpl.this.nodeSubscriptionService)) {
					BroadcastClientImpl.this.connect(true);
				}
			} catch (RemoteException e) {
				BroadcastClientImpl.this.connect(true);
			}
		}
	}
}
