package dk.accel.misw.mp.model.server.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil;
import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil.ItemFactory;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorValue;
import dk.accel.misw.mp.model.server.node.rmitypes.Token;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineId;
import dk.accel.misw.mp.model.server.node.rmitypes.TurbineSubscriptionInfo;

class SubscriptionMultiplexerImpl implements SubscriptionMultiplexer {

	private final ConcurrentMap<Token, List<SensorProxy>> tokenMap = new ConcurrentHashMap<Token, List<SensorProxy>>();
	private final ConcurrentMap<TurbineId, TurbineSubscription> turbineSubscriptions = new ConcurrentHashMap<TurbineId, TurbineSubscription>();
	
	private final TurbineSubscriptionFactory turbineSubscriptionFactory;
	
	SubscriptionMultiplexerImpl(String serverId, String tcConfig) {
		this.turbineSubscriptionFactory = new TurbineSubscriptionFactory(serverId, tcConfig);
	}
	
	@Override
	public List<SensorValue> getChangedValues(Token token) {
		List<SensorProxy> sensorProxies = tokenMap.get(token);
		if (sensorProxies ==  null) {
			throw new IllegalArgumentException("No subscription for token " + token);
		}
		
		List<SensorValue> result = new ArrayList<SensorValue>(sensorProxies.size());
		for (SensorProxy proxy : sensorProxies) {
			SensorValue val = proxy.getValueIfChanged();
			if (val != null) {
				result.add(val);
			}
		}
		return result;
	}

	@Override
	public Token subscribe(List<SensorId> sensorIdList) {
		List<SensorProxy> sensorProxies = new ArrayList<SensorProxy>();
		
		// group by turbine - and iterate for each turbine group
		for (Map.Entry<TurbineId, List<SensorId>> entry : groupSensorIdsByTurbine(sensorIdList).entrySet()) {
			TurbineSubscription turbineSubscription = ConcurrentMapUtil.getItemCreateIfAbsent(entry.getKey(), turbineSubscriptions, turbineSubscriptionFactory);
			
			List<Sensor> sensorList = new ArrayList<Sensor>(entry.getValue().size());
			// then lookup all sensors, and wrap in sensor proxy objects.
			for (SensorId sid : entry.getValue()) {
				Sensor sensor = turbineSubscription.getSensor(sid);
				sensorList.add(sensor);
				sensorProxies.add(new SensorProxy(sensor));
			}
			
			turbineSubscription.addAll(sensorList);
		}
		
		while (true) {
			// loop to be sure that we don't have a token collision in tokenMap - although highly unlikely/impossible since we are using UUID's.
			Token token = Token.newInstance();
			if (tokenMap.putIfAbsent(token, sensorProxies) == null) {
				return token;
			}
		}
	}

	@Override
	public boolean unsubscribe(Token token) {
		List<SensorProxy> sensorProxies = tokenMap.remove(token);
		if (sensorProxies == null) {
			return false;
		}
		
		for (Map.Entry<TurbineId, List<Sensor>> entry : groupSensorsByTurbine(sensorProxies).entrySet()) {
			TurbineSubscription turbineSubscription = turbineSubscriptions.get(entry.getKey());
			turbineSubscription.removeAll(entry.getValue());
		}
		
		return true;
	}

	@Override
	public TurbineSubscriptionInfo getTurbineSubscriptionInfo(TurbineId turbine) throws InterruptedException {
		TurbineSubscription result = ConcurrentMapUtil.getItemCreateIfAbsent(turbine, turbineSubscriptions, turbineSubscriptionFactory);
		result.waitForChanged();
		return result.getSubscriptionInfo();
	}

	@Override
	public void markTurbineNodeDirty(TurbineId turbine) {
		TurbineSubscription turbineSubscription = ConcurrentMapUtil.getItemCreateIfAbsent(turbine, turbineSubscriptions, turbineSubscriptionFactory);
		turbineSubscription.markDirty();
	}
	
	private Map<TurbineId, List<SensorId>> groupSensorIdsByTurbine(List<SensorId> sensorIdList) {
		Map<TurbineId, List<SensorId>> result = new HashMap<TurbineId, List<SensorId>>();
		for (SensorId sid : sensorIdList) {
			List<SensorId> list = result.get(sid.getTurbine());
			if (list == null) {
				list = new ArrayList<SensorId>();
				result.put(sid.getTurbine(), list);
			}
			list.add(sid);
		}
		return result;
	}
	
	private Map<TurbineId, List<Sensor>> groupSensorsByTurbine(List<SensorProxy> sensorProxyList) {
		Map<TurbineId, List<Sensor>> result = new HashMap<TurbineId, List<Sensor>>();
		for (SensorProxy sp : sensorProxyList) {
			Sensor sensor = sp.getSensor();
			List<Sensor> list = result.get(sensor.getSensorId().getTurbine());
			if (list == null) {
				list = new ArrayList<Sensor>();
				result.put(sensor.getSensorId().getTurbine(), list);
			}
			list.add(sensor);
		}
		return result;
	}
	
	private static class TurbineSubscriptionFactory implements ItemFactory<TurbineId, TurbineSubscription> {
		private final String serverId;
		private final String tcConfig;
		
		private TurbineSubscriptionFactory(String serverId, String tcConfig) {
			this.serverId = serverId;
			this.tcConfig = tcConfig;
		}
		
		@Override
		public TurbineSubscription newInstance(TurbineId key) {
			return new TurbineSubscription(serverId, key, tcConfig);
		}
	}
}
