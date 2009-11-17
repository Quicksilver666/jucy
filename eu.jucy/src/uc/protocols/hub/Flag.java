package uc.protocols.hub;

import helpers.GH;

import java.util.HashSet;
import java.util.Set;

import uc.protocols.IProtocolCommand;

/**
 * ADC flags 
 * 
 * @author Quicksilver
 *
 */
public enum Flag {
	I4,I6, //IPs
	FC, //FourCharacterode
	FB,FM,  //Field Bad/Missing in inf
	TL,   //Tempban time in 
	MS,  //Message Sent .. together with the Quit
	ID,  // SID of MEssage Sender in Quit
	RD,  //Redirect address
	
	//Search flags
	AN,NO,EX, //Searchterm AN normal (and) , NO exclude (and not) , EX  extension (or)
	LE,GE,EQ,		// LEss , Greater, equals in bytes
	TO,				//Token used in result for identification / or in FileTransfer CTMs
	TY,				//Fileytype 1 = File 2 = Directory  not present all
	TR,				//Tiger in Search
	
	PR,				//Protocol
	
	PM,				//Private Message in MSG
	ME,				//1 if its "/me"msg
	
	// cmd 
	CT,  //context
	RM, //Remove
	SP, //Seperator
	TT, //to send
	CO,	//constrained..
	
	//SearchResult stuff
	FN,				//Full Filename
	SI,				//filesize
	SL,				//current slots
	RF,				//Referred.. used in client client STA for giving DDOsed hosts a chance to find originator
	
	//BLOM
	BK,		//magic K and magic H  for the Blom filter..
	BH,
	
	//user
	QP 		//which position in the queue one has..
	; 
	
	private static final Set<String> Flags;
	static {
		Flags = new HashSet<String>();
		for (Flag inf: Flag.values()) {
			Flags.add(inf.name());
		}
	}
	
	public static Flag parse(String flagchars) {
		try {
			if (Flags.contains(flagchars.toUpperCase())) {
				return Flag.valueOf(flagchars.toUpperCase());
			}
		} catch(IllegalArgumentException iae) {
			AbstractADCHubCommand.logger.debug("new INF FIELD Found: "+flagchars,iae);
		}
		return null;
	}
	
	
	public boolean verify(String value) {
		switch(this) {
		case I4: return value.matches(IProtocolCommand.IPv4);
		case FC: return value.length() == 4;
		case TL: //also a long number therefore FileSize..
		case SI:
		case LE:
		case GE:
		case CT:
		case BK:
		case BH:
		case EQ: return value.matches(IProtocolCommand.FILESIZE);
		case TY: return value.matches("[12]");
		case SP:
		case CO:
		case ME: return value.equals("1");
				//values that just shouldn't be empty
		case TT:
		case RD:
		case FB:
		case FN:
		case PR:
		case FM: return !GH.isEmpty(value);
		case PM:
		case ID: return value.matches(IProtocolCommand.SID);
		case TR: return value.matches(IProtocolCommand.TTH);
		case QP:
		case SL: return value.matches(IProtocolCommand.SHORT);
		case AN:
		case NO:
		case RF:
		case RM:
		case EX: return true; //allways true for these..
		
		}
		
		return true;
	}
}