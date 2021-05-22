package dk.accel.misw.mp.model.server;

import java.util.List;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.Interfaces;
import com.hazelcast.config.Join;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastUtils {

	public static final String GROUP_NAME = "itvest-mp";
	public static final String GROUP_PASSWORD = "some_random_string";
	
	public static final String TOPIC_NAME = "itvest-mp-topic";
	
	public static HazelcastInstance newHazelcastInstance(String thisIp, List<String> serverIpList) {
		Config config = new Config();
		config.setGroupConfig(new GroupConfig(GROUP_NAME, GROUP_PASSWORD));
		NetworkConfig networkConfig = new NetworkConfig();
		networkConfig.setInterfaces(new Interfaces().addInterface(thisIp).setEnabled(true));
		Join join = new Join();
		TcpIpConfig tcpIpConfig = new TcpIpConfig();
		tcpIpConfig.setMembers(serverIpList);
		join.setTcpIpConfig(tcpIpConfig.setEnabled(true));
		join.setMulticastConfig(new MulticastConfig().setEnabled(false));
		networkConfig.setJoin(join);
		config.setNetworkConfig(networkConfig);
		return Hazelcast.newHazelcastInstance(config);
	}
	
	private HazelcastUtils() {
		throw new AssertionError();
	}
}
