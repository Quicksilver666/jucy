package eu.jucy.gui;

import java.util.HashMap;
import java.util.Map;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

import org.osgi.service.prefs.BackingStoreException;

import eu.jucy.gui.texteditor.DateStampTextModificator;
import eu.jucy.gui.texteditor.ITextModificator;


public class GUIPI extends AbstractPreferenceInitializer {

	private static Logger logger = LoggerFactory.make();
	

	/**
	 * GUI configurations..
	 */
	public static final String  
	openPMInForeground	=	"openPMInForeground",
	showPMsInMC			=	"showPMsInMC", 
	askBeforeShutdown 	=	"askBeforeShutdown",
	timeStamps			=	GUIPI.IDForTextModificatorEnablement(DateStampTextModificator.ID),
	timeStampFormat		=	"timeStampFormat",
	
	editorFont			=	"editorFont",
	downloadColor1		=	"downloadColor1",
	downloadColor2		=	"downloadColor2",
	uploadColor1		=	"uploadColor1",
	uploadColor2		=	"uploadColor2",
	windowColor			=	"windowColor",
	windowFontColor		=	"windowFontColor",
	
	minimizeToTray		=	"minimizeToTray",
	minimizeOnStart		=	"minimizeOnStart",
	setAwayOnMinimize	=	"setAwayOnMinimize",
	alternativePresentation = "alternativePresentation",
	
	//Toaster stuff
	showToasterMessages	=	"showToasterMessages",
	addSoundOnPM		=	"addSoundToToaster",
	showToasterMessagesChatroom	=	"showToasterMessagesChatroom",
	addSoundOnChatroomMessage	=	"addSoundOnChatroomMessage",
	showToasterMessagesNickinMC	=	"showToasterMessagesNickinMC",
	addSoundOnNickinMC	=	"addSoundOnNickinMC",
	toasterTime			=	"toasterTime",
	
	//Tables
	hubWindowUserTable 	= 	"hubWindowUserTable",
	favHubsTable		=	"favHubsTable",
	favUsersTable		=	"favUsersTable",
	favUsersSlotTable	=	"favUsersSlotTable",
	fileListTable		=	"fileListTable",
	searchEditorTable	=	"searchEditorTable",
	downloadQueueTable	=	"downloadQueueTable",
	
	transfersViewTable	=	"transfersViewTable",
	downloadsViewTable	=	"downloadsViewTable",
	
	finishedTransfersTable	=	"finishedTransfers",
	uploadQueueTable	=	"uploadQueueTable",
	logViewerTable		=	"logViewerTable",
	
	userCommands		=	"userCommands",
	
	//StatusBarItems..
	hubsContrib			=	"hubsContrib",
	slotsContrib		=	"slotsContrib",
	downContrib			=	"downContrib",
	upContrib			=	"upContrib",
	downSpeedContrib	=	"downSpeedContrib",
	upSpeedContrib		=	"upSpeedContrib",
	shareSizeContrib	=	"shareSizeContrib",
	connectionStatusContrib="connectionStatusContrib",
	awayContrib			=	"awayContrib",
	
	
	//Nick Colourer
	ownNickCol = "ownNickCol" ,
	ownNickFont = "ownNickFont" ,
	opNickCol = "opNickCol",
	opNickFont = "opNickFont",
	favNickCol = "favNickCol",
	favNickFont = "favNickFont" ,
	normalNickCol= "normalNickCol",
	normalNickFont= "normalNickFont",
	
	//URL colourer
	urlModCol = "urlModCol" ,
	urlModFont = "urlModFont",
//	urlModUnderline = "urlModUnderline",
	
	//File colouring
	fileInDownloadCol= "fileInDownloadCol",
	fileInShareCol="fileInShareCol",
	fileMultiUserCol = "fileMultiUserCol",
	fileDefaultCol = "fileDefaultCol",
	
	allowTestRepos = "allowTestRepos",
	
	lastStartupVersion = "lastStartupVersion";


	private static Map<FontData,Font> FontRegistry = new HashMap<FontData,Font>();
	private static Map<RGB,Color> ColorRegistry = new HashMap<RGB,Color>(); 
	
	
	public GUIPI() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(Application.PLUGIN_ID);
		
		defaults.putBoolean(timeStamps, true);
		defaults.put(timeStampFormat, "[HH:mm]");
		
		defaults.putBoolean(openPMInForeground, true);
		defaults.putBoolean(showPMsInMC, false);
		defaults.putBoolean(askBeforeShutdown, false);
		
		
		defaults.put(editorFont, PreferenceConverter.FONTDATA_DEFAULT_DEFAULT.toString());
	
		RGB black = new RGB(0,0,0);
		RGB white = new RGB(255,255,255);
		defaults.put(downloadColor2, StringConverter.asString(new RGB(53,160,126)));
		defaults.put(downloadColor1, StringConverter.asString(new RGB(53,160,56)));
		defaults.put(uploadColor2, StringConverter.asString(new RGB(207,18,88)));
		defaults.put(uploadColor1, StringConverter.asString(new RGB(207,18,18)));
		defaults.put(windowColor, StringConverter.asString(white));
		defaults.put(windowFontColor, StringConverter.asString(black));
		
		// nick colourer
		defaults.put(ownNickCol, StringConverter.asString(black));
		defaults.put(opNickCol, StringConverter.asString(black));
		defaults.put(favNickCol, StringConverter.asString(black));
		defaults.put(normalNickCol, StringConverter.asString(black));
		
