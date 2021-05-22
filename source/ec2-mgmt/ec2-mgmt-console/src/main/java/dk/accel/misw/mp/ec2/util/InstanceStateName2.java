package dk.accel.misw.mp.ec2.util;

import com.amazonaws.services.ec2.model.InstanceStateName;

/**
 * Copy of {@link InstanceStateName} with the missing "Stopped" state added.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public enum InstanceStateName2 {

	Pending(InstanceStateName.Pending.toString()),
	Running(InstanceStateName.Running.toString()),
	ShuttingDown(InstanceStateName.ShuttingDown.toString()),
	Terminated(InstanceStateName.Terminated.toString()),
	Stopping("stopping"),
	Stopped("stopped");

	private String value;

	private InstanceStateName2(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Use this in place of valueOf.
	 * 
	 * @param value
	 *            real value
	 * @return InstanceStateName corresponding to the value
	 */
	public static InstanceStateName2 fromValue(String value) {
		for (InstanceStateName2 isn : values()) {
			if (isn.value.equals(value)) {
				return isn;
			}
		}
		throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
	}

}
