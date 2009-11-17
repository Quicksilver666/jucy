package uc.crypto;

import helpers.GH;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.security.KeyStore;


import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;

import uc.PI;



/*
 * 
 * keytool -genkey -storepass 123456 -keystore storagename -keyalg RSA -dname "CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown" -validity 999
 */
public class KeyGenerator {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG); 

	
	
//	public static void main(String[] args) {
//		
//		/*for (Provider p:Security.getProviders()) {
//			System.out.println(p.getName());
//			for (Service s: p.getServices()) {
//				System.out.print(s.getAlgorithm()+",");
//			}
//			System.out.println();
//		}
//		for (Entry<Object,Object> e: System.getProperties().entrySet()) {
//			System.out.println(e.getKey()+" : "+e.getValue());
//		}
//		
//		System.out.println(get().length); */
//		System.out.println(loadManager().length);
//	} 
	
	
    
    public static KeyManager[] loadManager() {
    	FileInputStream fis = null;
		try {
			File f = new File(PI.getStoragePath()+File.separator+".keystore" );
			if (!f.isFile()) {
				
				logger.info("Creating Certificate...");
				genKeypair(f,"RSA");
				genKeypair(f,"DSA");
				logger.info("Created Certificate");
			} 
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
			
			char[] pass = new char[]{'1','2','3','4','5','6'};
			fis = new FileInputStream(f);
			store.load(fis, pass );
			kmf.init(store, pass);
			logger.info("Loaded Certificate");
			KeyManager[] managers = kmf.getKeyManagers();
			logger.debug("Managers created: "+managers.length);
			return managers;
		} catch(RuntimeException re) {
			throw re;
		} catch(Exception e) {
			logger.warn("Certificate creation/loading failed, TLS " +
					" support for client-client connections is being switched off.\n"+ e, e);
			if (!Platform.inDevelopmentMode()) {
				PI.put(PI.allowTLS, false);
			}
			return null;
		} finally {
			GH.close(fis);
		}
	}
    
    private static void genKeypair(File f,String alg) throws Exception {
    	String keytool = System.getProperty("java.home")+File.separator
		+"bin"+File.separator+"keytool";
    	
    	logger.debug("certificate created on "+f.getCanonicalPath()+"  "+keytool);

    	final Process p = Runtime.getRuntime().exec(new String[]{
		keytool ,
		"-genkey",   //"-genkeypair"  would not work with java 1.5 though "-genkey" will work with newer java
		"-storepass",
		"123456", 
		"-keyalg",
		alg,  //"RSA" /"DSA"
		"-alias",
		"jucy-"+alg+"-key",
		"-dname",
		"CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown",
		"-validity", 
		"999",
		"-keystore",
		f.getCanonicalPath()});
		
    	
		PrintStream ps = new PrintStream(p.getOutputStream());
		ps.println();
		ps.println();
		
	/*	InputStream in =  p.getInputStream();
		int c;
		String s = "";
		while(-1 != (c = in.read())) {
			s+= (char)c;
			System.out.println((char)c);
		} 
		logger.debug("Read: "+s);
		in.close(); */
		ps.close();
		p.waitFor();
    }
}
