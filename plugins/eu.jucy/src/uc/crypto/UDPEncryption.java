package uc.crypto;


import helpers.GH;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.List;


import javax.crypto.Cipher;


import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import uc.protocols.DCProtocol;





public class UDPEncryption {
	
	//private static final Logger logger = LoggerFactory.make();
	
	public static final short KEYLENGTH = 16;
	private static final SecureRandom SRAND = new SecureRandom();
	private static final Cipher CIPHER;
	
	private static final byte[] start;
	
	static {
		Cipher d = null;
		
		try {
			d = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} 
		CIPHER = d;
		start = "URES ".getBytes(DCProtocol.ADC_CHARSET);
	}
	
	public static boolean isUDPEncryptionSupported() {
		return CIPHER != null;
	}
	
//	public static void main(String... args) throws IOException,Exception {
//		byte[] keyBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
//	        0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
//		
//		byte[] message = "hello world".getBytes();
//		byte[] cypher = encryptMessage(message,keyBytes);
//		System.out.println("Message size: "+message.length+ " cipherlength: "+cypher.length +"  "+new String(cypher));
//		byte[] decrypted = decryptMessage(cypher, keyBytes);
//		System.out.println(new String(decrypted));
//	}
	
	public static byte[] encryptMessage(byte[] input,byte[] keyBytes) throws GeneralSecurityException {
		if (keyBytes.length != KEYLENGTH) {
			throw new IllegalArgumentException();
		}

		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
		
	  //  System.out.println(new String(input));
	    
	    byte[] ivBytes = new byte[KEYLENGTH] ;
	    synchronized (SRAND) {
	    	SRAND.nextBytes(ivBytes);
	    }
	    IvParameterSpec iv = new IvParameterSpec(new byte[KEYLENGTH]);
	    byte[] packetToEncrypt = GH.concatenate(ivBytes,input);
//	    // encryption pass
//	    byte[] encryptedIV;
//	    synchronized (CIPHER_IV) {
//	    	CIPHER_IV.init(Cipher.ENCRYPT_MODE, key);
//	
//	    	byte[] encryptedIVwork = new byte[CIPHER_IV.getOutputSize(ivBytes.length)];
//		    
//		    int ctLength = CIPHER_IV.update(ivBytes, 0, ivBytes.length, encryptedIVwork, 0);
//		    ctLength += CIPHER_IV.doFinal(encryptedIVwork, ctLength);
//		    encryptedIV = GH.subarray(encryptedIVwork, 0, ctLength);
//	    }
	    
	    byte[] encryptedPacket;
	    synchronized (CIPHER) {
	    	CIPHER.init(Cipher.ENCRYPT_MODE, key, iv);
	    	 byte[] encryptedPacketWork = new byte[CIPHER.getOutputSize(packetToEncrypt.length)];
			 int ctLength = CIPHER.update(packetToEncrypt, 0, packetToEncrypt.length, encryptedPacketWork, 0);
			 ctLength += CIPHER.doFinal(encryptedPacketWork, ctLength);
			 encryptedPacket = GH.subarray(encryptedPacketWork, 0, ctLength); 
	    }
	    return encryptedPacket;
	}
	
	public static byte[] decryptMessage(byte[] encrypted,byte[] keyBytes) throws GeneralSecurityException {
		if (keyBytes.length != KEYLENGTH || encrypted.length < KEYLENGTH*2) {
			throw new IllegalArgumentException();
		}
		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

		IvParameterSpec iv =  new IvParameterSpec(new byte[KEYLENGTH]);

		synchronized (CIPHER) {
			CIPHER.init(Cipher.DECRYPT_MODE, key,iv);
		    byte[] plainPacket = new byte[CIPHER.getOutputSize(encrypted.length)];
		    int ptLength = CIPHER.update(encrypted, 0, encrypted.length, plainPacket, 0);
		    ptLength += CIPHER.doFinal(plainPacket, ptLength);
		    return GH.subarray(plainPacket, KEYLENGTH, ptLength-KEYLENGTH);
		}
	}
	
	/**
	 * tries out all provided key on the message..
	 * if the message is encrypted this will return the decrypted message ...
	 * null otherwise
	 * 
	 * @param potentialEncrypted - the message that might or might not be encrypted
	 * @param keys - the keys that could be tried
	 * @return 
	 */
	public static byte[] decryptMessage(byte[] potentialEncrypted,List<byte[]> keys) {
		for (byte[] key: keys) {
			try {
				byte[] decrypted = decryptMessage(potentialEncrypted, key);
				boolean matches = true;
				for (int i = 0; matches && i < start.length ; i++) {
					matches = decrypted[i] == start[i];
				}
				if (matches) {
					return decrypted;
				}
			} catch(GeneralSecurityException gse) {} 
		}
		return null;
	}
	
	
	public static byte[] getRandomKey() {
		byte[] encryptionKey = new byte[KEYLENGTH];
		synchronized(SRAND) {
			SRAND.nextBytes(encryptionKey);
		}
		return encryptionKey;
	}
	
}
