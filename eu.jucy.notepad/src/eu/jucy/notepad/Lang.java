package eu.jucy.notepad;

import uc.DCClient;
import helpers.NLS;


/**
* This is a automatically generated file! DO NOT CHANGE!!
* see eu.jucy.releng.languages -> CreateLangFile for more details
*/
public class Lang {

	public static String
		 Bundle_Name
		,NotepadInput
		,command_description
		,command_name
		,editor_name ;
	
	static {
		try {
			NLS.load("nl.notepad", Lang.class);
		} catch(RuntimeException re) {
			DCClient.logger.warn(re,re);
		}
	}
}