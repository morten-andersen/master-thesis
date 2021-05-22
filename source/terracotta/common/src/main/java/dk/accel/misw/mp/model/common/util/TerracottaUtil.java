package dk.accel.misw.mp.model.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;

public class TerracottaUtil {

	public static byte[] serialize(Object val) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(val);
		oos.close();
		return baos.toByteArray();
	}
	
	public static <T> T deserialize(byte[] val, Class<T> cls) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(val);
		ObjectInputStream ois = new ObjectInputStream(bais);
		try {
			return cls.cast(ois.readObject());
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}

	public static String createConfigFile(List<String> serverIpList) throws IOException {
		File dest = new File("terracotta-config.xml").getCanonicalFile();
		String text = Resources.toString(Resources.getResource(TerracottaUtil.class, "terracotta-config.xml"), Charset.forName("US-ASCII"));
		for (int i = 0; i < serverIpList.size(); i++) {
			text = text.replaceAll(Pattern.quote("server-" + i), serverIpList.get(i));
		}
		final String content = text;
		
		Files.copy(new InputSupplier<InputStream>() {

			@Override
			public InputStream getInput() throws IOException {
				return new ByteArrayInputStream(content.getBytes(Charset.forName("US-ASCII")));
			}
		}, dest);
		return dest.getCanonicalPath();
	}
	
	private TerracottaUtil() {
		throw new AssertionError();
	}
}
