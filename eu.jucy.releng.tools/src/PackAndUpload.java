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
