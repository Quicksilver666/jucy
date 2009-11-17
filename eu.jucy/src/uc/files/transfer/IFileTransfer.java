package uc.files.transfer;


import helpers.IObservable;

import java.util.Date;

import uc.IUser;
import uc.protocols.Compression;
import uc.protocols.client.ClientProtocol;

/**
 * interface for everything that can be shown in the GUI view
 * as a file transfer..
 * 
 * @author Quicksilver
 *
 */
public interface IFileTransfer extends IObservable<TransferChange> {
	
	/**
	 * 
	 * @return the user we are uploading to or downloading from
	 */
	IUser getOther();
	
	/**
	 * estimate the remaining time in seconds
	 * 
	 * @return the remaining time in seconds
	 */
	long getTimeRemaining();
	
	/**
	 * 
	 * @return when this FileTransfer has started
	 */
	Date getStartTime();
	
	
	 /**
	  * estimates the current speed 
	  * @return bytes transferred per second..
	  */
	long getSpeed();
	
	/**
	 *
	 * @return  a user readable name of the transferred data
	 * usually this is the filename
	 * 
	 */
	String getNameOfTransferred();
	
	
	/**
	 * 
	 * @return a FileInteral representing the current position
	 * and the total size of the transfer 
	 */
	AbstractFileInterval getFileInterval();
	
	/**
	 * if this FileTransfer is an upload or an download
	 * @return true if this represents an Upload false for a download
	 */
	boolean isUpload();
	
	/**
	 * stops the transfer by closing the connection
	 */
	void cancel() ;
	
	

	
	/**
	 * 
	 * @return the compression used for this transfer
	 */
	Compression getCompression();
	
	
	/**
	 * 
	 * @return ratio of compression .. 1 = no compression..
	 */
	float getCompressionRatio();
	
	/**
	 * all filetransfer information ... 
	 * @return
	 */
	FileTransferInformation getFti();
	
	/**
	 * 
	 * @return the client protocol associated with this..
	 */
	ClientProtocol getClientProtocol();
	
}

