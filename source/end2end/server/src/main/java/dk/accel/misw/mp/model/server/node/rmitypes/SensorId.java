package dk.accel.misw.mp.model.server.node.rmitypes;

import java.io.Serializable;

import com.google.common.base.Preconditions;

/**
 * A sensor identifier - is in context of a specific turbine.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class SensorId implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final TurbineId turbine;
	private final String sensor;
	
	public SensorId(TurbineId turbine, String sensor) {
		this.turbine = Preconditions.checkNotNull(turbine);
		this.sensor = Preconditions.checkNotNull(sensor);
	}

	public TurbineId getTurbine() {
		return turbine;
	}

	public String getSensor() {
		return sensor;
	}

	@Override
	public String toString() {
		return "SensorId [sensor=" + sensor + ", turbine=" + turbine + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sensor == null) ? 0 : sensor.hashCode());
		result = prime * result + ((turbine == null) ? 0 : turbine.hashCode());
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
		SensorId other = (SensorId) obj;
		if (sensor == null) {
			if (other.sensor != null) {
				return false;
			}
		} else if (!sensor.equals(other.sensor)) {
			return false;
		}
		if (turbine == null) {
			if (other.turbine != null) {
				return false;
			}
		} else if (!turbine.equals(other.turbine)) {
			return false;
		}
		return true;
	}	
}