		defaults.put(ownNickFont, PreferenceConverter.FONTDATA_DEFAULT_DEFAULT.toString());
		defaults.put(opNickFont, PreferenceConverter.FONTDATA_DEFAULT_DEFAULT.toString());
		defaults.put(favNickFont, PreferenceConverter.FONTDATA_DEFAULT_DEFAULT.toString());
		defaults.put(normalNickFont, PreferenceConverter.FONTDATA_DEFAULT_DEFAULT.toString());
		
		//url colourer
		defaults.put(urlModCol, StringConverter.asString(white)); //new RGB(0,0,0xff))); //blue
		defaults.put(urlModFont, PreferenceConverter.FONTDATA_DEFAULT_DEFAULT.toString());
		//defaults.putBoolean(urlModUnderline, true);
		
		
			
		//file colourer
		defaults.put(fileInDownloadCol,StringConverter.asString( new RGB(0xff,0,0))); //red
		defaults.put(fileInShareCol, StringConverter.asString(new RGB(0,0,0xff))); //blue
		defaults.put(fileMultiUserCol, StringConverter.asString(new RGB(0,0,0x80))); //dark blue
		defaults.put(fileDefaultCol, StringConverter.asString(black)); //black
		
		
		defaults.putBoolean(minimizeToTray, true);
		defaults.putBoolean(minimizeOnStart, false);
		defaults.putBoolean(setAwayOnMinimize, true);
		defaults.putBoolean(alternativePresentation, false);
		
		defaults.putBoolean(showToasterMessages, true);
		defaults.putBoolean(addSoundOnPM, false);
		defaults.putBoolean(showToasterMessagesChatroom, false);
		defaults.putBoolean(addSoundOnChatroomMessage, false);
		defaults.putBoolean(showToasterMessagesNickinMC, false);
		defaults.putBoolean(addSoundOnNickinMC, false);
		defaults.putInt(toasterTime, 6000);
		
		defaults.putBoolean(hubsContrib, true);
		defaults.putBoolean(slotsContrib, true);
		defaults.putBoolean(downContrib, true);
		defaults.putBoolean(upContrib, true);
		defaults.putBoolean(downSpeedContrib, true);
		defaults.putBoolean(upSpeedContrib, true);
		defaults.putBoolean(shareSizeContrib, false);
		defaults.putBoolean(connectionStatusContrib, true);
		defaults.putBoolean(awayContrib	, true);
		
		defaults.put(userCommands,"Kick user\\n6\\nop\\n$Kick %[nick]|\\ntrue\\n\n"+
			"Redirect\\n2\\nop\\n$OpForceMove $Who:%[userNI]$Where:%[line:Address?]$Msg:%[line:Reason?]|\\ntrue\\n");
		
		defaults.putBoolean(allowTestRepos, false);
		
		defaults.put(lastStartupVersion, "UC V:0.60");
		
		
		logger.debug("initialized GUI defaults");
		
		
		IExtensionRegistry reg = Platform.getExtensionRegistry();
    	
		IConfigurationElement[] configElements = reg
		.getConfigurationElementsFor(ITextModificator.ExtensionpointID);

		
		for (IConfigurationElement element : configElements) {
			try {
				String fullID = IDForTextModificatorEnablement(element.getAttribute("id"));
				defaults.putBoolean(fullID, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static IEclipsePreferences get() {
		return new InstanceScope().getNode(Application.PLUGIN_ID);
	}

	/**
	 * callers must !not! dispose their colours!!
	 * @param what - key of the colour
	 * @return the colour stored at that key
	 */
	public static Color getColor(String what) {
		RGB rgb = StringConverter.asRGB(get(what));
		Color colour = ColorRegistry.get(rgb);
		if (colour == null) {
			colour = new Color(null,rgb);
			ColorRegistry.put(rgb, colour);
		}
		
		return colour;
	}

	/**
	 * callers must !not! dispose their Fonts..
	 * @param what
	 * @return
	 */
	public static Font getFont(String what) {
		 FontData fd = new FontData(get(what));
		 Font font = GUIPI.FontRegistry.get(fd);
		 if (font == null) {
			 font = new Font(null, fd);
			 GUIPI.FontRegistry.put(fd, font);
		 }
		 return font;
	}

	public static boolean getBoolean(String what){
		return Boolean.parseBoolean(get(what));
	}

	public static int getInt(String what) {
		return Integer.parseInt(get(what));
	}

	public static String get(String what) {
		String s = new InstanceScope().getNode(Application.PLUGIN_ID).get(what, null);
		if (s != null) {
			return s;
		}
		s = new ConfigurationScope().getNode(Application.PLUGIN_ID).get(what,null);
		if (s != null) {
			return s;
		}
		
		return new DefaultScope().getNode(Application.PLUGIN_ID).get(what, null);
	}

	public static boolean put(String key,int value) {
		return put(key,""+value);
	}

	public static boolean put(String key,boolean value) {
		return put(key,""+value);
	}

	/**
	 * tries setting a value in the InstanceScope  
	 * @param key - 
	 * @param value - 
	 * @return true if successful set - false otherwise
	 */
	public static boolean put(String key,String value) {
		return put(key,value,Application.PLUGIN_ID);
	}
	
	public static boolean put(String key, String value,String pluginID) {
		IEclipsePreferences is = new InstanceScope().getNode(pluginID);
		is.put(key, value);
		try {
			is.flush();
		} catch(BackingStoreException bse) {
			bse.printStackTrace();
			return false;
		}
		return true;
	}

	public static String IDForTextModificatorEnablement(String textModificatorID) {
		return ITextModificator.ExtensionpointID+"."+textModificatorID;
	}
	

}
