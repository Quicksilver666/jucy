package network;


import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;



public class SubNetwork {

	private static final int MAX_PEERS_PER_LEVEL = 5;
	
	
	private final SortedSet<Peer> connected = new TreeSet<Peer>();
	
	private final ID intervalStart;
	private final int level;
	
	private boolean left;
	private SubNetwork subNetwork = null;
	
	private ID ownID;
	
	private SubNetwork(ID ownID,ID start, int level) {
		this.intervalStart = start;
		this.level = level;
		this.ownID = ownID;
	//	connected.addAll(peersInSubnetwork);
		ID middle = intervalStart.getMiddleID(level);
		
		int i = ownID.compareTo(middle);
		left = i < 0 ;
		
	/*	if (level < HashValue.digestlength * 8/2) {//only first half of hash is used as maximum for distinguishing -> more never needed (Birthday Paradoxon of Hash)
	//		Peer p = new Peer(middle);
	//		SortedSet<Peer> lowerLevel = 
	//			left? peersInSubnetwork.headSet(p): peersInSubnetwork.tailSet(p);

			subNetwork = new SubNetwork(socket,left ? start:middle , level+1); 
		} */
		
	}
	
	public SubNetwork(ID ownID) {
		this (ownID,ID.FirstID,0);
		
	}
	
	/**
	 * 
	 * @param p - what peer
	 * @param level - on which level of the network it should be added
	 * @return true if this subnetwork did not already contain the specified element.
	 */
	public boolean addPeer(Peer p, int level) {
		if (this.level < level) {
			return getSubNetwork().addPeer(p, level);
		} else {
			return connected.add(p);
		}
	}
	
	/**
	 * removes a peer from managing in that level..
	 * 
	 * @param p - the Peer to be removed..
	 * @param level - the level on which to add the peer
	 * @return true if the peer was there and really removed
	 */
	public boolean removePeer(Peer p, int level) {
		if (this.level < level) {
			return getSubNetwork().removePeer(p, level);
		} else {
			return connected.remove(p);
		}
	}
	
	
	/**
	 * 
	 * @param id - the ID for which we need a Peer
	 * @return a Peer as close as or equal to the given id if possible
	 */
	public Peer getClosest(ID id) {
		if (connected.contains(new Peer(id))) { //shortcut if we are connected to the peer..
			return connected.tailSet(new Peer(id)).first();
		}
		
		ID middle = intervalStart.getMiddleID(level);
		boolean idIsOnLeftSide = id.compareTo(middle) > 0; 
		
		if (idIsOnLeftSide ^ left  || subNetwork == null || subNetwork.isDeepEmpty()) {
			return getClosest(connected,middle);
		} else {
			return subNetwork.getClosest(id);
		}
	}
	
	
	private static Peer getClosest(Collection<Peer> peers , ID id) {
		ID minimumDistance = null;
		Peer minimum = null;
		for (Peer p: peers) {
			ID distance = id.getDistance(p.getPeerID());
			if (minimumDistance == null || minimumDistance.compareTo(distance) < 0 )  {
				minimumDistance = distance;
				minimum = p;
			}
		}
		
		return minimum;
	}
	
	private SubNetwork getSubNetwork() {
		if (subNetwork == null) {
			ID middle = intervalStart.getMiddleID(level);
			subNetwork = new SubNetwork(ownID,left ? intervalStart:middle , level+1); 
		}
		return subNetwork;
	}
	
	public boolean isEmpty() {
		return connected.isEmpty();
	}
	
	public boolean isDeepEmpty() {
		return isEmpty() && (subNetwork == null || subNetwork.isDeepEmpty());
	}
	
	
	
	
	
}
