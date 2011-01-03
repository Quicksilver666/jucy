package eu.jucy.ui.translation;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.google.api.translate.Language;



public class TransPI extends AbstractPreferenceInitializer {
	
	public static final String PLUGIN_ID = "eu.jucy.ui.translation";
	
	public static final String 	sourceLanguage = "sourceLanguage",
									targetLanguage = "targetLanguage" ;

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(PLUGIN_ID);
		
		defaults.put(sourceLanguage, Language.AUTO_DETECT.name());
		defaults.put(targetLanguage, Language.ENGLISH.name());
	}
	
	public static Language getLang(String key) {
		String val = get(key);
		return Language.valueOf(val);
	}

	public static String get(String what) {
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
