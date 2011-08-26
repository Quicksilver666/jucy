package helpers;



import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

public abstract class PreferenceChangedAdapter implements
		IPreferenceChangeListener {

	private final String[] preferences;
	
	private final IEclipsePreferences source;
	
	public PreferenceChangedAdapter(String pluginId,String... preferences) {
		this(new InstanceScope().getNode(pluginId),preferences);
	}
	
	public PreferenceChangedAdapter(IEclipsePreferences source ,String... preference ) {
		this.preferences = preference;
		this.source = source;
		source.addPreferenceChangeListener(this);
	}
	
	

	public final void preferenceChange(PreferenceChangeEvent event) {
		for (String preference: preferences) { 
			if (event.getKey().equals(preference)) {
				preferenceChanged(preference,(String)event.getOldValue(),(String)event.getNewValue());
			}
		}
	}
	
	public void dispose() {
		source.removePreferenceChangeListener(this);
	}

	public void reregister() {
		source.addPreferenceChangeListener(this);
	}


	public abstract void preferenceChanged(String preference,String oldValue,String newValue);
	
	
	

}
