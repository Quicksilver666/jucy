/**
 * 
 */
package uc.protocols.hub;


import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;


import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import uc.IUser;

import uc.crypto.HashValue;
import uc.protocols.IProtocolCommand;

public enum INFField {

	ID, //CID
	PD, //PID we will never see this  hash(PID) == CID
	I4,I6, //IPs
	U4,U6,  // UDP ports
	SS, //Sharesize
	SF, //Shared Files
	VE, //Version
	US, //Maximum upload speed, bytes/second 
	DS, //Maximum download speed, bytes/second 
	SL, //Slots
	AS, //Automatic slot allocator speed limit, bytes/sec. The client keeps opening slots as long as its total upload speed doesn't exceed this value. 
	AM, //Minimum simultaneous upload connectins in automatic slot manager mode 
	EM, //E-mail address 
	NI, //Nick
	DE, //Description
	HN,HR,HO, //Hubs  norm/Reg/Op 
	TO, //TOKEN in  (in client client connection)
	CT, // client type .. user , OP , Bot..
	AW, //1=Away, 2=Extended away, not interested in hub chat (hubs may skip sending broadcast type MSG commands to clients with this flag) 
	SU, //Supports  	 Comma-separated list of feature FOURCC's. This notifies other clients of extended capabilities of the connecting client. 
	RF,  // URL of referrer (hub in case of redirect, web page) 
	KP	//Keyprint
	;
	
	private static final Set<String> Fields;
	static {
		Fields = new HashSet<String>();
		for (INFField inf: INFField.values()) {
			Fields.add(inf.name());
		}
	}
		
	private static Logger logger = LoggerFactory.make(Level.DEBUG);
	


	public static INFField parse(String infChars) {
		try {
			if (Fields.contains(infChars.toUpperCase())) {
				return INFField.valueOf(infChars.toUpperCase());
			}
		} catch(IllegalArgumentException iae) {
			logger.debug("new INF FIELD Found: "+infChars,iae);
		}
		return null;
	}
	
	public String getProperty(IUser usr) {
		switch(this) {
		case ID: 
			HashValue hash = usr.getCID();
			if (hash == null) {
				return "";
			} else {
				return hash.toString();
			}
		case I4:
			InetAddress ia4 = usr.getIp();
			if (ia4 instanceof Inet4Address) {
				return ia4.getHostAddress();
			}
			return "";
		case I6:
			InetAddress ia6 = usr.getIp();
			if (ia6 instanceof Inet6Address) {
				return ia6.getHostAddress();
			}
			return "";
		case SS:
			return ""+usr.getShared();
		case SF:
			return ""+usr.getNumberOfSharedFiles();
		case SL:
			return ""+usr.getSlots();
		case EM:
			return usr.getEMail();
		case NI:
			return usr.getNick();
		case DE:
			return usr.getDescription();
		case HN:
			return ""+usr.getNormHubs();
		case HR:
			return ""+usr.getRegHubs();
		case HO:
			return ""+usr.getOpHubs();
		case CT:
			return ""+usr.getCt();
		case AW: //Away mode
			return ""+usr.getAwayMode().getValue();
		case SU:
			return usr.getSupports();
		case AM:
			return ""+usr.getAm();
		case AS:
			return ""+usr.getAs();
		case DS:
			return ""+usr.getDs();
		case US:
			return ""+usr.getUs();
		case U4:
		case U6:
			return ""+usr.getUdpPort();
		case PD: //PID is never sent to us, only interesting for the hub
			return usr.getPD().toString();
		case RF: //Referrer field is also only interesting for the hub
			break;
	/*	case TO: //TOKEN for CTM
			break; */
		case VE:
			return usr.getVersion();
		case KP: 
			HashValue kp = usr.getKeyPrint();
			if (kp != null) {
				return kp.magnetString().toUpperCase()+"/"+kp.toString();
			}
			break;
		}
		
		return null;
	}
	
	public boolean verify(String value) {
		switch(this) {
		case I4: return value.matches(IProtocolCommand.IPv4);
		case ID:
		case PD: return value.matches(IProtocolCommand.TTH);
		case HN:   //All positive integer numbers
		case HO:
		case HR:               
		case SL: return value.matches(IProtocolCommand.SHORT);
		case AM:
		case CT: return value.matches(IProtocolCommand.BYTE);
		case AS:
		case SF: return value.matches(IProtocolCommand.INT);
		case U4:
		case U6: 
			return  value.matches(IProtocolCommand.PORT);
		case DS:
		case SS:
		case US: return value.matches(IProtocolCommand.FILESIZE); 
		case AW: return value.matches("[012]");
		case KP: return value.matches(IProtocolCommand.HASH_WITH_TYPE);
		
		}
		
		return true; //all other are undefined.. and therefore correct..
	}
	
}