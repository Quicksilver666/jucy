package uc.files.search;

import eu.jucy.language.LanguageKeys;


/**
 * 
 * comparison .. used for Searches
 * 
 * @author Quicksilver
 *
 */
public enum ComparisonEnum {

	EQUAL("=",LanguageKeys.Equals,"EQ",'F'),ATLEAST("≥",LanguageKeys.AtLeast,"GE",'F'), ATMOST("≤",LanguageKeys.AtMost,"LE",'T');
	
	ComparisonEnum(String comp,String lang,String adc,char nmdcc){
		this.comp= comp;
		this.trans = lang;
		this.adcCC = adc;
		this.nmdcc= nmdcc;
	}
	/**
	 * the mathematical sing for the comparisson
	 */
	private final String comp;
	private final String trans;
	private final String adcCC;
	private final char nmdcc;
	
	/**
	 * 
	 * @return the adc character code for the comparison
	 * EQ ==
	 * GE >=
	 * LE <=
	 */
	public String getAdcCC() {
		return adcCC;
	}

	public String toString(){
		return trans;
	}

	public String getComp() {
		return comp;
	}
	
	public char getNMDCC() {
		return nmdcc;
	}
	
	
}
