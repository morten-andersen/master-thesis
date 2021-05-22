package dk.accel.misw.mp.ec2.ctrl;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

/**
 * Simple remotely accessible controller that can:
 * <ol>
 * <li>Fetch a zip archive with a java program from the Amazon S3 storage.</li>
 * <li>Launch the java program as a child process</li>
 * <li>Stop the child process again</li>
 * </ol>
 *  
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
@WebService(name=EC2ControllerSvc.WS_SERVICE_NAME, targetNamespace=EC2ControllerSvc.WS_NAMESPACE)
public interface EC2ControllerSvc {

	static final String BASE_URL = System.getProperty(EC2ControllerSvc.class.getName() + ".baseUrl", "http://0.0.0.0:8080/");

	static final String WS_NAMESPACE = "dk.accel.misw.mp.ec2.ws";
	static final String XSD_NAMESPACE = "dk.accel.misw.mp.ec2.xsd";
	
	static final String WS_SERVICE_NAME = "EC2Controller";
	static final String FULL_NAME = "dk.accel.misw.mp.ec2.ctrl.EC2ControllerSvc";

	/**
	 * Download the java program from Amazon S3.
	 */
	void init(String s3key, Environment env) throws IOException;

	/**
	 * Probe the network to the hosts in the ipAddresses parameter.
	 * @throws IOException 
	 */
	NodeNetworkInfoList probeNetwork(List<String> ipAddresses) throws IOException;
	
	/**
	 * Start the downloaded java program.
	 */
	void start() throws IOException;
	
	/**
	 * Stop the download java program.
	 */
	void stop() throws IOException;
	
	/**
	 * Zip the log folder and upload it to S3.
	 * 
	 * @return the s3key for the data.
	 */
	String uploadData(String s3prefix) throws IOException;
	
	/**
	 * Simple wrapper class to help JAXB marshal a map.
	 * See http://jaxb.java.net/guide/Mapping_your_favorite_class.html
	 */
	public static class Environment {
		public HashMap<String, String> env;
		
		public Environment() {
			// JAXB required
		}
		
		public Environment(HashMap<String, String> env) {
			this.env = env;
		}
	}
	
	public static class NodeNetworkInfoList {
		public List<NodeNetworkInfo> list;
		
		public NodeNetworkInfoList() {
			// JAXB required
		}
		
		public NodeNetworkInfoList(List<NodeNetworkInfo> list) {
			this.list = list;
		}
	}
	
	public static class NodeNetworkInfo {
		
		public static enum ResultType { hops, rtt }
		
		private static final NumberFormat RTT_FORMATTER = newNumberFormatter();
		
		private static NumberFormat newNumberFormatter() {
			NumberFormat result = NumberFormat.getNumberInstance();
			result.setParseIntegerOnly(false);
			result.setMinimumFractionDigits(3);
			result.setMaximumFractionDigits(3);
			result.setMinimumIntegerDigits(1);
			return result;
		}
		
		/**
		 * The target ip
		 */
		public String ip;

		/**
		 * The ip of the dom 0 node (i.e. the first node from localhost).
		 */
		public String dom0ip;

		/**
		 * The number of hops to the target ip.
		 */
		public int hops;
		
		/**
		 * Avg round trip time in ms.
		 */
		public double rtt;

		public NodeNetworkInfo() {
			// JAXB required
		}
		
		public NodeNetworkInfo(String ip, String dom0ip, int hops, double rtt) {
			this.ip = ip;
			this.dom0ip = dom0ip;
			this.hops = hops;
			this.rtt = rtt;
		}

		@Override
		public String toString() {
			return "NodeNetworkInfo [ip=" + ip + ", dom0ip=" + dom0ip + ", hops=" + hops + ", rtt=" + RTT_FORMATTER.format(rtt) + "]";
		}
		
		public String toString(ResultType resultType) {
			switch (resultType) {
			case hops:
				return Integer.toString(hops);
			case rtt:
				return RTT_FORMATTER.format(rtt);
			default:
				throw new IllegalArgumentException("Unknown result type " + resultType);
			}
		}
	}
	
	public static class Factory {
		private static final Logger LOG = Logger.getLogger(EC2ControllerSvc.class.getName());
		
		public static EC2ControllerSvc newClient(String host) throws IOException, InterruptedException {
			for (int i = 0; i < 10; i++) {
				try {
					Service service = Service.create(new URL("http://" + host + "/" + WS_SERVICE_NAME + "?wsdl"), new QName(WS_NAMESPACE, WS_SERVICE_NAME + "Service"));
					return service.getPort(EC2ControllerSvc.class);
				} catch (WebServiceException e) {
					if (i >= 9) {
						throw e;
					} else {
						LOG.warning("Unable to connect to " + host + " (" + e.getMessage() + "), retrying in 5 seconds");
						Thread.sleep(5000L);
					}
				}
			}
			// we shouldn't get down here
			throw new AssertionError();
		}
	}

}
