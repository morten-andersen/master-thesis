package dk.accel.misw.mp.model.server.client.wstypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import dk.accel.misw.mp.model.server.node.rmitypes.SensorId;

/**
 * A sensor identifier - is in context of a specific turbine.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
@XmlType(name="SensorId", namespace=XmlConstants.XSD_NAMESPACE)
public class XmlSensorId {
	
	private String turbine;
	private String sensor;

	/**
	 * No args constructor necessary for JAXB
	 */
	public XmlSensorId() {
	}
	
	public XmlSensorId(String turbine, String sensor) {
		this.turbine = turbine;
		this.sensor = sensor;
	}

	public XmlSensorId(SensorId sensorId) {
		this.turbine = sensorId.getTurbine().getTurbine();
		this.sensor = sensorId.getSensor();
	}

	@XmlElement(required=true)
	public String getTurbine() {
		return turbine;
	}

	@XmlElement(required=true)
	public String getSensor() {
		return sensor;
	}

	public void setTurbine(String turbine) {
		this.turbine = turbine;
	}

	public void setSensor(String sensor) {
		this.sensor = sensor;
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
		XmlSensorId other = (XmlSensorId) obj;
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
