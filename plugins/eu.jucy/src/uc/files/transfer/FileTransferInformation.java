package uc.files.transfer;

import java.io.File;
import java.io.IOException;



import uc.DCClient;
import uc.IUser;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;

import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.FileDQE;
import uc.files.filelist.FileListFile;
import uc.files.transfer.AbstractReadableFileInterval.FileReadInterval;
import uc.files.transfer.AbstractReadableFileInterval.MemoryReadInterval;
import uc.protocols.Compression;
import uc.protocols.TransferType;
import uc.protocols.client.ClientProtocol;

/**
 * CommandPattern for FileTransfer information..
 * 
 * @author Quicksilver
 *
 */
public class FileTransferInformation  {


//	private static Logger logger = LoggerFactory.make(Level.DEBUG);
	
	private final DCClient dcc;
	
	/**
	 * transfer will happen with this user
	 */
	private IUser other;
	
	/**
	 * what compression that is to be used
	 */
	private Compression compression = Compression.NONE;
	
	/**
	 * start position in the file
	 * 
	 */
	private long startposition;
	
	
	private long length;
	
	/**
	 * if its a file or TTHL transfer
	 * this is the root hash otherwise null
	 */
	private  HashValue hashValue;
	
	
	/**
	 * what type of transfer this is
	 */
	private volatile TransferType type;
	
	
	private volatile String nameOfTransferred;
	
	/**
	 * for a download the DownloadQueueEntry
	 * is needed to gather information on target path and likewise
	 */
	private AbstractDownloadQueueEntry dqe;
	

	/**
	 * true if upload
	 * false for download
	 * null if not yet set.
	 */
	private volatile Boolean download;
	
	
	private AbstractFileInterval fileInterval;
	

	/**
	 * for partial Filelist 
	 * null means whole filelist
	 * if not recursive only single directory is sent..
	 */
	private String fileListSubPath = null;
	private volatile boolean bz2Compressed = false;
	private volatile boolean partialList = false;
	



	private boolean recursive = true; 
	
	
	
	public FileTransferInformation(DCClient dcc) {
		super();
		this.dcc = dcc;
	}

	/**
	 * checks if the gained information is valid and can be used
	 * to create a transfer
	 * @return
	 */
	public boolean isValid() {
		if (isUpload() && isValidForSND() && fileInterval != null) {
			return true;
		}
		
		if (isValidForGet() && fileInterval != null) {
			return true;
		}
		return false; 
	}
	
	public boolean isValidForGet() {
		if (type == null || !isDownload() || other == null) {
			return false;
		}
		
		switch(type) {
		case FILE:
			if (nameOfTransferred == null) { //files need a name
				return false;
			}
			//fall through!
		case TTHL:
			if (hashValue == null) {
				return false;
			}
		case FILELIST:
			return true; //no special requirements..
			
		}
		throw new IllegalStateException();
	}
	
	private boolean isValidForSND() {
		if (type == null  || !isUpload() ||other == null) {
			return false;
		}
		switch(type) {
		case FILE:
			if (nameOfTransferred == null) { //files need a name
				return false;
			}
			//fall through!
		case TTHL:
			if (hashValue == null) {
				return false;
			}
		case FILELIST:
			return true; //no special requirements..
			
		}
		throw new IllegalStateException();
	}


	public IUser getOther() {
		return other;
	}


	public void setOther(IUser other) {
		this.other = other;
	}


	public Compression getCompression() {
		return compression;
	}


	public void setCompression(Compression comp) {
		this.compression = comp;
	}
	
	/**
	 * sets compression for downloading.. what we would prefer..
	 * @param otherIsLocal
	 */
	public void setCompression(boolean otherIsLocal,boolean otherSupportsCompression) {
		switch(type) {
		case FILE:
			setCompression(otherIsLocal||!otherSupportsCompression ? Compression.NONE: Compression.ZLIB_BEST);
			break;
		case TTHL:
			setCompression(Compression.NONE);
			break;
		case FILELIST:
			setCompression(otherSupportsCompression?Compression.ZLIB_FAST:Compression.NONE);
			break;
		}
	}


	public long getStartposition() {
		if (getType() == TransferType.FILE) {
			return startposition;
		} else {
			return 0;
		}
	}


	public void setStartposition(long startposition) {
		this.startposition = startposition;
	}


	public long getLength() {
		return length;
	}


	public void setLength(long length) {
		this.length = length;
	}


	public HashValue getHashValue() {
		return hashValue;
	}


	public void setHashValue(HashValue hashValue) {
		this.hashValue = hashValue;
	}


	public TransferType getType() {
		return type;
	}


	public void setType(TransferType type) {
		this.type = type;
	}


	public String getNameOfTransferred() {
		if (type != null) {
			switch(type) {
			case FILE:
				return nameOfTransferred;
			case FILELIST: 
				return "FileList";
			case TTHL:
				return "TTHL: "+getHashValue();
			}
		}
		
		return nameOfTransferred;
	}

