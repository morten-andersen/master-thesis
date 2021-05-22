package dk.accel.misw.mp.model.server.node.rmitypes;

import java.io.Serializable;
import java.util.List;

import dk.accel.misw.mp.model.common.util.StdUtil;

public class TurbineSubscriptionInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int generation;
	private final List<SensorId> sensorIdList;

	public TurbineSubscriptionInfo(int generation, List<SensorId> sensorIdList) {
		this.generation = generation;
		this.sensorIdList = StdUtil.checkForNull(sensorIdList);
	}

	public int getGeneration() {
		return generation;
	}

	public List<SensorId> getSensorIdList() {
		return sensorIdList;
	}
}
