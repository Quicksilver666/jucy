package eu.jucy.countries;

import geoip.GEOIP;
import helpers.GH;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logger.LoggerFactory;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;

import uc.DCClient;
import uc.IHub;
import uc.IUser;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.texteditor.ITextModificator;
import eu.jucy.gui.texteditor.SendingWriteline;
import eu.jucy.gui.texteditor.StyledTextViewer;
import eu.jucy.gui.texteditor.StyledTextViewer.ImageReplacement;
import eu.jucy.gui.texteditor.StyledTextViewer.Message;
import eu.jucy.gui.texteditor.StyledTextViewer.TextReplacement;



public class FlagsTextModificator implements ITextModificator {
	
	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	private final Set<IUser> askingForIP = new HashSet<IUser>();
	
	public FlagsTextModificator() {}


	public void init(StyledText st,StyledTextViewer viewer, IHub hub) {
		DCClient.execute(new Runnable() {
			public void run() {
				GEOIP.get(); //force initialization
			}
		});
	}
	

	public void dispose() {}



	public void getMessageModifications(Message original, boolean pm,List<TextReplacement> replacement) {
		final IUser user = original.getUsr();
		if (user != null) {
			if (user.getIp() != null) {
				String cc = GEOIP.get().getCountryCode(user.getIp());
				if (!GH.isNullOrEmpty(cc)) {
					cc = "["+cc+"]";
				} else {
					cc= "";
				}
				Image flag = FlagStorage.get().getFlag(user,false,true);
				replacement.add(new ImageReplacement(1,0,cc,flag));
				
			} else if (!askingForIP.contains(user) 
					&& SendingWriteline.lastTypingOccurred(300000)  //only ask for users if we are on the keyboard..-> prevents spamming..
					&& (ApplicationWorkbenchWindowAdvisor.get().isActive() 
							|| user.isActive()
							|| user.getHub().supportsUserIP()) 
					&& user.getShared() != 0 ) {
				
				askingForIP.add(user);
				DCClient.execute(new Runnable() {
					public void run() {
						IHub hub = user.getHub();
						logger.debug("asking for user: " + user + "  "+ hub.getName()+"  "+hub.supportsUserIP());
						if (hub.supportsUserIP()) {
							hub.requestUserIP(user);
						} else {
							hub.requestConnection(user, ""+ System.currentTimeMillis());
						}
					}
				});
				if (askingForIP.size() > 10) {
					askingForIP.clear();
					logger.debug("clearing ask ");
				}
			}
		}
	}
	
	

	
}
