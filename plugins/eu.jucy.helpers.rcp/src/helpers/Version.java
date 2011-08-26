package helpers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class Version {
	
	private static final String VERSION;
	
	static {
		Properties p = new Properties();
		Bundle bundle = Platform.getBundle("eu.jucy.product1");
		Path path = new Path("about.mappings"); 
		URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
		try {
			InputStream is =url.openStream();
			p.load(is);
		
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		VERSION = p.getProperty("0");
	}

	public static String getVersion() {
		return VERSION;
	}
}
