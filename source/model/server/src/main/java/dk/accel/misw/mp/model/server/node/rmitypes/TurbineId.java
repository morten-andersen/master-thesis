package dk.accel.misw.mp.model.server.node.rmitypes;

import java.io.Serializable;

import dk.accel.misw.mp.model.common.util.StdUtil;

/**
 * A turbine identifier.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class TurbineId implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final String turbine;
	
	public TurbineId(String turbine) {
		this.turbine = StdUtil.checkForNull(turbine);
	}

	public String getTurbine() {
		return turbine;
	}

	@Override
	public String toString() {
		return "Turbine [turbine=" + turbine + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		TurbineId other = (TurbineId) obj;
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
