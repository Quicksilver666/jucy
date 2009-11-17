package eu.jucy.op;

import java.util.List;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import eu.jucy.op.ui.CounterFieldEditor;

public class PI extends AbstractPreferenceInitializer {

	public static final String PLUGIN_ID = "eu.jucy.op";
	
	public static final String 	protectedUsersRegEx 	= 	"protectedUsersRegEx",
								parallelChecks			=	"parallelChecks",
								counters				=	"counters", // all counters
								opADLEntries 			=	"opADLEntries",
								staticReplacements		=	"staticReplacements",
								checkUsers				=	"checkUsers",
								
								counterActionsTable 	=	PLUGIN_ID+".counterActionsTable",
								staticReplacementTable	=   PLUGIN_ID+".staticReplacementTable";
	
	
	
	public static final String 	fh_checkUsers 	= PLUGIN_ID +".CheckUsers",
								fh_replacements	= PLUGIN_ID +".replacements";	
	
	
	
	public PI() {}

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(PLUGIN_ID);
		
		defaults.put(protectedUsersRegEx, "");
		defaults.putInt(parallelChecks, 10);
		defaults.put(counters, "");
		defaults.put(opADLEntries, "");
		defaults.put(staticReplacements, "");
		defaults.putBoolean(checkUsers, true);
		
	}
	
	public static List<CounterFactory> getCounterFactories() {
		return CounterFieldEditor.loadCFFromString(get(PI.counters));
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
	
	public static int getInt(String what) {
		return Integer.parseInt(get(what));
	}

	public static boolean getBoolean(String what){
		return Boolean.parseBoolean(get(what));
	}

	public static IEclipsePreferences get() {
		return new InstanceScope().getNode(PLUGIN_ID);
	}

}
