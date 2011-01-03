package eu.jucy.eliza;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import eliza.ElizaMain;

public class ElizaSession {

	private static final String PLUGIN_ID = "eu.jucy.eliza";
	private final ElizaMain eliza;
	
	public ElizaSession() {
		this.eliza = new ElizaMain();
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		Path path = new Path("script"); 
		URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
		try {
			InputStream is = url.openStream();
			eliza.readScript(is);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	
	/**
	 * say something to eliza and get a respnsoe for it..
	 * @param what - what is said to eliza
	 * @return what is the response to eliza
	 */
	public String saySomethingToEliza(String what) {
		String s = eliza.processInput(what);
		return s;
	}
	
	
	
}
