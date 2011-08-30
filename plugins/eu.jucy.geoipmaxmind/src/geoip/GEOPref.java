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
	
	public static final String COUNTRY_ONLY 	=	"countryOnly";
	public static final String LAST_VERSION = 	"LastVersion";
	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = DefaultScope.INSTANCE .getNode(PLUGIN_ID);
		defaults.putBoolean(COUNTRY_ONLY, true);
		defaults.put(LAST_VERSION, "");
	}
	
	public static boolean getBoolean(String what){
		return Boolean.parseBoolean(get(what));
	}
	
	public static String get(String what ) {
		String s = InstanceScope.INSTANCE .getNode(PLUGIN_ID).get(what, null);
		if (s != null) {
			return s;
		}
		s = ConfigurationScope.INSTANCE .getNode(PLUGIN_ID).get(what,null);
		if (s != null) {
			return s;
		}
		
		return DefaultScope.INSTANCE .getNode(PLUGIN_ID).get(what, null);
	}
	
	public static void put(String what,String value) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE .getNode(PLUGIN_ID);
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
