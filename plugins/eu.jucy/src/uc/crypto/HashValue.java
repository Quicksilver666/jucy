package uc.crypto;

import helpers.GH;


import java.util.Arrays;


/**
 * 
 * A HashValue represents the digest of a Tiger hash
 * most times used with TigetTreeHash
 * 
 * @author Quicksilver
 *
 */
public abstract class HashValue implements Comparable<HashValue> {

	
	/**
	 * the data.. 
	 */
	private final byte[] hashValue;
	
	/**
	 * parses a base 32 string into a hashvalue object
	 * @param base32value
	 * @return
	 */
	public static HashValue createHash(String base32value) {
		if (base32value.length() < 20) {
			throw new IllegalStateException(); // too small for a hash
		}
		if (base32value.substring(0, 4).equalsIgnoreCase("TTH/")) {
			base32value = base32value.substring(4);
			return new TigerHashValue(base32value);
		}
		
		if (base32value.length() == TigerHashValue.serializedDigestLength) {
			return new TigerHashValue(base32value);
		}
		if (base32value.substring(0, 7).equalsIgnoreCase("sha256/")) {
			base32value = base32value.substring(7);
			return new SHA256HashValue(base32value);
		}
		
		if (base32value.length() == SHA256HashValue.serializedDigestLength) {
			return new SHA256HashValue(base32value);
		}
		
		
		throw new IllegalStateException("hash not parseable: len "+base32value.length());
	}
	
	public static HashValue createHash(byte[] data,String alg) {
		if (alg.equalsIgnoreCase("sha256") || alg.equalsIgnoreCase("sha-256")) {
			return SHA256HashValue.hashData(data);
		}
		if (alg.equalsIgnoreCase("tiger") || alg.equalsIgnoreCase("tigr")) {
			return Tiger.tigerOfBytes(data);
		}

		throw new IllegalStateException("unknown alg");
	}
	
	
	
	public static boolean isHash(String base32Value) {
		if (base32Value == null) {
			return false;
		}
		return TigerHashValue.isTTH(base32Value) || SHA256HashValue.isSHA256HashValue(base32Value);
	}
	
	public static boolean isHash(byte[] hashBytes) {
		return 	hashBytes != null 
				&& (hashBytes.length == TigerHashValue.digestlength 
					|| hashBytes.length == SHA256HashValue.digestlength);
	}
	
	
	public static HashValue createHash(byte[] bytes) {
		if (bytes.length == TigerHashValue.digestlength) {
			return new TigerHashValue(bytes);
		}
		throw new IllegalStateException();
	}
	
	/**
	 * creates the same hash from a base32 encoded String 
	 * 
	 * @param base32value
	 */
	protected HashValue(String base32value){
		hashValue = BASE32Encoder.decode(base32value);
	}
	
	/**
	 * 
	 * @param value - a HashValue as an array of length 24  (24*8 = 192Bit)
	 */
	protected HashValue(byte[] value){
		hashValue = value;
	}
	
	
	public HashValue copy() {
		byte[] retval = new byte[hashValue.length];
		System.arraycopy(hashValue, 0, retval, 0, hashValue.length);
		return createHash(retval);
	}
	
	/**
	 * @return a base32 encoded String of the HashValue
	 */
	public String toString(){
		return  BASE32Encoder.encode(hashValue);
	}
	
	/**
	 * 
	 * @return a string name for magnet links
	 * 
	 * i.e. "tiger" for Tiger hash
	 * so urn:tree:tiger:
	 */
	public abstract String magnetString();
	

	
	/**
	 * 
	 * @return the array containing the hash in raw form
	 * no copy..
	 * so changing this will change the hashvalue
	 */
	public byte[] getRaw() {
		return hashValue;
	}

	@Override
	public int hashCode() {
		return 
			 (hashValue[0] & 0xff)			| 
			((hashValue[1] & 0xff) << 8)	|
			((hashValue[2] & 0xff) << 16)	| 
			((hashValue[3] & 0xff) << 24) ;
	}
	
	/**
	 * @return hash of this hash
	 * hashofHash(h(x)) = h(h(x))
	 */
	public abstract HashValue hashOfHash();
	

	public abstract HashValue internalHash(HashValue rightChild);
	


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof HashValue))
			return false;
		final HashValue other = (HashValue) obj;
		if (!Arrays.equals(getRaw(), other.getRaw()))
			return false;
		return true;
	}

	/**
	 * comparing against other for sorting...
	 */
	public int compareTo(HashValue arg0) {
		for (int i=0; i < hashValue.length; i++) {
			if (hashValue[i] != arg0.hashValue[i]) {
				return GH.compareTo(hashValue[i] & 0xff  , arg0.hashValue[i] & 0xff);
			}
		}
		return 0;
	}

	
}
