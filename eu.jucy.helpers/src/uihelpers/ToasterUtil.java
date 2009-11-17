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
	
//	private static Toaster toaster;
//	private static Icon icon;
	private static Image image;
	
	static {		
		Bundle bundle = Platform.getBundle(PLUGIN_ID);   
		Path path = new Path(MESSAGE_ICON);
		URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
	//	icon = new ImageIcon(url);
		image = ImageDescriptor.createFromURL(url).createImage();

	}
	
	public static void showMessage(String message,long closeDelay) {
		Toaster t = new Toaster(Display.getDefault(),image,message,closeDelay);
		t.open();
	//	t.setDelayClose(10000);
		//t.scheduleClose();
		
//		if (toaster == null)  {
//			toaster = new Toaster();
//			toaster.setBorderColor(Color.BLUE);
//		//	toaster.setMessageColor(Color.BLACK);
//
//			toaster.setToasterColor(new Color(235,235,235));
//			toaster.setToasterWidth(300);
//		}
//		
//		toaster.showToaster(icon,message);
	}
	
	
}
