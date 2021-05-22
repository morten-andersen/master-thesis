package dk.accel.misw.mp.ec2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.UserIdGroupPair;

class EC2SecurityGroup {

	private static final Logger LOG = Logger.getLogger(EC2SecurityGroup.class.getName());
	
	private static final String WHAT_IS_MY_IP = "http://automation.whatismyip.com/n09230945.asp";
	private static final int SSH_PORT = 22;
	private static final int WS_PORT = 8080;
	
	private final AmazonEC2 ec2;
	private final String ipAddress;
	
	EC2SecurityGroup(AmazonEC2 ec2) throws IOException {
		this.ec2 = ec2;
		this.ipAddress = EC2SecurityGroup.getIpAddress();
		LOG.info("Local IP = " + ipAddress);
	}
	
	String createSecurityGroup(String userId) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		String groupName = "it-vest-" + format.format(new Date());
		ec2.createSecurityGroup(new CreateSecurityGroupRequest(groupName, "it-vest security group"));
		
		
		ec2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest(groupName, createIpPermissions(groupName, userId)));
		LOG.info("Security group with name '" + groupName + "' set up");
		return groupName;
	}
	
	void deleteSecurityGroup(String groupName) {
		ec2.deleteSecurityGroup(new DeleteSecurityGroupRequest(groupName));
		LOG.info("Security group with name '" + groupName + "' deleted");
	}
	
	private List<IpPermission> createIpPermissions(String groupName, String userId) {
		UserIdGroupPair userIdGroupPair = new UserIdGroupPair().withGroupName(groupName).withUserId(userId);
		
		List<IpPermission> permissions = new ArrayList<IpPermission>();
		// ssh from controller machine
		permissions.add(new IpPermission().withIpProtocol("tcp").withFromPort(SSH_PORT).withToPort(SSH_PORT).withIpRanges(ipAddress + "/32"));
		// webservices call from controller machine
		permissions.add(new IpPermission().withIpProtocol("tcp").withFromPort(WS_PORT).withToPort(WS_PORT).withIpRanges(ipAddress + "/32"));
		// local group ping
		permissions.add(new IpPermission().withIpProtocol("icmp").withUserIdGroupPairs(userIdGroupPair).withFromPort(-1).withToPort(-1));
		// local group tcp
		permissions.add(new IpPermission().withIpProtocol("tcp").withUserIdGroupPairs(userIdGroupPair).withFromPort(1).withToPort(0xFFFF));
		// local group udp
		permissions.add(new IpPermission().withIpProtocol("udp").withUserIdGroupPairs(userIdGroupPair).withFromPort(1).withToPort(0xFFFF));
		return permissions;
	}
	
	
	private static String getIpAddress() throws IOException {
		HttpURLConnection url = (HttpURLConnection) (new URL(WHAT_IS_MY_IP).openConnection());
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.getInputStream()));
			try {
				return reader.readLine();
			} finally {
				reader.close();
			}
		} finally {
			url.disconnect();
		}
	}
}
