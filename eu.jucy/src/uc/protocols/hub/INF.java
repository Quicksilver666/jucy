package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;




import uc.FavHub;
import uc.Identity;
import uc.User;
import uc.IUserChangedListener.UserChange;
import uc.IUserChangedListener.UserChangeEvent;
import uc.crypto.HashValue;
import uc.protocols.ADCStatusMessage;
import uc.protocols.ConnectionState;
import uc.protocols.DCProtocol;


public class INF extends AbstractADCHubCommand {

	
	
	private static final Map<FavHub,Map<INFField,String>> LAST_INF =  
				new HashMap<FavHub,Map<INFField,String>>();
	
	public INF(Hub hub) {
		super(hub);
		setPattern(getHeader()+" (.*)",true);
	}


	public void handle(String command) throws ProtocolException, IOException {
		logger.debug("Received inf: "+command);
		String sids = getOtherSID(); // matcher.group(1);
		Map<INFField,String> attribs = INFMap(matcher.group(HeaderCapt+1)) ;
		if (!GH.isEmpty(sids)) { 
			logger.debug("Received inf2 " );
			int sid = SIDToInt(sids);
			boolean self = hub.getSelf().getSid() == sid ;
			if ( self &&  ConnectionState.CONNECTED.equals(hub.getState()) ) {
				hub.onLogIn();
			}
			
			User current = hub.getUserBySID(sid);
	
			
			boolean connected = current == null;
			if (connected) {
				if (attribs.containsKey(INFField.ID)) {
					String id = attribs.get(INFField.ID); 
					HashValue cid = HashValue.createHash(id);
					HashValue userid = DCProtocol.CIDToUserID(cid, hub);
					
					current = hub.getDcc().getPopulation().get("", userid);
					if (current.isOnline()) { //if the user is already online ... then the hub fucked up..
						STA.sendSTAtoHub(hub, new ADCStatusMessage("User already online as " +current.getNick(), 
										ADCStatusMessage.RECOVERABLE, 
										ADCStatusMessage.UserCIDTaken));
						return;
					}
					current.setSid(sid);
				} else { 
					//Special handling of users without ID --> discard the command  TODO .. add CID handling ...
					STA.sendSTAtoHub(hub, new ADCStatusMessage("Need ID in first INF", 
							ADCStatusMessage.RECOVERABLE, 
							ADCStatusMessage.ProtocolRequiredINFfieldBadMissing, 
							Flag.FM, "ID"));
					
					return;
				}
			} else {
				attribs.remove(INFField.ID); //ignore IDS of present users...
			}
			
			for (Entry<INFField,String> attr : attribs.entrySet()) {
				current.setProperty(attr.getKey(), attr.getValue());
			}
			
		    if (connected) {
		    	hub.insertUser(current);
		    } else {
		    	current.notifyUserChanged(UserChange.CHANGED,UserChangeEvent.INF);
		    }
	    
		} else {
			// HubInfo received as SID  is not Present..
			if (attribs.containsKey(INFField.NI)) {
				hub.setHubname(attribs.get(INFField.NI));
			}
			if (attribs.containsKey(INFField.DE)) {
				hub.setTopic(attribs.get(INFField.DE));
			}
			if (attribs.containsKey(INFField.VE)) {
				hub.setVersion(attribs.get(INFField.VE));
			}
		}
	}
	
	public static void sendINF(Hub hub, boolean forcenew) {
		Map<INFField,String> last ;
		synchronized (LAST_INF) {
			last = LAST_INF.get(hub.getFavHub());
			if (last == null) {
				last = new LinkedHashMap<INFField,String>();
				LAST_INF.put(hub.getFavHub(), last);
			}
		}
		synchronized(last) {
			if (forcenew) {
				last.clear();
			}
			
			Map<INFField,String> next = new LinkedHashMap<INFField,String>();
			User self = hub.getSelf();
			//BINF 7KJB IDPUK62XDM5GLHOJ4NZ6KCFUGPEW5B3FKC4HMKRSA PDEZROSKSJ54SNWZFEIECDFAH5IGJRYMM5SZ2RWHQ 
			//NIQuicksilver SL4 SS695217057756 SF9712 HN1 HR0 HO0 VE++\s0.704 US5242 SUADC0
			List<INFField> fields = new ArrayList<INFField>(Arrays.asList(INFField.ID,INFField.PD,
										INFField.NI,INFField.SL , 
										INFField.SS,INFField.SF ,INFField.HN,INFField.HR,
										INFField.HO, INFField.VE,INFField.SU,INFField.US,INFField.EM));
			

			Identity id = hub.getIdentity();
			if (id.isActive()) {
				fields.add(INFField.U4);
			}  else {
				checkAdd(next,last,INFField.U4,"");
			}
			
			if (self.getKeyPrint() != null) {
				fields.add(INFField.KP);
			} else {
				checkAdd(next,last,INFField.KP,"");
			}
			if (id.isIPv6Used()) {
				fields.add(INFField.I6);
				fields.add(INFField.U6);
			} else {
				checkAdd(next,last,INFField.I6,"");
				checkAdd(next,last,INFField.U6,"");
			}

			for (INFField f: fields ) { //  currently
				String prop = f.getProperty(self);
				checkAdd(next,last,f,prop);
			}
			
			if (forcenew && id.isActive() && id.isIPv4Used()) { //on first connect we also add I4 so we get our IP set from hub if active
				String address;
				if (id.getConnectionDeterminator().isExternalIPSetByHand()) {
					address = id.getConnectionDeterminator().getPublicIP().getHostAddress();
				} else {
					address =  "0.0.0.0"; //:"::0";
				}
				next.put(INFField.I4, address );
				
			}
			
			
			next.entrySet().removeAll(last.entrySet()); //remove all duplicate info
			for (Entry<INFField,String> e :next.entrySet()) {//add to last all new Info
				last.put(e.getKey(), e.getValue());
			}
			
			if (!next.isEmpty()) {
				String inf = "BINF "+SIDToStr(self.getSid());
				inf += reverseINFMap( next);
				
				hub.sendUnmodifiedRaw(inf + "\n");
			}
		}
	}
	
	private static void checkAdd(Map<INFField,String> next,Map<INFField,String> check,INFField add,String val) {
		if (!GH.isEmpty(val) || check.containsKey(add)) {
			next.put(add,  val);
		}
	}

	


}
