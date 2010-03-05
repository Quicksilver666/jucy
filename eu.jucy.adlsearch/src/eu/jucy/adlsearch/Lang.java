package eu.jucy.adlsearch;

import uc.DCClient;
import helpers.NLS;


/**
* This is a automatically generated file! DO NOT CHANGE!!
* see eu.jucy.releng.languages -> CreateLangFile for more details
*/
public class Lang {

	public static String
		 ADLSearch
		,ADL_ADLSearch
		,ADL_Active
		,ADL_Directory
		,ADL_DownloadMatches
		,ADL_Filename
		,ADL_FullPath
		,ADL_MaxSize
		,ADL_MinSize
		,ADL_SearchString
		,ADL_SearchType
		,ADL_SizeType
		,ADL_TargetFolder ;
	
	static {
		try {
			NLS.load("nl.adl", Lang.class);
		} catch(RuntimeException re) {
			DCClient.logger.warn(re,re);
		}
	}
}