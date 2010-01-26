package uc.crypto;


import helpers.GH;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.List;


import javax.crypto.Cipher;


import javax.crypto.spec.SecretKeySpec;

import uc.protocols.DCProtocol;



public class UDPEncryption {
	
	//private static final Logger logger = LoggerFactory.make();
	
	public static final short KEYLENGTH = 16;
	
	
	
	private static final Cipher cipher;
	
	static {
		Cipher c = null;
		try {
			c = Cipher.getInstance("AES/ECB/PKCS5Padding");
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} 
		cipher = c;
	}
	
	public static boolean isUDPEncryptionSupported() {
		return cipher != null;
	}
	
	public static void main(String... args) throws IOException,Exception {
		byte[] keyBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
	        0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
		
		byte[] message = "hello world".getBytes();
		byte[] cypher = encryptMessage(message,keyBytes);
		byte[] decrypted = decryptMessage(cypher, keyBytes);
		System.out.println(new String(decrypted));
	}
	
	public static byte[] encryptMessage(byte[] input,byte[] keyBytes) throws GeneralSecurityException {
		

		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
	    System.out.println(new String(input));

	    // encryption pass
	    synchronized (cipher) {
		    cipher.init(Cipher.ENCRYPT_MODE, key);
	
		    byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
		    int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
		    
		    ctLength += cipher.doFinal(cipherText, ctLength);
			return cipherText;
	    }
	}
	
	public static byte[] decryptMessage(byte[] encrypted,byte[] keyBytes) throws GeneralSecurityException {
		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

	    // decryption pass
		synchronized (cipher) {
		    cipher.init(Cipher.DECRYPT_MODE, key);
		    byte[] plainText = new byte[cipher.getOutputSize(encrypted.length)];
		    int ptLength = cipher.update(encrypted, 0, encrypted.length, plainText, 0);
		    ptLength += cipher.doFinal(plainText, ptLength);
		    
		    byte[] ret = GH.subarray(plainText, 0, ptLength);
		    return ret;
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
				return decrypted;
			} catch(GeneralSecurityException gse) {} 
		}
		return null;
	}
	
	public static byte[] tokenStringToKey(String token)  {
		try {
			byte[] bytes = token.getBytes(DCProtocol.ADCCHARSET.name());
			byte[] keyFull = Tiger.tigerOfBytes(bytes).getRaw();
			byte[] key = GH.subarray(keyFull, 0, UDPEncryption.KEYLENGTH);
			return key;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		
	}
	
}
