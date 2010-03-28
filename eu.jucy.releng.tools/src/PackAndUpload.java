import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


public class PackAndUpload {

	private static Properties prop;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		loadProperties( args.length >= 1 ? args[0]: "deploy.properties");

	}
	
	public static String get(String key) {
		if (prop == null) {
			try {
				loadProperties("deploy.properties");
			} catch(IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}
		return prop.get(key).toString();
	}
	
	private static void loadProperties(String file) throws IOException {
		prop = new Properties();
		File loadProp = new File(file);
		if (!loadProp.isFile()) {
			throw new FileNotFoundException(file);
		}
		FileInputStream fis = new FileInputStream(loadProp);
		prop.load(fis);
		fis.close();
	}
	
	

}
