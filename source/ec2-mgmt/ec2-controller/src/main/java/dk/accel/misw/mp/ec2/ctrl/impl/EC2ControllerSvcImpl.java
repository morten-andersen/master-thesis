package dk.accel.misw.mp.ec2.ctrl.impl;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.jws.WebService;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

import dk.accel.misw.mp.ec2.ctrl.EC2ControllerSvc;
import dk.accel.misw.mp.ec2.util.IOUtils;
import dk.accel.misw.mp.ec2.util.ProcessUtils;

@WebService(endpointInterface = EC2ControllerSvc.FULL_NAME, name = EC2ControllerSvc.WS_SERVICE_NAME, portName = EC2ControllerSvc.WS_SERVICE_NAME
		+ "Port", serviceName = EC2ControllerSvc.WS_SERVICE_NAME + "Service", targetNamespace = EC2ControllerSvc.WS_NAMESPACE)
public class EC2ControllerSvcImpl implements EC2ControllerSvc {

	private static final Logger LOG = Logger.getLogger(EC2ControllerSvcImpl.class.getName());
	
	private static final String S3_BUCKET = "dk.accel.itvest";
	
	private final File workingDir;
	
	private final AtomicReference<HashMap<String, String>> environment = new AtomicReference<HashMap<String, String>>();
	private final AtomicReference<Process> childProcess = new AtomicReference<Process>();
	
	EC2ControllerSvcImpl() throws IOException {
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
		
		this.workingDir = new File("workingdir", sdf.format(new Date())).getCanonicalFile();
		if (workingDir.exists()) {
			throw new IOException("it-vest test dir already exists (" + workingDir + ")");
		}
		IOUtils.mkdirs(workingDir);
		workingDir.deleteOnExit();
	}
	
	@Override
	public void init(String s3key, EC2ControllerSvc.Environment env) throws IOException {
		LOG.info("init called " + s3key);
		AmazonS3 s3 = newAmazonS3();

		if (workingDir.exists()) {
			IOUtils.deleteDirectory(workingDir);
		}
		this.environment.set(env.env);
		IOUtils.mkdirs(workingDir);
		File zip = new File(workingDir, s3key);
		ObjectMetadata result = s3.getObject(new GetObjectRequest(S3_BUCKET, s3key), zip);
		LOG.info("Retrieved file '" + zip + "' (len: " + result.getContentLength() + ", type: " + result.getContentType() + ")");
		IOUtils.unzip(zip, workingDir);
		IOUtils.mkdirs(new File(workingDir, "logs"));
	}

	@Override
	public NodeNetworkInfoList probeNetwork(List<String> ipAddresses) throws IOException {
		LOG.info("probing network to " + ipAddresses);
		try {
			return new NodeNetworkInfoList(NetworkProber.probe(ipAddresses));
		} catch (InterruptedException e) {
			throw new IOException("interrupted", e);
		}
	}

	@Override
	public void start() throws IOException {
		if (childProcess.get() != null) {
			LOG.info("child process already running");
			return;
		}
		LOG.info("starting child process ..");
		File startFile = new File(new File(System.getProperty("java.io.tmpdir")), ".it-vest-starting").getCanonicalFile();
		LOG.info("created signal temp file: '" + startFile + ", success = " + startFile.createNewFile());
		ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "launch.jar").directory(workingDir).redirectErrorStream(false);
		processBuilder.environment().putAll(environment.get());
		Process process = processBuilder.start();
		ProcessUtils.processStreams(process, System.out, System.err);
		if (!this.childProcess.compareAndSet(null, process)) {
			LOG.warning("race condition when starting child process");
			process.destroy();
			return;
		}
		while (startFile.exists()) {
			LOG.info("startup file still exists...");
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
		LOG.info("child process started");
	}

	@Override
	public void stop() throws IOException {
		Process process = childProcess.getAndSet(null);
		if (process == null) {
			LOG.info("no child process running");
			return;
		}
		LOG.info("stopping child process ..");
		File stopFile = new File(new File(System.getProperty("java.io.tmpdir")), ".it-vest-stopping").getCanonicalFile();
		LOG.info("created signal temp file: '" + stopFile + ", success = " + stopFile.createNewFile());
		process.destroy();
		while (stopFile.exists()) {
			LOG.info("stop file still exists...");
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}

		LOG.info("child process stopped");
		
		try {
			Thread.sleep(5000L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	@Override
	public String uploadData(String s3prefix) throws IOException {
		// after everything is stopped, zip the log dir and upload it to S3
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		String zipFileName = "log-" + format.format(new Date()) + ".zip";
		File zipFile = new File(workingDir, zipFileName);
		File logDir = new File(workingDir, "logs");
		IOUtils.zip(logDir, zipFile);
		AmazonS3 s3 = newAmazonS3();
		String s3key = s3prefix + "-" + zipFileName;
		s3.putObject(S3_BUCKET, s3key, zipFile);
		LOG.info("Uploaded file '" + zipFile + "' to '" + s3key + "'");
		zipFile.delete();
		IOUtils.deleteDirectory(workingDir);
		return s3key;
	}

	private AmazonS3 newAmazonS3() throws IOException {
		AWSCredentials credentials = new PropertiesCredentials(EC2ControllerSvcImpl.class.getResourceAsStream("AwsCredentials.properties"));
		return new AmazonS3Client(credentials);
	}
}
