package uc.files;

import helpers.IObservable;
import helpers.StatusObject;

import java.io.File;
import java.util.Date;
import java.util.List;

import uc.IUser;
import uc.crypto.HashValue;
import uc.files.UploadQueue.TransferRecord;
import uc.files.UploadQueue.UploadInfo;

public interface IUploadQueue extends IObservable<StatusObject> {

	void start();

	/**
	 * tells the uploadQueue when a User requested a file
	 * 
	 * @param usr - which user
	 * @param nameOfTransferred - the name of the transferred
	 * @param hash - the hash corresponding to the name .. null if none (FileLists)
	 * @param gotASlot - if he got a slot for the upload
	 */
	void userRequestedFile(IUser usr, String nameOfTransferred, HashValue hash,
			boolean gotASlot);

	/**
	 * @param usr - the user that got some bytes uploaded..
	 * @param bytesServedToUser 
	 */
	void transferFinished(File file, IUser usr, String nameOfTransferred,
			HashValue hashOfTransferred, long bytesServedToUser,
			Date startTime, long timeNeeded);

	List<TransferRecord> getTransferRecords();

	int getUploadRecordsSize();

	long getTotalDuration();

	long getTotalSize();

	List<UploadInfo> getUploadInfos();

	/**
	 * how many bytes the user transferred to us or from us..
	 * @param usr - which user..
	 * @return bytes transferred
	 */
	long getTotalTransferredOf(IUser usr);

	/**
	 * total time transferring bytes cost on a per user base
	 * @param usr which user
	 * @return number of milliseconds
	 */
	long getTimeNeededOf(IUser usr);

}