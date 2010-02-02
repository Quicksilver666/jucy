package uc.crypto;

public class TigerHashValue extends HashValue {


	/**
	 * the zero byte TTH! HashValue 
	 */
	public static final TigerHashValue ZEROBYTEHASH = new TigerHashValue("LWPNACQDBZRYXW3VHJVCJ64QBZNGHOHHHZWCLNQ");
	

	/**
	 * a reqexp describing how a TTH in Base32 has to look
	 */
	public static final String TTHREGEX = "(?:[A-Z2-7]{38}[QYAI])";
	
	
	/**
	 * the length of the digest in bytes
	 */
	public static final int digestlength = 24;  
	

	
	public static final int serializedDigestLength = 39;
	
	public static boolean isTTH(String what) {
		return what.length() == 39 && what.matches(TTHREGEX);
	}
	

	
	TigerHashValue(String base32value) {
		super(base32value);
		if (base32value.length() != serializedDigestLength ) {
			throw new IllegalArgumentException("bad length for a tiger value");
		}
	}

	public TigerHashValue(byte[] value) {
		super(value);
		if (value.length != digestlength) {
			throw new IllegalArgumentException("bad length for a tiger value");
		}
	}


	

	@Override
	public String magnetString() {
		return "tiger";
	}



	@Override
	public HashValue hashOfHash() {
		return Tiger.tigerOfHash(this);
	}
	
	

}
