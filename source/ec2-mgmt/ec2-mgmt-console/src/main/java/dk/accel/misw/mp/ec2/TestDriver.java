package dk.accel.misw.mp.ec2;

import java.util.logging.Logger;

public class TestDriver {

	private static final Logger LOG = Logger.getLogger(TestDriver.class.getName());
	
	private static String[] TESTS = { "end2end", "hazelcast" /* , "terracotta" */ };
	
	public static void main(String[] args) throws Exception {
		Main.main(new String[] { "start" });
		Main.main(new String[] { "probenet" });
		
		System.out.println("Nodes started and network probed, continue with tests?");
		System.in.read();
		
		for (String test : TESTS) {
			LOG.info("Running tests for " + test);
			System.setProperty("test-project", test);
			Main.main(new String[] { "fulltests" });
			LOG.info("Completed tests for " + test);
		}

		// create the combined graph
		RGraphUtils.visualizeAggregatedCombinedGraph(Main.getOutputRoot(), Main.TEST_RUN_COUNT, TESTS);
		
		System.out.println("Tests completed, stop nodes?");
		System.in.read();
		
		Main.main(new String[] { "stop" });
	}
}
