package dk.accel.misw.mp.model.node.mock;

import dk.accel.misw.mp.model.common.util.ConcurrentMapUtil.ItemFactory;
import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;

public class SensorMockFactory {

	public static final ItemFactory<SensorId, SensorMock> SENSOR_RANDOM_MOCK_FACTORY = new SensorRandomMock.SensorRandomMockFactory();
	
	public static final ItemFactory<SensorId, SensorMock> SENSOR_STANDARD_MOCK_FACTORY = new SensorMockImpl.SensorRandomMockFactory();
	
	private SensorMockFactory() {
		throw new AssertionError();
	}
}
