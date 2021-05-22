package dk.accel.misw.mp.model.server.client.wstypes;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * One sensor reading. As the supported datatypes doesn't really matter
 * for the project, only double values are supported.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
@XmlType(name="SensorValue", namespace=XmlConstants.XSD_NAMESPACE)
public class XmlSensorValue {
	
	private XmlSensorId sensor;
	private double value;
	
	/**
	 * No args constructor necessary for JAXB
	 */
	public XmlSensorValue() {
	}
	
	public XmlSensorValue(XmlSensorId sensor, double value) {
		this.sensor = sensor;
		this.value = value;
	}

	@XmlElement(required=true)
	public XmlSensorId getSensor() {
		return sensor;
	}

	@XmlElement(required=true)
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
		XmlSensorValue other = (XmlSensorValue) obj;
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
