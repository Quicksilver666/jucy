package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;

import uc.UDPhandler.PacketReceiver;



/**
 * represents a Socket for the P2P overlay
 * 
 * @author Quicksilver
 *
 */
public class P2PSocket implements PacketReceiver {
	
	private static Logger logger = LoggerFactory.make();
	
	static {
		logger.setLevel(Platform.inDevelopmentMode()? Level.DEBUG:Level.INFO);
	}
	
	private final ID ownID;

	

	private final int ownUDPPort;
	
	public P2PSocket(int ownUDPPort,ID ownID) {
		this.ownID = ownID;
		this.ownUDPPort = ownUDPPort;
	}
	
	
	/**
	 * used for initialisation 
	 * though can be called multiple times 
	 * i.e. bootstrapping with multiple peers..
	 * 
	 * @param otherPeer
	 */
	public void init(InetSocketAddress otherPeerTCP) {
		
	}
	
	/**
	 * sends message to Peer
	 * 
	 * @param target the peer that should receive the message
	 * @param message  the message sent
	 */
	public void send(P2PMessage message) {
		
	}
	
	/**
	 * sends a message to the Peer responsible for the given
	 * target
	 * 
	 * @param target the HashValue for which this message is interesting
	 * like the peer responsible for the interval in which target is lying
	 * @param message the data to be sent..
	 */
	public void sendCloseTo(ID target , byte[] message) {
		
	}
	

	
	public void packetReceived(ByteBuffer packet, InetSocketAddress source) {
		try {
			P2PMessage mes = P2PMessage.receiveP2PMessage(packet);
			
		} catch (IOException ioe) {
			logger.debug(ioe,ioe);
		}
	}


	public ID getOwnID() {
		return ownID;
	}
	

}
