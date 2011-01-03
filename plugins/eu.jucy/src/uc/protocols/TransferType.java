package uc.protocols;



/**
 * Enum for the type of transfer ..
 * FileList or a normal FileList
 * TTHL : interleaves..
 * File .. for a normal file.
 * 
 * @author Quicksilver
 *
 */
public enum TransferType implements Comparable<TransferType> {
	
	/**
	 * order of declaration is important as
	 * this will be used to download TTHLs first
	 * and Files last after filelists
	 */
	FILE("file"),FILELIST("file","list"),TTHL("tthl");
	
	
	private final String nmdcstring;

	private final String adcString;
	

	private TransferType(String nmdcstring) {
		this(nmdcstring,nmdcstring);
	}
	private TransferType(String nmdcstring,String adcString) {
		this.nmdcstring = nmdcstring;
		this.adcString = adcString;
	}
	
	public String toNMDCString() {
		return nmdcstring;
	}
	
	public String getAdcString() {
		return adcString;
	}
}


