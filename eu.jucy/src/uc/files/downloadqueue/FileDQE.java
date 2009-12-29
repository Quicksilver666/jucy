package uc.files.downloadqueue;

import helpers.StatusObject;
import helpers.StatusObject.ChangeType;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import logger.LoggerFactory;
import org.apache.log4j.Logger;


import uc.DCClient;
import uc.IUser;
import uc.PI;
import uc.crypto.InterleaveHashes;
import uc.files.IDownloadable.IDownloadableFile;

import uc.files.transfer.AbstractWritableFileInterval;
import uc.files.transfer.Block;
import uc.files.transfer.FileTransferInformation;
import uc.files.transfer.AbstractWritableFileInterval.FileWriteInterval;
import uc.files.transfer.Block.BlockState;
import uc.protocols.TransferType;

public class FileDQE extends AbstractFileDQE {

	private static final Logger logger = LoggerFactory.make();

	private static final int MAX_SIZE_NO_INTERLEAVES = 64 * 1024; 
	
	/**
	 * the minimum size a file must have so multiple downloads are allowed..
	 */
	private static final long MULTIDOWNLOADTHRESHOLD = PI.getInt(PI.minimumSegmentSize) * 1024*1024;
	
	
	/**
	 * object to synchronize requests against..
	 * currently used for access to the blocks ..
	 */
	private final Object synch = new Object();
	
	
	private BlockINFO bi;
	
	
	private static class BlockINFO  {
		
		
		/**
		 * the interleaves used to verify this file..
		 */
		private final InterleaveHashes ih;  
		
		/**
		 * 
		 */
		private List<Block> blocks;
		
		
		private volatile long blocksize = -1;

		public BlockINFO(InterleaveHashes ih) {
			super();
			this.ih = ih;
		}
	}
	
	
	
	
	protected FileDQE(DownloadQueue dq,File target, InterleaveHashes ih, IDownloadableFile file,int priority,Date added) {
		super(dq,TransferType.FILE,target, file, priority,added); 
		//this.ih = ih;
	//	logger.info("1 set Blockinfo for: "+file.getName());
		logger.debug("restoring file: "+file.getName());
		
		if (getTempPath().isFile() || file.getSize() <= MAX_SIZE_NO_INTERLEAVES) {
			setBlockInfo(ih);
			logger.debug("set Blockinfo for: "+file.getName());
		} else {
			logger.debug("not set Blockinfo for: "+file.getName());
		}
		
	}
	
	private void setBlockInfoIfNotSet() {
		synchronized (synch) {
			if (bi == null) {
	//			InterleaveHashes ih =  dq.getDatabase().getInterleaves(getTTHRoot());
				setBlockInfo(getIh());
			}
		}
	}
	
	private void setBlockInfo(InterleaveHashes ih) {
		ArrayList<Block> blocks;
		synchronized(synch) {
			try {
			bi = new BlockINFO(ih);
			blocks = new ArrayList<Block>();
			for (int i = 0; i < ih.getInterleaves().size();i++) {
				blocks.add(new Block(this,i,ih.getHashValue(i)));
			}
			bi.blocks = blocks;
			} catch(Exception e) {
				logger.warn(e,e);
				
			}
		}
	}
	
