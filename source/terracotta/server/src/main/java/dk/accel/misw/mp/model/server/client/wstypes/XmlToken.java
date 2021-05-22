package dk.accel.misw.mp.model.server.client.wstypes;

import java.util.UUID;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A client subscription token.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
@XmlType(name="Token", namespace=XmlConstants.XSD_NAMESPACE)
public class XmlToken {
	
	private UUID token;
	
	/**
	 * No args constructor necessary for JAXB
	 */
	public XmlToken() {
	}
	
	public XmlToken(UUID token) {
		this.token = token;
	}
	
	@XmlElement(required=true)
	public UUID getToken() {
		return token;
	}

	public void setToken(UUID token) {
		this.token = token;
	}

	@Override
	public String toString() {
		return "Token [token=" + token + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((token == null) ? 0 : token.hashCode());
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
		XmlToken other = (XmlToken) obj;
		if (token == null) {
			if (other.token != null) {
				return false;
			}
		} else if (!token.equals(other.token)) {
			return false;
		}
		return true;
	}
}
