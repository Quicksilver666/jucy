/*
 * UDPhandler.java
 *
 * Created on 5. November 2005, 23:20
 *
 * handles incoming UDP packets(search results)
 */

package uc;

import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;


import java.util.concurrent.CopyOnWriteArrayList;





import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;



import uc.crypto.UDPEncryption;
import uc.protocols.DCProtocol;
import uc.protocols.Socks;
import uc.protocols.DCPacketReceiver.ADCReceiver;
import uc.protocols.DCPacketReceiver.NMDCReceiver;


/**
 *
 * Receives UDP signals and provides facility to send UDP packets 
 *
 * @author Quicksilver
 */
public class UDPhandler implements IUDPHandler {

	private static final Logger logger = LoggerFactory.make();

	/**
	 * decoders and encoders for the charsets
	 */
	private final CharsetEncoder	nmdcencoder, 
									adcencoder;
								
	
	private final CopyOnWriteArrayList<byte[]> keysActive = new CopyOnWriteArrayList<byte[]>();

	/**
	 * communication variable to signal port changed in the settings 
	 */
	private volatile boolean portchanged;
	private volatile int port;
	
	private Thread udpThread;
	private volatile boolean running = true;
	
	private volatile DatagramChannel datagramChannel;
	
	private static final int UDPMAXPAYLOAD = 65536; 
	private ByteBuffer packet = ByteBuffer.allocate(UDPMAXPAYLOAD/4); //larger than 16KiB won'T arrive anyway..

	private PacketReceiver[] receivers = new PacketReceiver[255];
	
	private final DCClient dcc;
	
    /** will handle the incoming Searches  UDP Port */
    public UDPhandler(DCClient dcc) {
    	this.dcc = dcc;
    	nmdcencoder		= DCProtocol.NMDCCHARSET.newEncoder();
    	nmdcencoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    	nmdcencoder.onMalformedInput(CodingErrorAction.REPLACE);


    	adcencoder		= DCProtocol.ADCCHARSET.newEncoder();
      	adcencoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    	adcencoder.onMalformedInput(CodingErrorAction.REPLACE);
   
    	//listener for port changes in the settings
    	new PreferenceChangedAdapter(PI.get(),PI.udpPort) {
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				portchanged = true;	
				GH.close(datagramChannel);
			}
    	};
    	
		register((int)'$', new NMDCReceiver(dcc));
		register((int)'U', new ADCReceiver(dcc));
    }
    
    /**
     * opens the UDP socket..
     * @throws IOException - may be error because port is in use..
     */
    private void open() throws IOException {
    	try {
	    	datagramChannel = DatagramChannel.open();
	    	datagramChannel.socket().bind( new InetSocketAddress(port));
	    	datagramChannel.configureBlocking(true);
	    	logger.info("UDP handler gone up on Port: "+port);
		} catch (SecurityException se) {
			if (port <= 1024 && (Platform.getOS().equals(Platform.OS_MACOSX) || Platform.getOS().equals(Platform.OS_LINUX)) ) {
				throw new missing16api.IOException("Unix based OS need root rights to use ports lower than 1024!",se );
			} 
			throw new missing16api.IOException(se);
		}
    }
    
    /* (non-Javadoc)
	 * @see uc.IUDPHandler#start()
	 */
    public void start() {
    	udpThread = new Thread(new Runnable() {
			public void run() {
				runUDP();
			}
    	},"UDP-Handler");
    	udpThread.start();
    }
    
    /* (non-Javadoc)
	 * @see uc.IUDPHandler#stop()
	 */
    public void stop() {
    	running = false;
    	GH.close(datagramChannel);
    }
    
    /**
     * runs the channel  endlessly loops for receiving packets
     * and provides payloads of the packets to receivedPacket()
     * 
     */
    private void runUDP() { 
    	while (running) {
    		try {
    			portchanged = false;
    			port = PI.getInt(PI.udpPort);
    			open();
    			while (!portchanged && datagramChannel.isOpen()) {
    				packet.clear();
    				SocketAddress from = datagramChannel.receive(packet);
    				if (from instanceof InetSocketAddress) {
    					packet.flip();
    					receivedPacket((InetSocketAddress)from, packet,true);
    				} else {
    					logger.debug("not a InetSocketAddress received: "+from);
    				}
    			}
    
    		} catch (IOException ioe) {
    			if (!portchanged && running) {
    				logger.warn(ioe,ioe);
    			} else {
    				logger.debug(ioe,ioe);
    			}
    			GH.sleep(5000);
    			
    		} finally {
    			GH.close(datagramChannel);
    		}
    	}
    }
    
    /**
     * checks if a packet is nmdc or adc,
     * decodes it accordingly and then gives it to the matching method
     * 
     * @param from - address of sender 
     * @param packet - buffer containing the payload
     * @param possiblyEncrypted - true if the packet might possibly be encrypted
     * @throws CharacterCodingException 
     */
    private void receivedPacket(InetSocketAddress from, ByteBuffer packet,boolean possiblyEncrypted)   {
    	//charBuffer.clear();
    	dcc.getConnectionDeterminator().udpPacketReceived(from);//jay we received something..
 	
    	if (packet.hasRemaining()) {
    		PacketReceiver r = receivers[(int)(packet.get(0) & 0xff)];
    		
    		if (r != null && r.matches(packet.get(1), packet.get(2), packet.get(3))) {
    			r.packetReceived(packet, from);
    		} else if (possiblyEncrypted) {
    			byte[] encrypted = new byte[packet.remaining()];
    			packet.get(encrypted);
    			byte[] decrypted = UDPEncryption.decryptMessage(encrypted, keysActive);
    			if (decrypted != null) {
    				receivedPacket(from,ByteBuffer.wrap(decrypted),false);
    			} else {
	    			if (Platform.inDevelopmentMode()) {
	    				logger.warn("unknown PacketReceived: "+ new String(packet.array())+"  "+possiblyEncrypted);
	    			}
    			}
    		}
    	}
    }


    
