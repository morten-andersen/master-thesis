package dk.accel.misw.mp.model.server.node.rmitypes;

import java.io.Serializable;
import java.util.List;

import com.google.common.base.Preconditions;

public class TurbineSubscriptionInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String serverId;
	private final int generation;
	private final List<SensorId> sensorIdList;

	public TurbineSubscriptionInfo(String serverId, int generation, List<SensorId> sensorIdList) {
		this.serverId = Preconditions.checkNotNull(serverId);
		this.generation = generation;
		this.sensorIdList = Preconditions.checkNotNull(sensorIdList);
	}

	public String getServerId() {
		return serverId;
	}
	
	public int getGeneration() {
		return generation;
	}

	public List<SensorId> getSensorIdList() {
		return sensorIdList;
	}
}
