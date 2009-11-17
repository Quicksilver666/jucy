package uc.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

public class UDPEncryption {

	//TODO
	public static void main(String... args) throws IOException,Exception {
		File keyFile = new File("public.der");        
		byte[] encodedKey = new byte[(int)keyFile.length()];

		new FileInputStream(keyFile).read(encodedKey);

		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);

		KeyFactory kf = KeyFactory.getInstance("RSA");
		PublicKey pk = kf.generatePublic(publicKeySpec);

		Cipher rsa = Cipher.getInstance("RSA");

		rsa.init(Cipher.ENCRYPT_MODE, pk);
		OutputStream os = new CipherOutputStream(
		        new FileOutputStream("encrypted.rsa"), rsa);

		Writer out = new OutputStreamWriter(os);
		out.write("Hello World!!");
		out.close();
		os.close();
	}
	
	public void test() throws Exception {
		KeyPairGenerator keyGen =
		    KeyPairGenerator.getInstance("DSA", "SUN");

	}
	
}
