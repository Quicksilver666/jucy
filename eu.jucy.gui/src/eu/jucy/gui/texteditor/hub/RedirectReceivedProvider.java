package eu.jucy.gui.texteditor.hub;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

/**
 * TODO
 * 
 * http://www.vogella.de/articles/EclipseCommands/article.html
 * 
 * 
 * @author christian
 *
 */
public class RedirectReceivedProvider extends AbstractSourceProvider {
	public final static String REDIRECT_STATE = "eu.jucy.hub.sourceprovider.redirect";
	public final static String ENABLED = "ENABLED";
	public final static String DISENABLED = "DISENABLED";

	private boolean enabled = true ;
	
	public RedirectReceivedProvider() {	
	
	}

	public void dispose() {}
	

	@SuppressWarnings("unchecked")
	public Map getCurrentState() {
		Map<Object,Object> map = new HashMap<Object,Object>(1);
		String value = enabled ? ENABLED : DISENABLED;
		map.put(REDIRECT_STATE, value);
		return map;

	}

	public String[] getProvidedSourceNames() {
		return new String[]{REDIRECT_STATE};
	}
	
	public void toogleEnabled() {
		enabled = !enabled ;
		String value = enabled ? ENABLED : DISENABLED;
		fireSourceChanged(ISources.WORKBENCH, REDIRECT_STATE, value);
	}


}