	/**
	 * sets the name of the transferred..
	 * only needed for files.. not for TTHL or Filelist
	 * @param nameOfTransferred
	 */
	public void setNameOfTransferred(String nameOfTransferred) {
		this.nameOfTransferred = nameOfTransferred;
	}


	public Boolean getDownload() {
		return download;
	}
	
	public boolean isUpload() {
		return download != null && !download;
	}

	public boolean isDownload() {
		return download != null && download;
	}

	public void setDownload(Boolean download) {
		this.download = download;
	}

	/**
	 * gets the filepath either source or target ..
	 * for displaying in gui ..
	 * @return Targetpath if download ... sourcepath if upload
	 * null if not applicable
	 */
	public File getFile() {
		File f = null;
		if (isUpload()) {
			TransferType t = getType();
			if (t == null) {
				return null;
			}
			switch(t) {
			case FILE:
			case TTHL:
				f = dcc.getFilelist().getFile(getHashValue());
			}
		} else if (isDownload()) {
			f = dqe.getTargetPath();
		}
		
		return f;
	}
	
	public FileListFile getFileListFile() {
		if (isUpload() && getHashValue() != null) {
			return dcc.getFilelist().get(getHashValue());
		}
		return null;
	}
	
	
	/**
	 * creates a file interval for the upload..
	 * 
	 * @return true if creation was successful
	 */
	public boolean setFileInterval() {
		fileInterval = null;
		if (isUpload()) {
			File f = null;
			switch(getType()) {
			case FILE:
				FileListFile flfile = dcc.getFilelist().get(getHashValue());
				long startpos = getStartposition();
				if (flfile != null && flfile.mayDownload(getOther())) {
					f  = dcc.getFilelist().getFile(flfile);
					if (f != null && f.canRead()) {
						
						if (getLength() == -1) {
							setLength( f.length() - startpos);
						}
						//check if the requested length is not too long..
						if (getLength() + startpos > f.length()) {
							setLength(f.length()- startpos );
						}
						
						fileInterval = new FileReadInterval(f,getStartposition(),getLength()); 
						setNameOfTransferred(f.getName());
					}
				} else {
					AbstractDownloadQueueEntry adqe = dcc.getDownloadQueue().get(getHashValue());
					if (adqe instanceof FileDQE) {
						FileDQE fdqe = (FileDQE)adqe;
						
						if (getLength() == -1 | getLength() > fdqe.getBytesReadable(startpos)) {
							setLength(fdqe.getBytesReadable(startpos));
						}
						
						fileInterval = fdqe.getReadInterval(this);
						setNameOfTransferred(fdqe.getFileName());
					}
				}
				break;
			case FILELIST:
				byte[] filelist = dcc.getOwnFileList().writeFileList(fileListSubPath, recursive,bz2Compressed);

				fileInterval = new MemoryReadInterval(filelist);
				setLength(filelist.length);
				break;
			case TTHL:
				InterleaveHashes interleaves = dcc.getDatabase().getInterleaves(getHashValue(),false);
				if (interleaves != null) {
					fileInterval = AbstractReadableFileInterval.create(interleaves);
					setLength(fileInterval.length());
				} 
				break;
			}
			
		} else if (isDownload()) {
			fileInterval = dqe.getInterval(this); //new WritableFileInterval(dqe,startposition,length);
		}
		
		return fileInterval != null;
	}
	
	/**
	 * create an abstract File transfer from the gathered date.
	 * @return
	 */
	public AbstractFileTransfer create(ClientProtocol cp) throws IOException {
		if (isDownload()) {
			return new Download(this,cp,(AbstractWritableFileInterval)fileInterval);
		} else if (isUpload()){
			return new Upload(this,cp,(AbstractReadableFileInterval)fileInterval);
		}
		
		throw new IllegalStateException();
	}

	public AbstractDownloadQueueEntry getDqe() {
		return dqe;
	}

	public void setDqe(AbstractDownloadQueueEntry dqe) {
		this.dqe = dqe;
	}
	
	public void setPartialFileList(String fileListSubPath,boolean recursive) {
		this.fileListSubPath = fileListSubPath;
		this.recursive  = recursive;
	}
	
	public String getFileListSubPath() {
		return fileListSubPath;
	}

	public boolean isRecursive() {
		return recursive;
	}
	
	
	



	public boolean isBz2Compressed() {
		return bz2Compressed;
	}

	public void setBz2Compressed(boolean bz2Compressed) {
		this.bz2Compressed = bz2Compressed;
	}

	public boolean isPartialList() {
		return partialList;
	}

	public void setPartialList(boolean partialList) {
		this.partialList = partialList;
	}

	@Override
	public String toString() {
		return "FileTransferInformation [other=" + other + ", compression="
				+ compression + ", startposition=" + startposition
				+ ", length=" + length + ", hashValue=" + hashValue + ", type="
				+ type + ", nameOfTransferred=" + nameOfTransferred + ", dqe="
				+ dqe + ", download=" + download + ", fileInterval="
				+ fileInterval + ", fileListSubPath=" + fileListSubPath
				+ ", recursive=" + recursive + "]";
	}


	
}
