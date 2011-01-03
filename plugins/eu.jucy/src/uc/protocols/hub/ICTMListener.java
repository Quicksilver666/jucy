package uc.protocols.hub;

import java.net.InetSocketAddress;

import uc.IUser;
import uc.protocols.CPType;

public interface ICTMListener {
	
	/**
	 * 
	 * @param self - the user we are in the hub
	 * @param isa - the others IP and port
	 * @param other - null in NMDC
	 * @param protocol - what protocol to be used ("NMDC" for NMDC "ADC/1.0" for ADC or "ADCS/0.10" for ADCS) 
	 */
	void ctmReceived(IUser self , InetSocketAddress isa, IUser other,CPType protocol,String token);
	
	
	void ctmSent(IUser target, CPType protocol,String token);
}
