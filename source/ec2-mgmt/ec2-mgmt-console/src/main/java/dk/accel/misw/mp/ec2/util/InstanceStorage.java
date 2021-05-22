package dk.accel.misw.mp.ec2.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;

/**
 * Simple helper class for storing and loading of {@link Instance} objects.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class InstanceStorage {

	public static void store(String ec2securityGroup, List<Instance> instances, File dest) throws IOException {
		List<PersistentInstance> persistentInstances = new ArrayList<InstanceStorage.PersistentInstance>();
		for (Instance instance : instances) {
			persistentInstances.add(new PersistentInstance(instance));
		}
		
		ObjectOutput oo = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dest)));
		try {
			oo.writeObject(ec2securityGroup);
			oo.writeObject(persistentInstances);
		} finally {
			oo.close();
		}
	}
	
	/**
	 * @param dest adds the loaded instances to this list (i.e. an [out] param).
	 * @return the ec2security group.
	 */
	public static String load(File src, List<Instance> dest) throws IOException {
		String ec2securityGroup;
		List<PersistentInstance> persistentInstances;
		ObjectInput oi = new ObjectInputStream(new BufferedInputStream(new FileInputStream(src)));
		try {
			ec2securityGroup = (String) oi.readObject();
			persistentInstances = PersistentInstance.castList(oi.readObject());
		} catch (ClassNotFoundException e) {
			throw new IOException("Class not found", e);
		} finally {
			oi.close();
		}
		for (PersistentInstance instance : persistentInstances) {
			dest.add(instance.getInstance());
		}
		return ec2securityGroup;
	}
	
	/**
	 * A serializable wrapper around {@link Instance} making the fields 
	 * we use serializable.
	 * 	
	 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
	 */
	private static class PersistentInstance implements Serializable {
		private static final long serialVersionUID = 1L;

		private transient final Instance instance;
		
		private PersistentInstance(Instance instance) {
			this.instance = instance;
		}
		
		Instance getInstance() {
			return instance;
		}
		
		private Object writeReplace() {
			return new SerializationProxy(instance);
		}
		
		private void readObject(ObjectInputStream stream) throws InvalidObjectException {
			throw new InvalidObjectException("proxy required");
		}
		
		/**
		 * Serialization proxy for {@link PersistentInstance}
		 * 
		 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
		 */
		private static class SerializationProxy implements Serializable {
			private static final long serialVersionUID = 1L;
			
			private final String instanceId;
			private final String privateIp;
			private final String privateDns;
			private final String publicIp;
			private final String publicDns;
			
			SerializationProxy(Instance instance) {
				this.instanceId = instance.getInstanceId();
				this.privateIp = instance.getPrivateIpAddress();
				this.privateDns = instance.getPrivateDnsName();
				this.publicIp = instance.getPublicIpAddress();
				this.publicDns = instance.getPublicDnsName();
			}
			
			private Object readResolve() {
				Instance instance = new Instance();
				instance.setInstanceId(instanceId);
				instance.setPrivateIpAddress(privateIp);
				instance.setPrivateDnsName(privateDns);
				instance.setPublicIpAddress(publicIp);
				instance.setPublicDnsName(publicDns);
				return new PersistentInstance(instance);
			}
		}
		
		/**
		 * To minimize scope of suppressWarnings.
		 */
		@SuppressWarnings("unchecked")
		private static List<PersistentInstance> castList(Object o) {
			return (List<PersistentInstance>) o;
		}
	}
}
