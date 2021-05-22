package dk.accel.misw.mp.model.common.register;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class RegisterProtocol {

	public static final Charset UTF8 = Charset.forName("UTF-8");
	
	private static final byte[] INIT_MESSAGE = "init".getBytes(UTF8);
	private static final byte[] SERVER_ACK = "server_at:".getBytes(UTF8);
	
	public static final int initLength() {
		return INIT_MESSAGE.length;
	}
	
	public static final int ackLength() {
		return SERVER_ACK.length;
	}
	
	public static byte[] initMessage() {
		return INIT_MESSAGE.clone();
	}
	
	public static byte[] serverAck() {
		return SERVER_ACK.clone();
	}
	
	public static byte[] serverAck(String rmiUrl) {
		return ("server_at:" + rmiUrl).getBytes(UTF8);
	}

	public static SocketAddress getBroadCastAddress() {
		int port = Integer.getInteger(RegisterProtocol.class.getName() + ".broadcastPort", 8787);
		try {
			// 224.0.0.1 is local segment multicast
			return new InetSocketAddress(InetAddress.getByName("224.0.0.1"), port);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private RegisterProtocol() {
		throw new AssertionError();
	}
}
