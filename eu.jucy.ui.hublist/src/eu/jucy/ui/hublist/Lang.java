package eu.jucy.ui.hublist;

import uc.DCClient;
import helpers.NLS;


/**
* This is a automatically generated file! DO NOT CHANGE!!
* see eu.jucy.releng.languages -> CreateLangFile for more details
*/
public class Lang {

	public static String
		 ConfigurePublicHubLists
		,EnterAddressOfTheHublist
		,HLHubs
		,HLUsers
		,Hublist
		,LoadHublist
		,PublicHubsList ;
	
	static {
		try {
			NLS.load("nl.publichubs", Lang.class);
		} catch(RuntimeException re) {
			DCClient.logger.warn(re,re);
		}
	}
}