package dk.accel.misw.mp.ec2;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;

import dk.accel.misw.mp.ec2.util.InstanceStateName2;

public class EC2Launcher implements Closeable {
	
	private static final Logger LOG = Logger.getLogger(EC2Launcher.class.getName());

	private final AmazonEC2 ec2;
	private final EC2Settings settings = EC2Settings.loadFromProperties(EC2Launcher.class);
	
	public EC2Launcher() throws IOException {
		this.ec2 = new AmazonEC2Client(EC2Launcher.getCredentials());
		this.ec2.setEndpoint(settings.getRegion());
	}
	
	@Override
	public void close() {
		ec2.shutdown();
	}
	
	static AWSCredentials getCredentials() throws IOException {
		return new PropertiesCredentials(EC2Launcher.class.getResourceAsStream("AwsCredentials.properties"));
	}
	
	public String createSecurityGroup() throws IOException {
		EC2SecurityGroup group = new EC2SecurityGroup(ec2);
		return group.createSecurityGroup(settings.getUserId());
	}
	
	public void deleteSecurityGroup(String groupName) throws IOException {
		EC2SecurityGroup group = new EC2SecurityGroup(ec2);
		group.deleteSecurityGroup(groupName);
	}
	
	public List<Instance> launch(int count, String groupName) throws IOException, InterruptedException {
		List<Instance> instances = startNodes(settings.getAmi(), groupName, count);
		LOG.info("Launched " + count + " nodes:");
		for (Instance instance : instances) {
			LOG.info("\t " + instance.getInstanceId() + " at " + instance.getPrivateIpAddress() + " / " + instance.getPublicIpAddress());
		}
		return instances;
	}
	
	public void terminate(List<String> instanceIds) throws IOException, InterruptedException {
		stopInstances(instanceIds);
		terminateInstances(instanceIds);
	}
	
	private List<Instance> startNodes(String imageId, String securityGroup, int count) throws InterruptedException {
		DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
		describeImagesRequest.withOwners("self").withImageIds(imageId);
		DescribeImagesResult describeImagesResult = ec2.describeImages(describeImagesRequest);
		if (describeImagesResult.getImages().size() != 1) {
			throw new AmazonClientException("No EC2 AMI with id '" + imageId + "'");
		}
		String uniqueImageId = describeImagesResult.getImages().get(0).getImageId();
		
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest(uniqueImageId, count, count);
		runInstancesRequest.withInstanceType(settings.getInstanceType()).withKeyName(settings.getSshKey());
		runInstancesRequest.withSecurityGroups(securityGroup);
		
		RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
		LOG.info("Launching " + count + " instances");
		List<Instance> result = waitForState(InstanceStateName2.Running, unwrapInstanceIds(unwrapReservations(runInstancesResult.getReservation())));
		LOG.info("Successfully launched instances");
		return result;
	}
	
	private void stopInstances(List<String> instanceIds) throws InterruptedException {
		ec2.stopInstances(new StopInstancesRequest(instanceIds));
		waitForState(InstanceStateName2.Stopped, instanceIds);
	}
	
	private void terminateInstances(List<String> instanceIds) throws InterruptedException {
		ec2.terminateInstances(new TerminateInstancesRequest(instanceIds));
		waitForState(InstanceStateName2.Terminated, instanceIds);
	}

	private List<Instance> unwrapReservations(List<Reservation> reservations) {
		return unwrapReservations(reservations.toArray(new Reservation[reservations.size()]));
	}
	
	private List<Instance> unwrapReservations(Reservation ... reservations) {
		List<Instance> result = new ArrayList<Instance>();
		for (Reservation reservation : reservations) {
			result.addAll(reservation.getInstances());
		}
		return result;
	}

	private List<String> unwrapInstanceIds(List<Instance> instances) {
		List<String> instanceIds = new ArrayList<String>(instances.size());
		for (Instance instance : instances) {
			instanceIds.add(instance.getInstanceId());
		}
		return instanceIds;
	}
	
	private List<Instance> waitForState(InstanceStateName2 state, List<String> instanceIds) throws InterruptedException {
		DescribeInstancesRequest req = new DescribeInstancesRequest().withInstanceIds(instanceIds);
		while (true) {
			Thread.sleep(5000L);
			// make a local copy, as we modify it below
			List<String> localInstanceIds = new ArrayList<String>(instanceIds);
			DescribeInstancesResult resp = ec2.describeInstances(req);
			for (Instance instance : unwrapReservations(resp.getReservations())) {
				String id = instance.getInstanceId();
				InstanceStateName2 actualState = InstanceStateName2.fromValue(instance.getState().getName());
				if (state.equals(actualState)) {
					if (!localInstanceIds.remove(id)) {
						LOG.warning("DescribeInstances returned an unexpected id '" + id + "'");
					}
				} else {
					LOG.info(id + " = " + actualState);
				}
			}
			if (localInstanceIds.isEmpty()) {
				return unwrapReservations(resp.getReservations());
			}
		}
	}
	
	private static class EC2Settings {
		private final String region;
		private final String ami;
		private final String instanceType;
		private final String userId;
		private final String sshKey;
		
		EC2Settings(String region, String ami, String instanceType, String userId, String sshKey) {
			this.region = Preconditions.checkNotNull(region);
			this.ami = Preconditions.checkNotNull(ami);
			this.instanceType = Preconditions.checkNotNull(instanceType);
			this.userId = Preconditions.checkNotNull(userId);
			this.sshKey = Preconditions.checkNotNull(sshKey);
		}

		String getRegion() {
			return region;
		}

		String getAmi() {
			return ami;
		}

		String getInstanceType() {
			return instanceType;
		}

		String getUserId() {
			return userId;
		}

		String getSshKey() {
			return sshKey;
		}

		static EC2Settings loadFromProperties(Properties settings, Properties credentials) {
			String region = settings.getProperty("region");
			String ami = settings.getProperty("ami");
			String instanceType = settings.getProperty("instance-type");
			String userId = credentials.getProperty("user-id");
			String sshKey = credentials.getProperty("ssh-key");
			return new EC2Settings(region, ami, instanceType, userId, sshKey);
		}
		
		static EC2Settings loadFromProperties(Class<?> cls) throws IOException {
			InputStream is = cls.getResourceAsStream("AwsSettings.properties");
			if (is == null) {
				throw new IOException("No AwsSettings.properties");
			}
			InputStream is2 = cls.getResourceAsStream("AwsCredentials.properties");
			if (is2 == null) {
				throw new IOException("No AwsCredentials.properties");
			}
			try {
				Properties settings = new Properties();
				settings.load(is);
				Properties credentials = new Properties();
				credentials.load(is2);
				return loadFromProperties(settings, credentials);
			} finally {
				Closeables.closeQuietly(is);
				Closeables.closeQuietly(is2);
			}
		}
	}
}
