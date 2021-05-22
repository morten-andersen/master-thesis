package dk.accel.misw.mp.ec2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.io.Files;

import dk.accel.misw.mp.ec2.util.ProcessUtils;

class LyxUtils {

	static void generateLyxReport(File outputRoot, String testProject) throws IOException, InterruptedException {
		File destDir = new File(outputRoot, "graphs-" + testProject);;
		
		// start by copying lyx scripts to the destDir
		File etcDir = new File(new File(System.getProperty("user.dir")), "etc/lyx-scripts");
		List<File> scriptFiles = new ArrayList<File>();
		for (File f : etcDir.listFiles()) {
			File to = new File(destDir, f.getName());
			Files.copy(f, to);
			if (to.getName().endsWith(".sh")) {
				to.setExecutable(true);
			}
			scriptFiles.add(to);
		}
		
		// call the bash script for generating the lyx file
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", destDir.getCanonicalPath() + "/create-lyx-file.sh " + testProject).redirectErrorStream(false);
		processBuilder.directory(destDir);
		Process process = processBuilder.start();
		
		ExecutorService streamExecutor = ProcessUtils.processStreams(process, System.out, System.err);
		int errCode = process.waitFor();
		if (errCode != 0) {
			throw new IOException("Failed in generating lyx report, return code = " + errCode);
		}
		streamExecutor.shutdown();
		streamExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
		
		// remove lyx scripts
		for (File f : scriptFiles) {
			if (!f.delete()) {
				f.deleteOnExit();
			}
		}
	}
	
	private LyxUtils() {
		throw new AssertionError();
	}
}