	@Override
	public boolean getDownload(FileTransferInformation fti) {
		setBlockInfoIfNotSet();
		int startofMaxBlocks = 0;
		int maxBlocks = -1;
		int currentblocks;
		int totalBlocksToBeDownloaded = 0;
		synchronized (synch) {
			for (int i=0;i < bi.blocks.size(); i++) {
				Block b = bi.blocks.get(i);
				if (b.isWritable()) {
					int interval = b.getIntervalLengthInBlocks();
					
					
					if (i > 0 && bi.blocks.get(i-1).getState() == BlockState.WRITEINPROGRESS) {
						currentblocks = interval/ 2;  //halve the interval size if someone writes to this interval
					} else {
						currentblocks = interval;
					}
					totalBlocksToBeDownloaded += interval;
					
					i += interval; //advance the for loop
					
					if (currentblocks > maxBlocks) {
						maxBlocks = currentblocks;
						
						startofMaxBlocks = i - currentblocks;
					}
				}
			}
			
		}
		if (maxBlocks > 0) {
			fti.setType(type);
			fti.setHashValue(getID());
			long blocksize = bi.blocks.get(0).getLength();
			fti.setStartposition(startofMaxBlocks * blocksize);
			fti.setLength(bi.blocks.get(startofMaxBlocks).getIntervalLength());
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	public long getDownloadedBytes() {
		synchronized (synch) {
			if (bi == null) {
				return 0;
			}
		}
		long bytes = 0;
		synchronized(synch)  {
			for (Block b:bi.blocks) {
				if (b.isFinished()) {
					bytes += b.getLength();
				} 
			}
		}
		return bytes;
	}

	public void internal_NotifyBlockChanged(Block b,BlockState state) {
		dq.notifyObservers(new StatusObject(this,ChangeType.CHANGED,
				DownloadQueue.FILEDQE_BLOCKSTATUSCHANGED,b));
		
		if (state == BlockState.FINISHED) {
			blockValidated();
		}
	}

	@Override
	public AbstractWritableFileInterval getInterval(FileTransferInformation fti) {
		return new FileWriteInterval(this,fti.getStartposition(),fti.getLength());
	}

	@Override
	public boolean isDownloadable() {
		if (!isWriteable()) {
			return false;
		} else if (getNrOfRunningDownloads() == 0) {
			//no download is currently running on this DQE.. so  ok
			return true;
		} else if (getSize() < (getNrOfRunningDownloads()+1)* MULTIDOWNLOADTHRESHOLD) {
			//a lot of people are already downloading compared to size .. so no
			return false;
		} else {
		//our first checks didn't help .. so we do some more costly checks.. 
		//		int startofMaxBlocks;
			int maxBlocks = -1;
			int currentblocks;
			int totalBlocksToBeDownloaded = 0;
			synchronized (synch) {
				for (int i = 0 ;i < bi.blocks.size(); i++) {
					Block b = bi.blocks.get(i);
					if (b.isWritable()) {
						int interval = b.getIntervalLengthInBlocks();
							
							
						if (i > 0 && bi.blocks.get(i-1).getState() == BlockState.WRITEINPROGRESS) {
							currentblocks = interval/ 2;  //halve the interval size if someone writes to this interval
						} else {
							currentblocks = interval;
						}
						totalBlocksToBeDownloaded+= interval;
							
						i += interval; //advance the for loop
							
						if (currentblocks > maxBlocks) {
							maxBlocks = currentblocks;
						}
					}
				}
			}
				
				
			long blocksize = bi.blocks.get(0).getLength();
			//check if largest free space is smaller than 8Mib if so download only if no other users are online
			return blocksize * maxBlocks  > MULTIDOWNLOADTHRESHOLD ;
			
		}
	}
	
	/**
	 * factory for a FileDQE
	 * will create a FileDQE directly or if the size is larger than 
	 * 64 KBit TTHLDQE will be created which will in return create
	 * a FileDQE
	 * 
	 * no Entry will be created if a File Already exists on the Target position
	 * of a File with same TTH is contained in our FileList.
	 * 
	 * @param idf
	 * @param target
	 * @return the DownloadQueueEntry.. or null if the File is already in the OwnFilelist or exists on the target possition
	 */
	public static AbstractDownloadQueueEntry get(IDownloadableFile idf,File target) {
		DCClient dcc = DCClient.get();
		DownloadQueue dq = dcc.getDownloadQueue();
		if (dcc.getFilelist().search(idf.getTTHRoot()) != null || target.isFile()) {
			return null;
		}
		
		AbstractDownloadQueueEntry dqe = dq.get(idf.getTTHRoot());
		// check if existent
		if (dqe == null) {	
			//if not existing create TTHLDQR or FileDQE
			//and add to Queue
			if (idf.getSize() <= MAX_SIZE_NO_INTERLEAVES) { //if it is smaller than 64KiB we won't download interleave hashes 
				//create FileDQE
				dqe = new FileDQE(dq,target, new InterleaveHashes(idf.getTTHRoot()), idf,255/2,new Date()); 
			} else {
				//create TTHLDQE
				dqe = new TTHLDQE(dq,idf,target,new Date()); 
			}
			//persist  
			dcc.getDatabase().addOrUpdateDQE(DQEDAO.get(dqe),true); //add the DQE item to the persistent storage..
			
			dq.addDownloadQueueEntry(dqe);
			
			
			logger.debug("persisted the dqe");
		}
		logger.debug("adding User");
		for (IUser usr: idf.getIterable()) {
			dqe.addUser(usr);
			logger.debug("added usr to dqe: "+usr.getNick());
		}
		
		
		return dqe; 
	}
	
	public static void restore(DQEDAO restoredata,DownloadQueue dq) {
		logger.debug("in FileDQE.restore()");
		AbstractDownloadQueueEntry dqe = null;
		
		if (restoredata.getSize() <= MAX_SIZE_NO_INTERLEAVES || restoredata.getIh() != null) {
			InterleaveHashes ih = restoredata.getIh();
			if (ih == null) {
				ih = new InterleaveHashes(restoredata.getTTHRoot());
			}
			
			dqe = new FileDQE(dq,restoredata.getTarget(),ih,restoredata,restoredata.getPriority(),restoredata.getAdded()); 
		} else {
			dqe = new TTHLDQE(dq,restoredata,restoredata.getTarget(),restoredata.getAdded()); 
		}
		
		
		dq.addDownloadQueueEntry(dqe);
		for (IUser usr: restoredata.getIterable()) {
			logger.debug("adding user to restored DQE: "+usr.getNick());
			dqe.addUser(usr);
		}
	}

	public InterleaveHashes getIh() {
		synchronized(synch) {
			if (bi == null) {
				//logger.info("IH retrieved: "+getTTHRoot());
				return dq.getDatabase().getInterleaves(getTTHRoot());
			} else {
				return bi.ih;
			}
		}
	}
	
	/**
	 * calculates the blocksize from the filesize and the number of interleave hashes..
	 * @return the size a single Block has..
	 */
	public long getBlocksize() {
		synchronized(synch) {
			if (bi.blocksize == -1) {
				bi.blocksize = bi.ih.getGranularity(file.getSize());
			}
			return bi.blocksize;
		}
	}

	
	/**
	 * this is called by the Blocks to signal
	 * the validation of a single block
	 * method checks if all blocks are finished
	 * if they are the file is moved to the destination
	 * eventually actions are executed .. 
	 * and finally the file is removed from the DownloadQueue
	 */
	private void blockValidated() {
	//	logger.debug("in FileDQE.blockValidated()");
		if (isFinished()) { 
	
			logger.debug("in FileDQE.blockValidated() and finished");
			finished = new Date(); //we set the time when this FileDQE was finished..

			//finalise the DQE
			if (storeToDestination()) {
				//then do finalising tasks..
				executeDoAfterDownload();

				//remove.. from the queue
				remove();
			}
			logger.debug("in FileDQE.blockValidated() and end of finishing");
		
		}
	}
	
	
	public boolean isFinished() {
		synchronized(synch) {
			for (Block b: bi.blocks) {
				if (!b.isFinished()) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * checks if there are any blocks that can  be written to
	 * @return
	 */
	private boolean isWriteable() {
		synchronized(synch) {
			if (bi == null) {
				return true;
			}
			for (Block b: bi.blocks) {
				if (b.isWritable()) {
					return true;
				}
			}
		}
		return false;
	}
	

	/**
	 * retrieves one Block (a part of the file matching one 
	 * HashValue of the interleave hashes)
	 * 
	 * @param blocknumber - which block of the file
	 * @return a block matching the given position 
	 */
	public Block getBlock(int blocknumber) {
		setBlockInfoIfNotSet();
		synchronized(synch) {
			if (blocknumber < bi.blocks.size() && blocknumber >= 0) {
				return bi.blocks.get(blocknumber);
			} else {
				return null;
			}
		}
	}

	/**
	 * number of Blocks.. or 0 if currently unknown. due to blocks not loaded..
	 * 
	 * @return
	 */
	public int getNrOfBlocks() {
		synchronized(synch) {
			if (bi == null) {
				return 0;
			}
			return bi.blocks.size();
		}
	}

	
	

}
