package eu.jucy.ui.smileys;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class SmileysPI extends AbstractPreferenceInitializer {

	public static final String PLUGIN_ID = "eu.jucy.ui.smileys"; //$NON-NLS-1$
	
	public static final String SMILEYS_PATH = "SMILEYS_PATH";
	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(PLUGIN_ID);
		defaults.put(SMILEYS_PATH, "");
		
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
	

}
