package uc.crypto;

import helpers.GH;

public class SHA256HashValue extends HashValue {

	/**
	 * the length of the digest in bytes
	 */
	public static final int digestlength = 32;  
	
	public static final String SHA256REGEX = "(?:[A-Z2-7]{52})";

	
	public static final int serializedDigestLength = 52;
	
	
	public SHA256HashValue(byte[] value) {
		super(value);
		if (value.length != digestlength) {
			throw new IllegalArgumentException("bad length for a sha256 value");
		}
	}
	
	
	
	public SHA256HashValue(String base32value) {
		super(base32value);
		if (base32value.length() != serializedDigestLength ) {
			throw new IllegalArgumentException("bad length for a sha256 value "+base32value.length());
		}
	}

	public static boolean isSHA256HashValue(String hash) {
		if (hash.startsWith("SHA256/")) {
			hash = hash.substring(7);
		}
		return hash.length() == serializedDigestLength && hash.matches(SHA256REGEX);
	}

	@Override
	public SHA256HashValue hashOfHash() {
		return hashData(getRaw());
	}

	@Override
	public String magnetString() {
		return "sha256";
	}
	
	public static SHA256HashValue hashData(byte[] data) {
		byte[] raw = GH.getHash(data, "sha-256"); 
		return new SHA256HashValue(raw);
	}

}
