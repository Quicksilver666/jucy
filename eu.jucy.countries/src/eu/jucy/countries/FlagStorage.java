package eu.jucy.countries;


import geoip.GEOIP;



import java.net.InetAddress;



import logger.LoggerFactory;


import org.apache.log4j.Logger;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;


import uc.IUser;


public class FlagStorage {

	public static final class FlagStorageHolder {
		private static final FlagStorage singleton = new FlagStorage();
	}
	
	public static final Logger logger = LoggerFactory.make();
	

	
	private static final String PLUGIN_ID = "eu.jucy.countries";
	
	public static FlagStorage get() {
		return FlagStorageHolder.singleton;
	}
	
	private  ImageRegistry	ir 	= new ImageRegistry(),
							irRect 	=  new ImageRegistry();
	
	//private ImageRegistry oldAndDisposeable = null;
	
	private Image defaultFlag,defaultFlagRect;
	
	//private Map<Color,Image> defaultFlagRectSt = new HashMap<Color,Image>();
//	private ImageRegistry irRectSt = new HashMap<Color,ImageRegistry>();
	//private Color current;
	
	
	private FlagStorage() {
		defaultFlag = createDefaultFlag();
		defaultFlagRect = getFlagRect(defaultFlag);
		//updateRectStores();

		logger.debug("default flag loaded");
	
//		new PreferenceChangedAdapter(GUIPI.get(),GUIPI.windowColor) { 
//			@Override
//			public void preferenceChanged(String preference, String oldValue,String newValue) {
//				updateRectStores();
//			}
//		}; 
	}
	
	/**
	 * 
	 * @return a white flag that can be used as default.
	 */
	private static Image createDefaultFlag() {
		Image img = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "png/de.png").createImage();
		GC gc = new GC(img);
		gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(img.getBounds());
		gc.dispose();
		return img;
	}
	
//	/**
//	 * changes stored images according do background color..
//	 * done because disposing old flags leads to errors
//	 * as they still might be in use..
//	 */
//	private void updateRectStores() {
//	//	Color current = GUIPI.getColor(GUIPI.windowColor);
//		
//		defaultFlagRect = defaultFlagRectSt.get(current);
//		if (defaultFlagRect == null) {
//			defaultFlagRect = getFlagRect(defaultFlag);
//			defaultFlagRectSt.put(current,defaultFlagRect);
//		}
//		irRect = irRectSt.get(current);
//		if (irRect == null) {
//			irRect = new ImageRegistry();
//			irRectSt.put(current, irRect);
//		}
//	}
	
	
	
	
	
	
	/**
	 * 
	 * @param user - user for which we want the flag..
	 * @param rect - if the flag should be in rectangular format (needed for tables)
	 * @param defaultflag - if true will return a white flag if no other can be determined..
	 * if flase.. null is returned...
	 * @return 
	 */
	public Image getFlag(IUser user,boolean rect,boolean defaultFlag) {
		if (user == null) {
			return getDefault(rect,defaultFlag);
		}
		InetAddress ip = user.getIp();
		if (ip != null) {
			String cc = GEOIP.get().getCountryCode(ip);
			if (cc != null) {
				return getImageByCC(cc,rect);
			}
		}
		return getDefault(rect,defaultFlag); 
	}
	
	private Image getDefault(boolean rect,boolean defaultFlag) {
		return  defaultFlag? (rect ? defaultFlagRect:this.defaultFlag) : null;
	}
	
	private Image getImageByCC(String cc,boolean rect) {
		ImageRegistry ir = rect ? irRect: this.ir;
		Image img = ir.get(cc);
		if (img == null) {
			String s = "png/"+cc.toLowerCase()+".png";
			ImageDescriptor id =  AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, s);
			if (id == null) {
				img = rect ? defaultFlagRect:defaultFlag;
			} else {
				img = id.createImage();
				if (rect) {
					Image old = img;
					img = getFlagRect(img);
					old.dispose();
				}
			}
			ir.put(cc, img);
		}
		return img;
	}
	
	private static Image getFlagRect(Image oFlag) {
		//Color background = GUIPI.getColor(GUIPI.windowColor);
		
		int size = Math.max(oFlag.getBounds().height, oFlag.getBounds().width);
	//	Rectangle rect = new Rectangle(0,0,size,size);
		Image i = new Image(oFlag.getDevice(),size,size);
		GC gc = new GC(i);
		gc.setBackground(oFlag.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, size, size);
		gc.drawImage(oFlag, (size-oFlag.getBounds().width)/2,  (size-oFlag.getBounds().height)/2 );
		gc.dispose();
		
		ImageData id = i.getImageData();
		id.transparentPixel = id.palette.getPixel(new RGB(255,255,255));
		i.dispose();
		return new Image(oFlag.getDevice(),id);
	}
	
}
