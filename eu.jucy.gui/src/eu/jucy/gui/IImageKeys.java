package eu.jucy.gui;


import java.util.GregorianCalendar;

public class IImageKeys {
	
	// usericons
	public static final String 
//								USER_PASSIVE	=   "icons/user_blue_passive.gif",
//								USER_ACTIVE		=	"icons/user_green_active.gif",
//								USER_OFFLINE		=	"icons/user_offline.gif",
//								USER_OPKEY			=	"icons/user_opkey2.gif",
								
								USER_PASSIVE2		=   "icons/user_passive.128.png",
								USER_ACTIVE2		=	"icons/user_active.128.png",
								USER_OFFLINE2		=	"icons/user_offline.128.png",
								USER_OPKEY2			=	"icons/user_opkey.128.png",
	
	// CoolBar icons /Editor icons
//								DOWNLOAD_QUEUE		=	"icons/DownloadQueue.png",
//								SETTINGS			=	"icons/Settings.png",
//								FAVHUBS				=	"icons/FavHubs.png",
//								FILELIST			=	"icons/FilelistIcon.png",
//								SEARCH				=	"icons/search.png",
								NEWPM				=	"icons/new_pm.png",
								NEWPMOFFLINE		=	"icons/new_pm_offline.png",
								
								RECONNECT_SMALL		=	"icons/reconnect.16.gif",
								
	//3 icons for the state of HubEditors
								LEDLIGHT_GREEN		=	"icons/ledgreen.png",
								LEDLIGHT_YELLOW		=	"icons/ledyellow.png",
								LEDLIGHT_RED		=	"icons/ledred.png",
								
								NICK_CALLED			=	"icons/nick_called.22.gif",
								
								//usr actions
								FILELIST			=	"icons/filelist.16.gif",
								SENDPM				=	"icons/sendpm.16.gif",
								FAVUSER				=	"icons/favusers.16.gif",
								
								FAVHUBS				=	"icons/favhubs.16.gif",
								
								
    // Tray icon
								TRAYICON			, 
								
								
								OFFLINE_16			=	"icons/offline.16.gif",

								
	// misc			
								STOPICON			=	"icons/stop.16.gif",
								VIEWIMAGEICON 		=	"icons/view_image.gif",
								ENCRYPTED			=	"icons/encrypted.16.gif",
								ENCRYPTEDKEYP		=	"icons/encryptedkeyp.16.gif",
								DECRYPTED			=	"icons/decrypted.16.gif",
								UPLOAD_ICON			=	"icons/upload.gif" ,
								DOWNLOAD_ICON		=	"icons/download.gif" 
			
									;
								
								
	static {
		GregorianCalendar cal = new GregorianCalendar();
		String icon = "icons/icon_16.gif";
		if (cal.get(GregorianCalendar.MONTH) == GregorianCalendar.DECEMBER) { //christmas
			boolean christmas = false;
			int day = cal.get(GregorianCalendar.DATE);
			christmas = day >= 23 && day <= 27; 
			if (christmas) {
				icon = "icons/iconch09_16.gif";
			}
		} 
		if (cal.get(GregorianCalendar.MONTH) == GregorianCalendar.APRIL && cal.get(GregorianCalendar.DATE) == 1) { //birthday
			icon = "icons/icon_birthday.16.gif";
		}
		TRAYICON =	icon;
	}



}
