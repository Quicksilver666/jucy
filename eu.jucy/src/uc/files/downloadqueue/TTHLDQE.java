package uc.files.downloadqueue;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import logger.LoggerFactory;


import org.apache.log4j.Logger;


import uc.DCClient;
import uc.IUser;
import uc.crypto.InterleaveHashes;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.transfer.AbstractWritableFileInterval;
import uc.files.transfer.FileTransferInformation;
import uc.files.transfer.AbstractWritableFileInterval.TTHLWriteInterval;
import uc.protocols.TransferType;

/**
 * a DownloadQueueEntry for TTHLs
 * when it finishes its download it will replace itself with a FileDQE
 * 
 * @author Quicksilver
 *
 */
public class TTHLDQE extends AbstractFileDQE {

	
	private static final Logger logger = LoggerFactory.make();
	

	private volatile boolean finished = false;
	
	TTHLDQE(DownloadQueue dq,IDownloadableFile file, File target,Date added) {
		super(dq,TransferType.TTHL,target, file,255,added);
	}

	@Override
	public boolean getDownload(FileTransferInformation fti) {
		fti.setLength(-1);
		fti.setStartposition(0);
		fti.setType(type);
		fti.setHashValue(getID());
		return runningFileTransfers.isEmpty();
	}

	@Override
	public long getDownloadedBytes() {
		return 0;
	}
	
	


	@Override
	public AbstractWritableFileInterval getInterval(FileTransferInformation fti) {
		return new TTHLWriteInterval(this,fti.getLength());
	}



	@Override
	public boolean isDownloadable() {
		return runningFileTransfers.isEmpty() && !finished;
	}
	

	
	/**
	 * when the download of the interleaves is finished..
	 * the TTHLWriteInterval will call this function to signal successful download..
	 * @param ih - the interleave hashes that were downloaded
	 */
	public synchronized void onDownloadOfInterleaves(final InterleaveHashes ih) {
		if (! finished) {
			finished = true;
			DCClient.execute(new Runnable() {
				public void run() {
					synchronized (TTHLDQE.this) {
						FileDQE fdqe = new FileDQE(dq,target,ih,file, 255/2,getAdded());
	
						//add all users and actions
						for (IDownloadFinished idf : downloadFinished) {
							fdqe.addDoAfterDownload(idf);
						}
				
						List<IUser> users = new ArrayList<IUser>(TTHLDQE.this.users);
				
					
						remove(); 
				
						dq.addDownloadQueueEntry(fdqe); //add the FDQE now to the queue..
					
			
						dq.getDatabase().addOrUpdateDQE(new DQEDAO(fdqe,ih),true);
						for (IUser user : users) { //add users afterwards so they are persisted again
							logger.debug("adding user to FileDQE: " + users);
							user.addDQE(fdqe);
						}
					}
				}
			});
		}
	}


}
