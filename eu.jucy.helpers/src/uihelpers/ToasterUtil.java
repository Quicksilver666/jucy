package uihelpers;


import java.net.URL;
import java.util.Collections;





import mylyntoaster.Toaster;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;







public class ToasterUtil {
	
	public  static final String PLUGIN_ID 		= "eu.jucy.helpers",
								MESSAGE_ICON 	= "icons/newMessage.png";
	
	private static final int MAX_MESSAGE_SIZE = 500; 
	

	private static Image image;
	
	static {		
		Bundle bundle = Platform.getBundle(PLUGIN_ID);   
		Path path = new Path(MESSAGE_ICON);
		URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
		image = ImageDescriptor.createFromURL(url).createImage();

	}
	
	public static void showMessage(String message,long closeDelay) {
		if (message.length() > MAX_MESSAGE_SIZE) {
			message = 	 message.substring(0, MAX_MESSAGE_SIZE/2)
						+ "..." 
						+ message.substring(message.length()-MAX_MESSAGE_SIZE/2);
		}
		Toaster t = new Toaster(Display.getDefault(),image,message,closeDelay);
		t.open();

	}
	
	
}
