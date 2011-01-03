package eu.jucy.ui.hublist;

import helpers.GH;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class HublistPI extends AbstractPreferenceInitializer {

	public static final String PLUGIN_ID = "eu.jucy.ui.hublist";
	
	
	public static final String hublistServers		=	"HublistServers" ;
	
	
	public static final String[] defaultHublists = new String[] {
	
		//	"http://dchublist.com/hublist.xml.bz2;"+
		//		"http://www.dc-resources.com/downloads/hublist.config.bz2;"+
		//		"http://www.hublist.org/PublicHubList.xml.bz2;"+
			//	"http://hublist.openhublist.org/hublist.xml.bz2;"+
				"http://myhublist.com/hublist.xml.bz2",
			//	"http://hublist.hubtracker.com/hublist.xml.bz2;" + //former: http://hublist.hubtracker.com/hublist.xml.bz2 
				"http://dchublist.com/hublist.xml.bz2",
				"http://adc.dchublist.com/hublist.xml.bz2",
		//		"http://adchublist.com/hublist.xml.bz2;" +
				//"http://www.hublist.org/PublicHubList.xml.bz2;" + --> offline forever?
			
				
			//	"http://download.hublist.cz/hublist.xml.bz2;" +  offline
				
				"http://hublist.hubtracker.com.nyud.net/hublist.xml.bz2",
				"http://dchublist.com.nyud.net/hublist.xml.bz2",
	//			"http://adchublist.com.nyud.net/hublist.xml.bz2;" +
			//	"http://www.hublist.org.nyud.net/PublicHubList.xml.bz2;" +
				"http://dclist.eu.nyud.net/hublist.xml.bz2" 
			//	"http://download.hublist.cz.nyud.net/hublist.xml.bz2;" +
			//	"http://hublist.awenet.info.nyud.net/PublicHubList.xml.bz2;" +
		//		"http://hublist.hubtracker.com/hublist.xml.bz2;"+  -> openhublist
			//	"http://hublist.dreamland-net.eu/PublicHubList.xml.bz2" 
				
	};
		
		
	
	public HublistPI() {}

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(PLUGIN_ID);

		defaults.put(hublistServers, GH.concat(defaultHublists,";",""));
	}

	public static String[] getHublists() {
		return get(hublistServers).split(Pattern.quote(";"));
	}
	
	private static String get(String what ){
    	return  new InstanceScope().getNode(PLUGIN_ID).get(what,
    			new ConfigurationScope().getNode(PLUGIN_ID).get(what,
    			new DefaultScope().getNode(PLUGIN_ID).get(what, null)));
    }
	
}
