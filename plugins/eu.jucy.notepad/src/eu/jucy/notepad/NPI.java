package eu.jucy.notepad;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;





public class NPI extends AbstractPreferenceInitializer {

	public static final String PLUGIN_ID = "eu.jucy.notepad" ;
	
	public static final String NR_OF_NOTEPADS = "NR_OF_NOTEPADS";

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(PLUGIN_ID);
		defaults.putInt(NR_OF_NOTEPADS, 1);

	}
	
	public static int getInt(String what) {
		return Integer.parseInt(get(what));
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
		
		return new DefaultScope().getNode(PLUGIN_ID).get(what, "");
	}

}
