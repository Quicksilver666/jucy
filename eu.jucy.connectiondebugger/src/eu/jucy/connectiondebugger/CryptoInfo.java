package eu.jucy.connectiondebugger;

import helpers.GH;
import helpers.SizeEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import uc.protocols.ICryptoInfo;

import logger.LoggerFactory;

public class CryptoInfo implements ICryptoInfo {

	public static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	public static final String 	ENABLED_CIPHERSUITES 	= "Enabled Cipher suites"
								,CIPHERSUITE 			= "Ciphersuite used"		
								,ENABLED_PROTOCOLS 		= "Enabled Protocols"
								,PROTOCOL				= "Protocol in use"
								,HADNSHAKE_STATUS		= "Handshake Status"
								,PEER_CERTIFICATES		= "Peer certificates"
								,SESSION_VALUES			= "Session values"
								, APPLICATION_BUFFER	= "Application Buffer size"
								, PACKET_BUFFER			= "Packet Buffer size"
								,PRINCIPAL				= "Principal";
	
	private final List<CryptoInfoEntry> cryptoInfo = new ArrayList<CryptoInfoEntry>();
	
	

	public List<CryptoInfoEntry> getCryptoInfo() {
		return Collections.unmodifiableList(cryptoInfo);
	}

	public void setInfo(SSLEngine ssle) {
		cryptoInfo.clear();
		put(ENABLED_CIPHERSUITES , GH.concat(ssle.getEnabledCipherSuites(),", ","-"));
		put(ENABLED_PROTOCOLS , GH.concat(ssle.getEnabledProtocols(),", ","-"));
		put(HADNSHAKE_STATUS ,ssle.getHandshakeStatus().toString());
		SSLSession ssls = ssle.getSession();
		
		try {
			put(PEER_CERTIFICATES ,GH.concat(ssls.getPeerCertificates(),"\n---NEW CERT-------\n","-"));
			put(PRINCIPAL	 , ssls.getPeerPrincipal().toString());
		} catch (SSLPeerUnverifiedException e) {
			logger.debug(e, e);
		}
		
		List<String> keyValuePairs = new ArrayList<String>();
		for (String s : ssls.getValueNames()) {
			keyValuePairs.add(s+"="+ssls.getValue(s));
		}
		put(SESSION_VALUES , GH.concat(keyValuePairs,", ","-"));
		put(CIPHERSUITE, ssls.getCipherSuite());
		
		put(PROTOCOL, ssls.getProtocol());
		put(APPLICATION_BUFFER , SizeEnum.getReadableSize(ssls.getApplicationBufferSize())+"  ("+ssls.getApplicationBufferSize()+")");
		put(PACKET_BUFFER	 , SizeEnum.getReadableSize(ssls.getPacketBufferSize())+"  ("+ssls.getPacketBufferSize()+")");
		
	}
	
	private void put(String key, String value) {
		CryptoInfoEntry cie = new CryptoInfoEntry(key, value); 
		cryptoInfo.add(cie);
	}
	

	public static class CryptoInfoEntry {
		private final String type;
		private final String info;
		
		public CryptoInfoEntry(String type, String info) {
			super();
			this.type = type;
			this.info = info;
		}

		public String getType() {
			return type;
		}

		public String getInfo() {
			return info;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CryptoInfoEntry other = (CryptoInfoEntry) obj;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
		
	}
	
}
