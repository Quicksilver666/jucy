package uc;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

import uc.UDPhandler.PacketReceiver;
import uc.files.search.SearchResult;
import uc.protocols.hub.Hub;

public interface IUDPHandler {

	void start();

	void stop();

	/**
	 * 
	 * @param srs - a set of search results
	 * @param target - where to send it
	 */
	void sendSearchResultsBack(Set<SearchResult> srs, Hub hub,
			InetSocketAddress target);

	/**
	 * just sends  a packet with hte datagram channel .. without throwing an exception
	 * ... if this doesn't work well we might put the sending in some other thread
	 * @param packet - a packet of data
	 * @param target - where to send the data...
	 */
	void sendPacket(ByteBuffer packet, InetSocketAddress target);

	void sendPacketIgnoreProxy(ByteBuffer packet, InetSocketAddress target);

	/**
	 * registers a receiver for packets..
	 * all packets that have the first byte equal to firstByte
	 * will be given to receiver 
	 * 
	 * @param firstByte - the first byte of every packet
	 * @param receiver - the class to handle the packages
	 */
	void register(int firstByte, PacketReceiver receiver);

	/**
	 * unregisters a receiver if it is registered
	 * 
	 */
	void unregister(int firstByte, PacketReceiver receiver);

	int getPort();

}