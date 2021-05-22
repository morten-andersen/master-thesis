package dk.accel.misw.mp.ec2;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class S3Uploader implements Closeable {

	private static final Logger LOG = Logger.getLogger(S3Uploader.class.getName());
	
	private static final String S3_BUCKET = "dk.accel.itvest";
	
	private final AmazonS3 s3;
	
	public S3Uploader() throws IOException {
		this.s3 = new AmazonS3Client(EC2Launcher.getCredentials());
	}
	
	public void put(String key, File src) throws IOException {
		if (!src.exists()) {
			throw new IOException("Source file does not exist (" + src + ")");
		}
		s3.putObject(S3_BUCKET, key, src);
		LOG.info("Uploaded file '" + src + "' to '" + key + "'");
	}
	
	public void get(String key, File dest) throws IOException {
		if (dest.exists()) {
			throw new IOException("Destination file already exist (" + dest + ")");
		}
		ObjectMetadata result = s3.getObject(new GetObjectRequest(S3_BUCKET, key), dest);
		LOG.info("Retrieved file '" + dest + "' (len: " + result.getContentLength() + ", type: " + result.getContentType() + ")");
	}
	
	public void delete(String key) {
		s3.deleteObject(S3_BUCKET, key);
	}
	
	@Override
	public void close() {
		// noop
	}
}
