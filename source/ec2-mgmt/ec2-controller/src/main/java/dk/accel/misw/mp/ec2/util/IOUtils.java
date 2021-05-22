package dk.accel.misw.mp.ec2.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Common IO helper methods.
 * 
 * @author Morten Andersen, <a href="mailto:dev@accel.dk">dev@accel.dk</a>
 */
public class IOUtils {

	private static final Logger LOG = Logger.getLogger(IOUtils.class.getName());
	
	/**
	 * Closes the stream catching the IOException and just logging it. In most scenarios this
	 * is the most appropriate.
	 * 
	 * @param stream Stream to close. Can be null.
	 */
	public static void silentClose(Closeable stream) {
		if (stream == null) {
			return;
		}
		
		try {
			stream.close();
		} catch (IOException e) {
			// nothing we can do - just log it
			LOG.warning("closing stream failed");
		}
	}

	public static void copyFile(File src, File dest) throws IOException {
		FileInputStream in = new FileInputStream(src);
		try {
			FileChannel inFC = in.getChannel();
			try {
				FileOutputStream out = new FileOutputStream(dest);
				try {
					FileChannel outFC = out.getChannel();
					try {
						long count = inFC.size();
						long copiedCount = inFC.transferTo(0, count, outFC);
						if (count != copiedCount) {
							throw new IOException("Failure during copy of file from '" + src + "' to '" + dest + "', expected to copy " + count 
									+ " bytes, could only copy " + copiedCount);
						}
					} finally {
						silentClose(outFC);
					}
				} finally {
					silentClose(out);
				}
			} finally {
				silentClose(inFC);
			}
		} finally {
			silentClose(in);
		}
	}

	public static void copyStream(InputStream src, OutputStream dest) throws IOException {
		if ((src instanceof FileInputStream) && (dest instanceof FileOutputStream)) {
			// if both streams are file stream, we can use the NIO transferTo method
			FileChannel inFC = ((FileInputStream) src).getChannel();
			try {
				FileChannel outFC = ((FileOutputStream) dest).getChannel();
				try {
					long count = inFC.size();
					long copiedCount = inFC.transferTo(0, count, outFC);
					if (count != copiedCount) {
						throw new IOException("Failure during copy of file from '" + src + "' to '" + dest + "', expected to copy " + count 
								+ " bytes, could only copy " + copiedCount);
					}
				} finally {
					silentClose(outFC);
				}
			} finally {
				silentClose(inFC);
			}
		} else {
			byte[] buffer = new byte[8192];
			int len;
			while ((len = src.read(buffer)) != -1) {
				dest.write(buffer, 0, len);
			}
		}
	}
	
	public static void copyDirectory(File srcDir, File destDir) throws IOException {
		srcDir = srcDir.getCanonicalFile();
		destDir = destDir.getCanonicalFile();
		if (!srcDir.exists()) {
			throw new IOException("Missing src directory '" + srcDir + "'");
		}

		if (destDir.exists()) {
			throw new IOException("Dest directory '" + destDir + "' already exists");
		}
		mkdirs(destDir);
		
		for (File src : srcDir.listFiles()) {
			File dest = new File(destDir, src.getName());
			if (src.isDirectory()) {
				copyDirectory(src, dest);
			} else {
				copyFile(src, dest);
			}
		}
	}

