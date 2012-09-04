package uc.files.transfer;

import java.util.HashMap;
import java.util.Map;

import uc.IUser;
import uc.files.downloadqueue.FileDQE;
import uc.files.downloadqueue.FileDQE.ChunkedIntervalList;

public class FileDistributionTracker {

	private final FileDQE trackedFile;
	
	private final Object synch = new Object();
	
	/**
	 * what parts of the file the peer has
	 */
	private final Map<IUser,ChunkedIntervalList> chunksOfPeers = new HashMap<IUser,ChunkedIntervalList>();
	
	/**
	 * Tit-for-tat information and alike..
	 */
	private final Map<IUser,BehaviourData> behaviourData = new HashMap<IUser,BehaviourData>(); 

	public FileDistributionTracker(FileDQE trackedFile) {
		super();
		this.trackedFile = trackedFile;
	}
	
	public void addChunkInfo(IUser peer,ChunkedIntervalList fileInfo) {
		synchronized(synch) {
			if (fileInfo.isComplete()) {
				chunksOfPeers.remove(peer);
				behaviourData.remove(peer);
			} else {
				chunksOfPeers.put(peer, fileInfo);
				behaviourData.put(peer, new BehaviourData());
			}
		}
	}
	
	
	public static class BehaviourData {
		
	}
	
	
	
	
}
