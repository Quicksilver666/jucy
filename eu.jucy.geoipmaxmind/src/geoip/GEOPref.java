package geoip;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;





public class GEOPref extends AbstractPreferenceInitializer {

	public static final String PLUGIN_ID = "eu.jucy.geoipmaxmind";
	
	private static final Logger logger = LoggerFactory.make();
	
	public static final String countryOnly 	=	"countryOnly";
	public static final String LAST_VERSION = 	"LastVersion";
	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(PLUGIN_ID);
		defaults.putBoolean(countryOnly, true);
		defaults.put(LAST_VERSION, "");
	}
	
	public static boolean getBoolean(String what){
		return Boolean.parseBoolean(get(what));
	}
	
	public static String get(String what ) {
		String s = new InstanceScope().getNode(PLUGIN_ID).get(what, null);
		if (s != null) {
			return s;
		}
		s = new ConfigurationScope().getNode(PLUGIN_ID).get(what,null);
		if (s != null) {
			return s;
		}
		
		return new DefaultScope().getNode(PLUGIN_ID).get(what, null);
	}
	
	public static void put(String what,String value) {
		IEclipsePreferences prefs = new InstanceScope().getNode(PLUGIN_ID);
		prefs.put(what, value);
		try {
			prefs.flush();
		} catch(BackingStoreException bse) {
			logger.error(bse, bse);
		}
	}
	
	public static boolean isVersionChanged() {
		String lastModified = GEOPref.get(GEOPref.LAST_VERSION);
		String currentVersion = Platform.getBundle(PLUGIN_ID).getVersion().toString();
		return !lastModified.equals(currentVersion);
	}
	
	public static void setVersion() {
		put(LAST_VERSION, Platform.getBundle(PLUGIN_ID).getVersion().toString());
	}

}
