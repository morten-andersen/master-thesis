package dk.accel.misw.mp.model.common.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MainUtil {

	private static final Logger LOG = Logger.getLogger(MainUtil.class.getName());
	
	public static String[] parseArgs(String[] args, int min, int max, String usage) {
		if (args.length > 0) {
			// use args
			if ((args.length < min) || (args.length > max)) {
				System.err.println("Usage: " + usage);
				System.exit(1);
			}
			return args;
		} else {
			// use the enviroment
			List<String> argsList = new ArrayList<String>();
			for (int i = 0; i < min; i++) {
				String arg = System.getenv("arg" + i);
				if (arg == null) {
					System.err.println("Usage: " + usage);
					System.exit(1);
				}
				argsList.add(arg);
			}
			for (int i = min; i < max; i++) {
				String arg = System.getenv("arg" + i);
				if (arg == null) {
					// last arg
					break;
				}
				argsList.add(arg);
			}
			return argsList.toArray(new String[argsList.size()]);
		}
	}
	
	public static void deleteSignalFile(boolean isStart) {
		String name = ".it-vest-" + (isStart ? "starting" : "stopping");
		try {
			File file = new File(new File(System.getProperty("java.io.tmpdir")), name).getCanonicalFile();
			if (file.exists()) {
				LOG.info("found signal file: '" + file + "', deleting");
				boolean ok = file.delete();
				LOG.info("deleted signal file (status: " + ok + ")");
			} else {
				LOG.warning("no signal file: '" + file + "', found");
			}
		} catch (IOException e) {
			LOG.warning("error when checking for signal file: '" + name + "' (" + e + ")");
		}
	}
	
	private MainUtil() {
		throw new AssertionError();
	}
}
