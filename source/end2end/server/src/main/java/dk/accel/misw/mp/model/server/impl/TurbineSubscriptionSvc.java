package dk.accel.misw.mp.model.server.impl;

import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;

import dk.accel.misw.mp.model.common.util.ExecutorsUtil;
import dk.accel.misw.mp.model.server.node.NodeSubscriptionService;
import dk.accel.misw.mp.model.server.node.TurbineObserverSvc;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

class TurbineSubscriptionSvc implements Runnable {

	private final TurbineId turbineId;
	private final NodeSubscriptionService nodeSubscriptionService;
	private final TurbineObserverSvcImpl observer;
	private final SubscriptionMultiplexer subscriptionMultiplexer;
	
	private final AtomicReference<ScheduledFuture<?>> future = new AtomicReference<ScheduledFuture<?>>();
	
	TurbineSubscriptionSvc(TurbineId turbineId, NodeSubscriptionService nodeSubscriptionService,
			TurbineObserverSvcImpl observer, SubscriptionMultiplexer subscriptionMultiplexer) {
		this.turbineId = Preconditions.checkNotNull(turbineId);
		this.nodeSubscriptionService = Preconditions.checkNotNull(nodeSubscriptionService);
		this.observer = Preconditions.checkNotNull(observer);
		this.subscriptionMultiplexer = Preconditions.checkNotNull(subscriptionMultiplexer);
		this.subscriptionMultiplexer.markTurbineNodeDirty(turbineId);
	}

	TurbineObserverSvcImpl getObserver() {
		return observer;
	}

	NodeSubscriptionService getNodeSubscriptionService() {
		return nodeSubscriptionService;
	}

	@Override
	public void run() {
		try {
			TurbineSubscriptionInfo subscriptionInfo = subscriptionMultiplexer.getTurbineSubscriptionInfo(turbineId);
			TurbineObserverSvc removeObserver = observer.getRemote();
			nodeSubscriptionService.updateSubscription(subscriptionInfo, removeObserver);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ConnectException e) {
			throw new RuntimeException("unable to connect to " + turbineId, e);
		} catch (ConnectIOException e) {
			throw new RuntimeException("io-exception during connect to " + turbineId, e);
		} catch (RemoteException e) {
			throw unwrap(e);
		}
	}
	
	RuntimeException unwrap(RemoteException e) {
		Throwable t = e.getCause();
		if (t == null) {
			return new RuntimeException(e);
		} else {
			if (t instanceof Error) {
				throw (Error) t;
			} else if (t instanceof RuntimeException) {
				return (RuntimeException) t;
			} else {
				return new RuntimeException(t);
			}
		}
	}
	
	void start(ScheduledExecutorService executor) {
		if (future.get() != null) {
			throw new IllegalStateException("already started");
		}
		ScheduledFuture<?> f = executor.scheduleWithFixedDelay(ExecutorsUtil.wrap(this), 100L, 1L, TimeUnit.MILLISECONDS);
		if (!future.compareAndSet(null, f)) {
			// we got raced - stop the new task
			f.cancel(true);
		}
	}
	
	void stop() {
		ScheduledFuture<?> f = future.getAndSet(null);
		if (f == null) {
			// already stopped - just ignore
			return;
		}
		f.cancel(true);
	}
}
