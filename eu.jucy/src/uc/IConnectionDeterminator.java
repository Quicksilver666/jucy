package uc;

import helpers.IObservable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import uc.ConnectionDeterminator.CDState;
import uc.files.search.FileSearch;

public interface IConnectionDeterminator extends IObservable<String>{

	/**
	 * initialises for use  without blocking
	 * @return a future to wait for initialisation..
	 */
	Future<?> start();

	/**
	 * initialises for use blocks until finished..
	 */
	void init();

	boolean isUdpReceived();

	boolean isSearchStarted();

	boolean isNATPresent();

	/**
	 * @return object holding all state 
	 * getting all info to the observers ..
	 */
	CDState getState();

	/**
	 * tell if a connection to our TCP port
	 * was made..
	 * false for normal TCP port if true its the TLS port..
	 */
	void connectionReceived(boolean encrypted);

	/**
	 * tell if a connection attempt to us timed out..
	 * @param usr - the user connection that timed out..
	 */
	void connectionTimedOut(IUser usr, boolean encryption);

	/**
	 * notification that a search was started from our side ..
	 * so that we can expect UDP packets ..
	 */
	void searchStarted(FileSearch search);

	/**
	 * when ever we receive a UDP packet
	 * we are told by this method..
	 * 
	 * @param from - who sent the packet
	 */
	void udpPacketReceived(InetSocketAddress from);

	/**
	 * when ever we receive a UserIP signal from a hub
	 * we set this..
	 * 
	 * @param ourIPAddress - the IP we got
	 * @param whoTold - the hub that told us - null for web
	 */
	void userIPReceived(InetAddress ourIPAddress, FavHub whoTold);

	/**
	 * 
	 * @return the public IP address
	 * either as set in settings ..
	 * or as found by detection..
	 */
	InetAddress getPublicIP();

	InetAddress getDetectedIP();

	
	void stop();
	
	
	boolean isExternalIPSetByHand();
}