package uc.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256HashValue extends HashValue {

	/**
	 * the length of the digest in bytes
	 */
	public static final int digestlength = 32;  
	

	
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



	@Override
	public SHA256HashValue hashOfHash() {
		return hashData(getRaw());
	}

	@Override
	public String magnetString() {
		return "sha256";
	}
	
	public static SHA256HashValue hashData(byte[] data) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance( "sha-256" );
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		byte[] raw = md.digest(data);
		
		return new SHA256HashValue(raw);
	}

}
