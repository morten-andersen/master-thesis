package dk.accel.misw.mp.model.server.node.rmitypes;

import java.io.Serializable;
import java.util.UUID;

import com.google.common.base.Preconditions;

/**
 * A client subscription token.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class Token implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final UUID token;
	
	public Token(UUID token) {
		this.token = Preconditions.checkNotNull(token);
	}

	public UUID getToken() {
		return token;
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
		Token other = (Token) obj;
		if (token == null) {
			if (other.token != null) {
				return false;
			}
		} else if (!token.equals(other.token)) {
			return false;
		}
		return true;
	}
	
	public static Token newInstance() {
		return new Token(UUID.randomUUID());
	}
}
