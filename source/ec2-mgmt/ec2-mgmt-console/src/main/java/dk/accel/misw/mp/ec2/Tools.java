package dk.accel.misw.mp.ec2;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Tools {

	private static final Pattern CSV_FILE_REGEXP = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.csv$");
	
	static void generateAggregatedCsvFile(File root) throws IOException, InterruptedException {
		List<int[]> maxValues = new ArrayList<int[]>();
		
		File[] folders = root.listFiles();
		Arrays.sort(folders);
		for (File dir : folders) {
			int maxMax = 0;
			int maxSum = 0;
			int count = 0;
			int i = 0;
			for (File csv : dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return CSV_FILE_REGEXP.matcher(name).matches();
				}
			})) {
				// pos 0 = max, pos 1 = sum, pos 2 = count
				int[] max = RGraphUtils.parseClientTimingsMax(csv);
				maxMax = Math.max(maxMax, max[0]);
				maxSum += max[1];
				count += max[2];
				i++;
			}
			if (i != 5) {
				throw new RuntimeException("only " + i + " entries in " + dir);
			}
			maxValues.add(new int[] { (count != 0 ? (maxSum / count) : 0), maxMax });
		}
		
		if (maxValues.size() != 100) {
			throw new RuntimeException("only " + maxValues.size() + " entries in all");
		}

		// finally generate aggregated graphs and report
		RGraphUtils.dumpAggregatedTimingResults(maxValues, Main.getOutputRoot(), "hazelcast");
	}
	
	public static void main(String[] args) throws Exception {
		File hazelcast = new File(Main.getOutputRoot(), "hazelcast");
		generateAggregatedCsvFile(hazelcast);
	}
	
	private Tools() {
		throw new AssertionError();
	}
}