	/**
	 * Implementation like {@link File#mkdirs()} that throws an {@link IOException}
	 * if the dir does not already exist, and if it was impossible to create it.
	 */
	public static void mkdirs(File dir) throws IOException {
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new IOException("Unable to create output directory for '" + dir.getCanonicalPath() + "'");
			}
		}
	}

	/**
	 * Return the base dir for the current service. This defaults to '.', but can be overridden
	 * by setting {@code -Ddk.accel.misw.mp.ec2.ctrl.impl.IOUtils.root=}.
	 */
	public static File getSystemBaseDir() {
		return new File(System.getProperty(IOUtils.class.getCanonicalName() + ".root", "."));
	}
	
	/**
	 * Wrapper for {@link File#File(String)} that uses {@link #getSystemBaseDir()}
	 * for non-absolute filenames.
	 */
	public static File newFile(String fileName) {
		File result = new File(fileName);
		if (!result.isAbsolute()) {
			result = new File(getSystemBaseDir(), result.getPath());
		}
		return result;
	}

	public static File newTempDir(File root) throws IOException {
		for (int i = 0; i < 10; i++) {
			File result = new File(root, UUID.randomUUID().toString()).getCanonicalFile();
			if (!result.exists()) {
				if (result.mkdir()) {
					return result;
				}
			}
			LOG.fine("Unable to create tmp dir ");
		}
		// if we get here, we are unable to create a temp dir in 10 attempts
		throw new IOException("Unable to create tmp dir in " + root);
	}
	
	public static enum FilePermissions { R, W, RW }
	
	public static File checkForDirectory(File file, FilePermissions perms) throws IOException {
		file = file.getCanonicalFile();
		if (!file.isDirectory()) {
			throw new IOException("Not a directory (" + file + ")");
		}
		
		if (FilePermissions.R.equals(perms) || FilePermissions.RW.equals(perms)) {
			if (!file.canRead()) {
				throw new IOException("Not a readable directory (" + file + ")");
			}
		}
		
		if (FilePermissions.W.equals(perms) || FilePermissions.RW.equals(perms)) {
			if (!file.canWrite()) {
				throw new IOException("Not a writeable directory (" + file + ")");
			}
		}
		return file;
	}
	
	public static boolean deleteDirectory(File dir) {
		if (!dir.isDirectory()) {
			return false;
		}
		
		boolean result = true;
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				result &= deleteDirectory(f);
			} else {
				result &= f.delete();
			}
		}
		result &= dir.delete();
		return result;
	}
	
	public static final void copyInputStream(InputStream in, OutputStream out)
	  throws IOException
	  {
	    byte[] buffer = new byte[1024];
	    int len;

	    while((len = in.read(buffer)) >= 0)
	      out.write(buffer, 0, len);

	    in.close();
	    out.close();
	  }

	public static void unzip(File src, File dest) throws IOException {
		mkdirs(dest);

		ZipFile zipFile = new ZipFile(src);
		try {
			for (Enumeration<? extends ZipEntry> it = zipFile.entries(); it.hasMoreElements();) {
				ZipEntry entry = it.nextElement();
				if (entry.isDirectory()) {
					mkdirs(new File(dest, entry.getName()));
					continue;
				}
				OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(dest, entry.getName())));
				try {
					InputStream is = new BufferedInputStream(zipFile.getInputStream(entry));
					try {
						byte[] buff = new byte[8192];
						int len;
						while ((len = is.read(buff)) != -1) {
							os.write(buff, 0, len);
						}
					} finally {
						is.close();
					}
				} finally {
					os.close();
				}
			}
		} finally {
			zipFile.close();
		}
	}
	
	public static void zip(File srcDir, File zipDest) throws IOException {
		if (!srcDir.exists()) {
			throw new IOException("no such src dir " + srcDir);
		}
		if (zipDest.exists()) {
			throw new IOException("dest file " + zipDest + " already exists");
		}
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipDest)));
		try {
			String path = srcDir.getName() + "/";
			ZipEntry entry = new ZipEntry(path);
			zos.putNextEntry(entry);
			zos.closeEntry();
			
			for (File src : srcDir.listFiles()) {
				zipEntry(path, src, zos);
			}
		} finally {
			zos.close();
		}
	}
	
	private static void zipEntry(String path, File src, ZipOutputStream dest) throws IOException {
		if (src.isFile()){
			ZipEntry entry = new ZipEntry(path + src.getName());
			dest.putNextEntry(entry);
			InputStream is = new BufferedInputStream(new FileInputStream(src));
			try {
				copyStream(is, dest);
			} finally {
				silentClose(is);
			}
			dest.closeEntry();
		} else {
			path = (path == null ? "" : path) + src.getName() + "/";
			ZipEntry entry = new ZipEntry(path);
			dest.putNextEntry(entry);
			dest.closeEntry();
			for (File file : src.listFiles()) {
				zipEntry(path, file, dest);
			}
		}
	}
	
	private IOUtils() {
		throw new AssertionError();
	}

}
