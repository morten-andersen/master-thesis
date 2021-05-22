package dk.accel.misw.mp.model.server.node.rmitypes;

import java.io.Serializable;

import com.google.common.base.Preconditions;

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
	
	/**
	 * The time when the sensor value was read.
	 */
	private final long time;
	
	public SensorValue(SensorId sensor, double value) {
		this.sensor = Preconditions.checkNotNull(sensor);
		this.value = Preconditions.checkNotNull(value);
		this.time = System.currentTimeMillis();
	}

	public SensorId getSensor() {
		return sensor;
	}

	public double getValue() {
		return value;
	}
	
	public long getTime() {
		return time;
	}
	
	/**
	 * @return the age in milliseconds since the value
	 * was read. This age can only be used in a distributed
	 * system if the nodes are time-synchronised.
	 */
	public long getAge() {
		return System.currentTimeMillis() - time;
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
