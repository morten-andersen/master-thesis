package dk.accel.misw.mp.model.server.client.wstypes;

public class XmlConstants {

	public static final String BASE_URL = System.getProperty(XmlConstants.class.getName() + ".baseUrl", "http://localhost:8080/");

	public static final String WS_NAMESPACE = "dk.accel.misw.mp.ws";
	public static final String XSD_NAMESPACE = "dk.accel.misw.mp.xsd";
	
	private XmlConstants() {
		throw new AssertionError();
	}
}
