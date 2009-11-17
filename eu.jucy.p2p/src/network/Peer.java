package network;

import java.net.InetSocketAddress;





public class Peer implements Comparable<Peer>  {

	private final ID peerID;
	




	private final InetSocketAddress socket;
	
	
	public Peer(ID id) {
		this(id,null);
	}
	
	public Peer(ID id,InetSocketAddress socket) {
		peerID = id;
	
		this.socket = socket;
	}

	
	
	
	public int compareTo(Peer o) {
		return peerID.compareTo(o.peerID);
	} 


	public ID getPeerID() {
		return peerID;
	}


}