//    /* (non-Javadoc)
//	 * @see uc.IUDPHandler#sendSearchResultsBack(java.util.Set, uc.protocols.hub.Hub, java.net.InetSocketAddress)
//	 */
//    public void sendSearchResultsBack(Set<SearchResult> srs,Hub hub ,InetSocketAddress target) {
//    	logger.debug("sending search results back");
//    	CharBuffer charBuffer = CharBuffer.allocate(UDPMAXPAYLOAD/4);
//    	try {
//    		for (SearchResult sr: srs) {
//    			String command = hub.getUDPSRPacket(sr); 
//    			charBuffer.put(command);
//    			charBuffer.flip();
//    			
//    			if (hub.isNMDC() && !hub.getCharset().equals(DCProtocol.NMDCCHARSET)) {
//    				sendPacket(hub.getCharset().encode(charBuffer),target); //use overridden charsets 
//    			} else {
//    				CharsetEncoder encoder = hub.isNMDC()? nmdcencoder:adcencoder;
//    				synchronized(encoder) {
//    					sendPacket(encoder.encode(charBuffer),target);
//    				}
//    			}
//    			charBuffer.clear();
//    		}
//    		
//    	} catch (CharacterCodingException cee) {
//    		logger.warn(cee,cee);
//    	}
//    }
    
    /* (non-Javadoc)
	 * @see uc.IUDPHandler#sendPacket(java.nio.ByteBuffer, java.net.InetSocketAddress)
	 */
    public void sendPacket(ByteBuffer packet,InetSocketAddress target ) {
    	if (!Socks.isEnabled()) {
    		sendPacketIgnoreProxy(packet, target);
    	} else {
    		// ... UDP Relay... though not really needed..
    		 //as seems not to work and is common in DC++ client to not answer with udp..
    	}
    }
    
    /* (non-Javadoc)
	 * @see uc.IUDPHandler#sendPacketIgnoreProxy(java.nio.ByteBuffer, java.net.InetSocketAddress)
	 */
    public void sendPacketIgnoreProxy(ByteBuffer packet,InetSocketAddress target ){
    	if (0 <  target.getPort() && target.getPort() <= 65535) {
    	    logger.debug("sending udp packet: "+target+ " "+ packet);
    	    try {
    	    	int i = datagramChannel.send(packet, target);
    	    	logger.debug("sent bytes: "+i);
    	    } catch(BindException be) {
    	    	logger.debug("Packet could not be send: "+be,be);
    	    } catch (IOException ioe) {
    	    	logger.warn(ioe+" packet: "+target+"  "+new String(packet.array()),ioe);
    	    }
    	}
    }
    
    /* (non-Javadoc)
	 * @see uc.IUDPHandler#register(int, uc.UDPhandler.PacketReceiver)
	 */
    public final void register(int firstByte,PacketReceiver receiver ) {
    	if (firstByte < 0 || firstByte > 255 || receiver == null) {
    		throw new IllegalArgumentException();
    	}
    	if (receivers[firstByte] == null) {
    		receivers[firstByte] = receiver;
    	} else {
    		throw new IllegalArgumentException("Already registered a receiver for that byte");
    	}
    }
    
    /* (non-Javadoc)
	 * @see uc.IUDPHandler#unregister(int, uc.UDPhandler.PacketReceiver)
	 */
    public void unregister(int firstByte,PacketReceiver receiver) {
    	if (firstByte < 0 || firstByte > 255 || receiver == null) {
    		throw new IllegalArgumentException();
    	}
    	if (receivers[firstByte] != null) {
    		if (receivers[firstByte].equals(receiver)) {
    			receivers[firstByte] = null;
    		} else {
    			throw new IllegalArgumentException("Not registered on that byte");
    		}
    	} 
    }

	
	
	/* (non-Javadoc)
	 * @see uc.IUDPHandler#getPort()
	 */
	public int getPort() {
		return port;
	}
	
	
	/**
	 * 
	 * tokens must be told to UDP handler for decryption
	 * @param token the token used..
	 */
	public void addTokenExpected(String token) {
		byte[] key =  UDPEncryption.tokenStringToKey(token); 
		keysActive.add(0,key);
		if (keysActive.size() > 10) {
			keysActive.remove(keysActive.size()-1);
		}
	}
	

	
	public static interface PacketReceiver {
		
		/**
		 * signals that a packet was received.
		 * on returning from the method call the ByteBuffer may
		 * no longer be used by the called object. As it will be used for other packets.
		 * 
		 * @param packet - the packet received from the user..
		 * @param source - where it came from..
		 * 
		 */
		void packetReceived(ByteBuffer packet,InetSocketAddress source);
		
		/**
		 * while byte zero is used to discriminate against messages .. -> this is further used for faster
		 * check than with encryption..
		 * gets byte 1,2 and 3 against the Handler
		 * @param one
		 * @param two
		 * @param three
		 * @return
		 */
		boolean matches(byte one,byte two,byte three);
		
	}
    
}
