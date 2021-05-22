package dk.accel.misw.mp.model.server.node.rmitypes;

import java.io.Serializable;

import dk.accel.misw.mp.model.common.util.StdUtil;

/**
 * One sensor reading. As the supported datatypes doesn't really matter
 * for the project, only double values are supported.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class SensorValue implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final SensorId sensor;
	private final double value;
	
	public SensorValue(SensorId sensor, double value) {
		this.sensor = StdUtil.checkForNull(sensor);
		this.value = StdUtil.checkForNull(value);
	}

	public SensorId getSensor() {
		return sensor;
	}

	public double getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "SensorValue [sensor=" + sensor + ", value=" + value + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sensor == null) ? 0 : sensor.hashCode());
		long temp;
		temp = Double.doubleToLongBits(value);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SensorValue other = (SensorValue) obj;
		if (sensor == null) {
			if (other.sensor != null) {
				return false;
			}
		} else if (!sensor.equals(other.sensor)) {
			return false;
		}
		if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
			return false;
		}
		return true;
	}
}
