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

	EQUAL("=",LanguageKeys.Equals,"EQ"),ATLEAST("≥",LanguageKeys.AtLeast,"GE"), ATMOST("≤",LanguageKeys.AtMost,"LE");
	
	ComparisonEnum(String comp,String lang,String adc){
		this.comp= comp;
		this.trans = lang;
		this.adcCC = adc;
	}
	/**
	 * the mathematical sing for the comparisson
	 */
	private final String comp;
	private final String trans;
	private final String adcCC;
	
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
	
	
}
