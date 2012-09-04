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

import uc.DCClient;
import uc.UDPhandler.PacketReceiver;
import uc.protocols.hub.PSR;
import uc.protocols.hub.RES;
import uc.protocols.hub.SR;

public abstract class DCPacketReceiver implements PacketReceiver {

	private static final Logger logger = LoggerFactory.make();
	
	private final CharsetDecoder 	decoder;
	protected final DCClient dcc;
	
	private final byte one,two,three;
	
	public DCPacketReceiver(DCClient dcc,Charset cs,char one,char two, char three) {
		this.dcc = dcc;
		this.one = (byte)one;
		this.two = (byte)two;
		this.three = (byte)three;
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
	
	
	
	
	public boolean matches(byte one, byte two, byte three) {
		return this.one == one & this.two == two & this.three == three;
	}




	public static class NMDCReceiver extends DCPacketReceiver {

		public NMDCReceiver(DCClient dcc) {
			super(dcc,DCProtocol.NMDC_CHARSET,'S','R',' ');
		}

		@Override
		protected void received(InetSocketAddress from, ByteBuffer packet,CharBuffer contents) {
    		SR.receivedNMDCSR(from, contents,packet,dcc);
		}
	}
	
	public static class ADCReceiver extends DCPacketReceiver {

		public ADCReceiver(DCClient dcc) {
			super(dcc,DCProtocol.ADC_CHARSET,'R','E','S');
		}

		@Override
		protected void received(InetSocketAddress from, ByteBuffer packet,CharBuffer contents) { 
			if (packet.get(1) == 'R') {
				RES.receivedADCRES(from, contents,dcc);
			} else {
				PSR.receivedPSR(from, contents, dcc);
			}
		}
		
		public boolean matches(byte one, byte two, byte three) {
			return super.matches(one, two, three) | ('P' == one & 'S' == two &  'R' == three) ;
		}
	}
	

}
