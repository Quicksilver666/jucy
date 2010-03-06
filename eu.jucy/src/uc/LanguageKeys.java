package uc;

import uc.DCClient;
import helpers.NLS;


/**
* This is a automatically generated file! DO NOT CHANGE!!
* see eu.jucy.releng.languages -> CreateLangFile for more details
*/
public class LanguageKeys {

	public static String
		 AddressCouldNotBeResolved
		,AtLeast
		,AtMost
		,CDCheckFirewall
		,CDCheckForwardedPorts
		,CDCheckTCP
		,CDCheckUDP
		,CDDeterminedIPOverWeb
		,CDUPnPNotPresent
		,CDUPnPNotWorking
		,Closed
		,Connected
		,Connecting
		,ConnectionTimeout
		,Disconnected
		,Equals
		,FilelistRefreshAlreadyInProgress
		,FinishedFilelistRefresh
		,HubRequestedPassword
		,Idle
		,LoggedIn
		,LoginTimeout
		,MatchedXFilesWithUserY
		,Reconnecting
		,RedirectReceived
		,STA11HubFull
		,STA12HubDisabled
		,STA21NickInvalid
		,STA22NickTaken
		,STA23InvalidPassword
		,STA24CIDtaken
		,STA25AccessToCommandDenied
		,STA26RegisteredUsersOnly
		,STA27InvalidPIDSupplied
		,STA31PermanentlyBanned
		,STA32TemporarilyBanned
		,STA41TransferProtocolUnsupported
		,STA42DirectConnectionFailed
		,STA43RequiredINFfieldBad
		,STA43RequiredINFfieldMissing
		,STA44InvalidState
		,STA45RequiredFeatureMissing
		,STA46InvalidIPSupplied
		,STA47NoHashSupportOverlapHub
		,STA51FileNotAvailable
		,STA52FilePartNotAvailable
		,STA53SlotsFull
		,STA54NoHashSupportOverlapClient
		,SendingPassword
		,StartedRefreshingTheFilelist
		,TransferFinished
		,UserConnected
		,UserDisconnected
		,UserLeft
		,UserOffline
		,WaitingForConnect ;
	
	static {
		try {
			NLS.load("nl.language", LanguageKeys.class);
		} catch(RuntimeException re) {
			DCClient.logger.warn(re,re);
		}
	}
}