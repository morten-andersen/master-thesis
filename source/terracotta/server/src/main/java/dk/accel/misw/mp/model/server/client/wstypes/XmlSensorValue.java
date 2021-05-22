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
	private long time;
	
	/**
	 * No args constructor necessary for JAXB
	 */
	public XmlSensorValue() {
	}
	
	public XmlSensorValue(XmlSensorId sensor, double value, long time) {
		this.sensor = sensor;
		this.value = value;
		this.time = time;
	}

	@XmlElement(required=true)
	public XmlSensorId getSensor() {
		return sensor;
	}

	@XmlElement(required=true)
	public double getValue() {
		return value;
	}

	@XmlElement(required=true)
	public long getTime() {
		return time;
	}
	
	public void setSensor(XmlSensorId sensor) {
		this.sensor = sensor;
	}
	
	public void setValue(double value) {
		this.value = value;
	}

	public void setTime(long time) {
		this.time = time;
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
		return "SensorValue [sensor=" + sensor + ", value=" + value + ", time=" + time + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sensor == null) ? 0 : sensor.hashCode());
		result = prime * result + (int) (time ^ (time >>> 32));
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
		if (time != other.time) {
			return false;
		}
		if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
			return false;
		}
		return true;
	}
	
	
}
