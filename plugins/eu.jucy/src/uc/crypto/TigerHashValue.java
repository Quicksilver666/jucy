package uc.crypto;

import java.security.SecureRandom;
import java.util.Random;

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
		if (what.startsWith("TTH/")) {
			what = what.substring(4);
		}
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
	

	@Override
	public HashValue internalHash(HashValue rightChild) {
		return Tiger.internalHash(this, rightChild);
	}



	public static void main(String[] args) {
		byte[] startWithCID = BASE32Encoder.decode("JUCYR"); 
		byte[] startWithPID = BASE32Encoder.decode("LOVEA"); 
		int maxLengthFound = 0;
		Random rand = new SecureRandom();
		byte[] hash = new byte[digestlength];
		TigerHashValue pid = new TigerHashValue(hash);
		byte[] cid = null;
		while (maxLengthFound < startWithCID.length) {
			rand.nextBytes(hash);
			for (int x = 0; x < startWithPID.length;x++) {
				hash[x] = startWithPID[x];
			}
			cid = pid.hashOfHash().getRaw();
			int i = 0;
			for (i=0; i < startWithCID.length;i++) {
				if (cid[i] != startWithCID[i]) {
					if (i > maxLengthFound) {
						System.out.println("PID :"+ BASE32Encoder.encode(hash)+"  CID:"+BASE32Encoder.encode(cid));
						maxLengthFound = i;
					} 
					break;
				}
			}
			if (i == startWithCID.length) {
				break;
			}
		}
		System.out.println("fin: PID :"+ BASE32Encoder.encode(hash)+"  CID:"+BASE32Encoder.encode(cid));
	}
	
	

}
