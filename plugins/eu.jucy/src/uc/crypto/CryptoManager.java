package uc.crypto;

import helpers.GH;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


import logger.LoggerFactory;

import org.apache.log4j.Logger;

import uc.DCClient;
import uc.ICryptoManager;
import uc.Identity;
import uc.LanguageKeys;
import uc.PI;


public class CryptoManager implements ICryptoManager {

	private static Logger logger = LoggerFactory.make(); 
	
	private static final char[] PASS = new char[] {'1','2','3','4','5','6'};
	
	private final DCClient dcc;
	
	private final SSLContext tls;
	
	private final boolean tlsInitialised;
	private final HashValue fingerPrint;
	

	private final Identity identity;

	
	public CryptoManager(DCClient dcc,Identity identity) {
	
		this.identity = identity;
		this.dcc = dcc;
		boolean[] bP = new boolean[]{false}; 
		HashValue[] fPP = new HashValue[]{null};
		tls = loadTLS(bP,fPP);
		tlsInitialised = bP[0];
		fingerPrint= fPP[0];
	}
	
	private SSLContext loadTLS(boolean[] tlsPointer,HashValue[] fingerPrintPointer)  {
		SSLContext tlsinit = null;
		try {
			TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0];
						}
						public void checkClientTrusted(X509Certificate[] certs, String authType) {
							//logger.debug("checking client trusted: "+authType);
						}
						public void checkServerTrusted(X509Certificate[] certs, String authType) {
							//logger.debug("checking server trusted: "+authType);
						}
					}
			};
			
			
			tlsinit = SSLContext.getInstance("TLSv1.2");
			KeyManager[] managers =  identity.getBoolean(PI.allowTLS)? loadManager(fingerPrintPointer,true):null;
			tlsinit.init(managers,trustAllCerts , null);
			
			tlsPointer[0]= managers != null && managers.length > 0 ;
			logger.debug("tls initialized: "+tlsPointer[0]);
		} catch (NoSuchAlgorithmException nse) {
			logger.error(nse, nse);
		} catch (KeyManagementException kme) {
			logger.error(kme, kme);
		}

		return tlsinit;
	}
	
	
	/* (non-Javadoc)
	 * @see uc.crypto.ICryptoManager#getFingerPrint()
	 */
	public HashValue getFingerPrint() {
		return fingerPrint;
	}


	/* (non-Javadoc)
	 * @see uc.crypto.ICryptoManager#isTLSInitialized()
	 */
	public  boolean isTLSInitialized() {
		return tlsInitialised;
	}

	/* (non-Javadoc)
	 * @see uc.crypto.ICryptoManager#createSSLEngine()
	 */
	public final SSLEngine createSSLEngine() {
		return tls.createSSLEngine();
	}
	
	
	
	 private KeyManager[] loadManager(HashValue[] fingerPrintPointer,boolean retry) {
	    	FileInputStream fis = null;
			try {
				File f = new File(PI.getStoragePath()+File.separator+ identity.getCertFileName() );
				if (!f.isFile()) {
					dcc.logEvent(LanguageKeys.CreatingCertificate); 
					genKeypair(f,"RSA");
					dcc.logEvent(LanguageKeys.CreatedCertificate);
				} 
				
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
				
				
				fis = new FileInputStream(f);
				store.load(fis, PASS );
				kmf.init(store, PASS);
				dcc.logEvent(LanguageKeys.LoadedCertificate);
				KeyManager[] managers = kmf.getKeyManagers();
				Certificate cert = store.getCertificate(aliasForAlg("RSA"));
				
				if (cert != null) {
					Date oneWeek = new Date(System.currentTimeMillis()+TimeUnit.DAYS.toMillis(7));
					if (cert.getType().equals("X.509") && ((X509Certificate) cert).getNotAfter().before(oneWeek)) { // cert expiration
		                logger.warn("Certificat expires soon/ already expired: " + ((X509Certificate) cert).getNotAfter() +": deleting certificate.");
		                GH.close(fis);
		                
		                if (retry && (f.renameTo(new File(f.getParentFile(),"old_keystore"))||f.delete())) {
							return loadManager(fingerPrintPointer, false);
		                }
		            } 
					fingerPrintPointer[0] = SHA256HashValue.hashData(cert.getEncoded());
					logger.debug("fingerPrint: "+fingerPrintPointer[0]);
				} else {
					GH.close(fis);
					if (retry && (f.renameTo(new File(f.getParentFile(),"invalid_keystore"))||f.delete())) {
						return loadManager(fingerPrintPointer, false);
					} else {
						logger.error(String.format(LanguageKeys.NoCertFound,f));
					}
				}
				
				logger.debug("Managers created: "+managers.length);
				return managers;
			} catch(RuntimeException re) {
				throw re;
			} catch(Exception e) {
				logger.warn(String.format(LanguageKeys.CertificateCreationFailed,e),e);
	
				identity.put(PI.allowTLS, false);
			
				return null;
			} finally {
				GH.close(fis);
			}
		}

	 /*
	  * 
	  * keytool -genkey -storepass 123456 -keystore storagename -keyalg RSA -dname "CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown" -validity 999
	  */
	 private static void genKeypair(File f,String alg) throws Exception {
		 String keytool = System.getProperty("java.home")+File.separator
		 +"bin"+File.separator+"keytool";

		 logger.debug("certificate created on "+f.getCanonicalPath()+"  "+keytool);

		 final Process p = Runtime.getRuntime().exec(new String[]{
				 keytool ,
				 "-genkey",   //"-genkeypair"  would not work with java 1.5 though "-genkey" will work with newer java
				 "-storepass",
				new String(PASS), 
				 "-keyalg",
				 alg,  //"RSA" /"DSA"
				 "-alias",
				 aliasForAlg(alg),
				 "-dname",
				 "CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown",
				 "-validity", 
				 "730",
				 "-keystore",
				 f.getCanonicalPath()});


		 PrintStream ps = new PrintStream(p.getOutputStream());
		 ps.println();
		 ps.println();


		 ps.close();
		 p.waitFor();
	 }

	 private static String aliasForAlg(String alg) {
		 return "jucy-"+alg+"-key";
	 }


}
