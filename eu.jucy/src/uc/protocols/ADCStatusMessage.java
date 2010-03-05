package uc.protocols;

import helpers.SizeEnum;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

import uc.LanguageKeys;
import uc.protocols.hub.Flag;


public class ADCStatusMessage {
	
	/**
	 * severity codes
	 */
	public static final int SUCCESS = 0, RECOVERABLE = 1, FATAL = 2;

	public static final int
	Generic = 0,
	HubGeneric = 10,
	HubIsFull = 11,
	HubDisabled = 12,
	UserGeneric = 20,
	UserNickInvalid=21,
	UserNickTaken = 22,
	UserInvalidPassword = 23,
	UserCIDTaken = 24,
	UserAccessToCommandDenied = 25,
	UserRegisteredUsersOnly = 26,
	UserInvalidPIDSupplied = 27,
	KicksBansGeneric	= 30,
	KicksBansPermanentlyBanned = 31,
	KicksBansTemporarilyBanned = 32,
	ProtocolGeneric	= 40,
	ProtocolTransferProtocolUnsupported = 41 , 
	ProtocolDirectConnectionFailed = 42,
	ProtocolRequiredINFfieldBadMissing = 43,
	ProtocolInvalidState = 44,
	ProtocolRequiredFeatureMissing = 45,
	ProtocolInvalidIPSupplied = 46,
	ProtocolNoHashSupportOverlap = 47,
	TransferGeneric	= 50,
	TransferFileNotAvailable = 51,
	TransferFilePartNotAvailable = 52,
	TransferSlotsFull = 53,
	TransferNoHashSupportOverlap = 54;
	
	
	private final int severity;
	private final int type;
	private final String message;
	/**
	 * additional information with the status message used for sending
	 */
	private final Map<Flag,String> flags;
	
	
	public ADCStatusMessage(String message, int severity, int type) {
		this(message,severity,type,Collections.<Flag,String>emptyMap());
	}
	
	/**
	 * convenience constructor if a single map value is needed..
	 */
	public ADCStatusMessage(String message, int severity, int type,Flag flag,String value) {
		this(message,severity,type,Collections.<Flag,String>singletonMap(flag, value));
	}
	
	public ADCStatusMessage(String message, int severity, int type,Map<Flag,String> flags) {
		if (severity < 0 || severity > 2) {
			throw new IllegalArgumentException("severity not in {0,1,2}");
		}
		
		this.message = message;
		this.severity = severity;
		this.type = type;
		this.flags = flags;
	}
	

	public String getTypeMessage() {
		switch(type) {
		case Generic: //00 	Generic, show description
		case HubGeneric:
		case UserGeneric:
		case KicksBansGeneric:
		case ProtocolGeneric:
		case TransferGeneric:
			return null;
		case HubIsFull:
			return LanguageKeys.STA11HubFull;
		case HubDisabled:
			return LanguageKeys.STA12HubDisabled;
		case UserNickInvalid:
			return LanguageKeys.STA21NickInvalid;
		case UserNickTaken:
			return LanguageKeys.STA22NickTaken;
		case UserInvalidPassword:
			return LanguageKeys.STA23InvalidPassword;
		case UserCIDTaken:
			return LanguageKeys.STA24CIDtaken;
		case UserAccessToCommandDenied:
			return String.format(LanguageKeys.STA25AccessToCommandDenied,flags.get(Flag.FC));
		case UserRegisteredUsersOnly:
			return LanguageKeys.STA26RegisteredUsersOnly;
		case UserInvalidPIDSupplied:
			return LanguageKeys.STA27InvalidPIDSupplied;
		case KicksBansPermanentlyBanned:
			return LanguageKeys.STA31PermanentlyBanned;
		case KicksBansTemporarilyBanned:
			long time = Long.valueOf(flags.get(Flag.TL));
			return String.format(LanguageKeys.STA32TemporarilyBanned,SizeEnum.timeEstimation(time));
		case ProtocolTransferProtocolUnsupported:
			//maybe not send message?
			return LanguageKeys.STA41TransferProtocolUnsupported; 
		case ProtocolDirectConnectionFailed:
			return LanguageKeys.STA42DirectConnectionFailed;
		case ProtocolRequiredINFfieldBadMissing:
			if (flags.containsKey(Flag.FM)) {
				return String.format(LanguageKeys.STA43RequiredINFfieldMissing,flags.get(Flag.FM));
			} else if (flags.containsKey(Flag.FB)) {
				return String.format(LanguageKeys.STA43RequiredINFfieldBad,flags.get(Flag.FB));
			}
			break;
		case ProtocolInvalidState:
			return String.format(LanguageKeys.STA44InvalidState,flags.get(Flag.FC));
		case ProtocolRequiredFeatureMissing:
			return String.format(LanguageKeys.STA45RequiredFeatureMissing,flags.get(Flag.FC));
		case ProtocolInvalidIPSupplied: // Invalid IP
			InetAddress ip = null;
			try {
			if (flags.containsKey(Flag.I4)) {
				ip = InetAddress.getByName(flags.get(Flag.I4));
			} else if (flags.containsKey(Flag.I6)) {
				ip = InetAddress.getByName(flags.get(Flag.I6));
			}
			} catch (UnknownHostException uhe) {
				ip = null;
			}
			
			if (ip != null) {
			//	hub.getSelf().setIp(ip);
				return String.format( LanguageKeys.STA46InvalidIPSupplied,ip.getHostAddress());
			}
			break;
		case ProtocolNoHashSupportOverlap:
			return LanguageKeys.STA47NoHashSupportOverlapHub;
		case TransferFileNotAvailable:
			return LanguageKeys.STA51FileNotAvailable;
		case TransferFilePartNotAvailable:
			return LanguageKeys.STA52FilePartNotAvailable;
		case TransferSlotsFull:
			return LanguageKeys.STA53SlotsFull;
		case TransferNoHashSupportOverlap:
			return LanguageKeys.STA54NoHashSupportOverlapClient;
		}
		return null;
	}
	
	public int getSeverity() {
		return severity;
	}

	public int getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		String s = getTypeMessage();
		if (s != null) {
			return s;
		} else {
			return message;
		}
	}
	
	public String toADCString() {
		return severity+ String.format("%02d",type)+" "+
			AbstractADCCommand.doReplaces(message)+
			AbstractADCCommand.getFlagString(flags);
	}
	

	
	
}
