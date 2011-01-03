package eu.jucy.testfragment;

import helpers.StatusObject;
import helpers.Observable.IObserver;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import uc.IUser;
import uc.crypto.HashValue;
import uc.files.IUploadQueue;
import uc.files.UploadQueue.TransferRecord;
import uc.files.UploadQueue.UploadInfo;

public class FakeUploadQueue implements IUploadQueue {

	public long getTimeNeededOf(IUser usr) {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}

	public long getTotalDuration() {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}

	public long getTotalSize() {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}

	public long getTotalTransferredOf(IUser usr) {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}

	public List<TransferRecord> getTransferRecords() {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}

	public List<UploadInfo> getUploadInfos() {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}

	public int getUploadRecordsSize() {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}

	public void start() {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}
	
	public void stop() {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}


	public void userRequestedFile(IUser usr, String nameOfTransferred,
			HashValue hash, boolean gotASlot) {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");

	}

	public void addObserver(IObserver<StatusObject> o) {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");

	}

	public void deleteObserver(IObserver<StatusObject> o) {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
	}

	@Override
	public void transferFinished(File file, IUser usr,
			String nameOfTransferred, HashValue hashOfTransferred,
			long bytesServedToUser, Date startTime, long timeNeeded,
			InetAddress targetIP) {
		throw new IllegalStateException("Method in fake FakeUploadQueue called");
		
	}

}
