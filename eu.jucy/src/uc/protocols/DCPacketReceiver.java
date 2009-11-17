package uc.protocols;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import uc.UDPhandler.PacketReceiver;
import uc.protocols.hub.RES;
import uc.protocols.hub.SR;

public abstract class DCPacketReceiver implements PacketReceiver {

	private static final Logger logger = LoggerFactory.make();
	
	private final CharsetDecoder 	decoder;
	
	public DCPacketReceiver(Charset cs) {
		decoder		= cs.newDecoder();
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
	}
	
	public void packetReceived(ByteBuffer packet, InetSocketAddress source) {
		CharBuffer cb;
		try {
			cb = decoder.decode(packet.asReadOnlyBuffer());
			received(source,packet,cb);
		} catch (CharacterCodingException e) {
			logger.debug("packet decode failed "+e,e);
		}
	}
	
	protected abstract void received(InetSocketAddress from, ByteBuffer packet, CharBuffer contents);
	
	
	public static class NMDCReceiver extends DCPacketReceiver {

		public NMDCReceiver() {
			super(DCProtocol.NMDCCHARSET);
		}

		@Override
		protected void received(InetSocketAddress from, ByteBuffer packet,CharBuffer contents) {
    		SR.receivedNMDCSR(from, contents,packet);
		}
	}
	
	public static class ADCReceiver extends DCPacketReceiver {

		public ADCReceiver() {
			super(DCProtocol.ADCCHARSET);
		}

		@Override
		protected void received(InetSocketAddress from, ByteBuffer packet,CharBuffer contents) { 
    		RES.receivedADCRES(from, contents);
		}
	}
	

}
