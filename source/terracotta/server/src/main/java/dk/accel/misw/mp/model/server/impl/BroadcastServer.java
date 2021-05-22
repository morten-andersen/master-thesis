package dk.accel.misw.mp.model.server.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import dk.accel.misw.mp.model.common.register.RegisterProtocol;
import dk.accel.misw.mp.model.common.util.ExecutorsUtil;

class BroadcastServer implements Runnable {

	private static final Logger LOG = Logger.getLogger(BroadcastServer.class.getName());
	
	private final SocketAddress serverAddress = RegisterProtocol.getBroadCastAddress();
	
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(ExecutorsUtil.newNamedThreadFactory("broadcast-server", Executors.defaultThreadFactory()));
	private final AtomicReference<Future<?>> serverFuture = new AtomicReference<Future<?>>();
	private final String rmiUrl;
	
	BroadcastServer() {
		this.rmiUrl = "//" + getLocalHostAddress() + "/server";
	}
	
	void start() {
		Future<?> f = executor.submit(this);
		if (!serverFuture.compareAndSet(null, f)) {
			f.cancel(true);
			throw new IllegalStateException("already started");
		}
	}
	
	void stop() {
		Future<?> f = serverFuture.getAndSet(null);
		if (f != null) {
			f.cancel(true);
		}
	}
	
	@Override
	public void run() {
		try {
			DatagramChannel serverChannel = DatagramChannel.open();
			Selector selector = Selector.open();
			ByteBuffer buffer = ByteBuffer.allocate(RegisterProtocol.initLength());
			byte[] inMsg = new byte[RegisterProtocol.initLength()];
			byte[] ackMsg = RegisterProtocol.serverAck(rmiUrl);

			serverChannel.socket().bind(serverAddress);
			serverChannel.socket().setBroadcast(true);
			serverChannel.configureBlocking (false);
			serverChannel.register(selector, SelectionKey.OP_READ);
			
			while (!Thread.currentThread().isInterrupted()) {
				selector.select();
				
				for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
					SelectionKey key = it.next();
					it.remove();
					try {
						if (key.isReadable()) {
							buffer.clear();
							SocketAddress clientAddr = serverChannel.receive(buffer);
							if (clientAddr == null) {
								LOG.warning("no client address");
								continue;
								
							}
							
							if (buffer.remaining() != 0) {
								LOG.warning("not an init message (len err) - skipping msg from " + clientAddr);
								continue;
							}
							buffer.flip();
							buffer.get(inMsg);
							if (!Arrays.equals(inMsg, RegisterProtocol.initMessage())) {
								LOG.warning("not an init message - skipping msg from " + clientAddr);
								continue;
							}
							
							LOG.info("Init message received from " + clientAddr);
							serverChannel.send(ByteBuffer.wrap(ackMsg), clientAddr);
						}
					} catch (CancelledKeyException e) {
						LOG.log(Level.WARNING, "Key (" + key + ") was canceled, ignored", e);
					}
				}
			}
			LOG.info("Terminating broadcast server");
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Server error", e);
		} catch (Throwable e) {
			LOG.log(Level.WARNING, "Server error", e);
		}
	}

	public static String getLocalHostAddress() {
		try {
			return System.getProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		
	}
}
