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



import uc.DCClient;
import uc.FavHub;
import uc.User;
import uc.crypto.HashValue;
import uc.listener.IUserChangedListener.UserChange;
import uc.listener.IUserChangedListener.UserChangeEvent;
import uc.protocols.ADCStatusMessage;
import uc.protocols.AbstractConnection;
import uc.protocols.ConnectionState;
import uc.protocols.DCProtocol;


public class INF extends AbstractADCHubCommand {

	
	
	private static final Map<FavHub,Map<INFField,String>> LastINF =  
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
			if (hub.getSelf().getSid() == sid &&  ConnectionState.CONNECTED.equals(hub.getState()) ) {
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
				attribs.remove(INFField.ID); //ignore any ids we get.. TODO implement.. remove later..
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
				hub.statusMessage(attribs.get(INFField.VE), 0);
			}
		}
	}
	
	public static void sendINF(Hub hub, boolean forcenew) {
		DCClient dcc = hub.getDcc();
		Map<INFField,String> last ;
		synchronized (LastINF) {
			last = LastINF.get(hub.getFavHub());
			if (last == null) {
				last = new LinkedHashMap<INFField,String>();
				LastINF.put(hub.getFavHub(), last);
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
										INFField.HO, INFField.VE,INFField.SU,INFField.US));
			
			if (dcc.isActive()) {
				fields.add(INFField.U4);
			}
			if (AbstractConnection.getFingerPrint() != null) {
				fields.add(INFField.KP);
			}

			
			for (INFField f: fields ) { //  currently
				next.put(f,  f.getProperty(self));
			}
			if (forcenew && dcc.isActive()) { //on first connect we also add I4 so we get our IP set from hub if active
				boolean ipv4 = dcc.isIPv4Used();
				INFField which = ipv4? INFField.I4: INFField.I6;
				String address;
				if (dcc.getConnectionDeterminator().isExternalIPSetByHand()) {
					address = dcc.getConnectionDeterminator().getPublicIP().getHostAddress();
				} else {
					address = ipv4?  "0.0.0.0":"::0";
				}
				
				next.put(which, address );
			}
			
			next.entrySet().removeAll(last.entrySet()); //remove all duplicate info
			for (Entry<INFField,String> e :next.entrySet()) {//add to last all new Info
				last.put(e.getKey(), e.getValue());
			}
			
			if (!next.isEmpty()) {
				String inf = "BINF "+SIDToStr(self.getSid());
				inf += reverseINFMap( next);
				
				hub.sendUnmodifiedRaw(inf + "\n");
//				if (Platform.inDevelopmentMode()) {
//					logger.info("INF: "+inf);
//				}
			}
		}
	}
	


}
