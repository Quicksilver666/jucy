package uc.files.transfer;

import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;
import org.apache.log4j.Logger;

import uc.DCClient;
import uc.PI;
import uc.protocols.Compression.CompressionWrapper;
import uc.protocols.client.ClientProtocol;

public class Download extends AbstractFileTransfer {

	private static final int DRAINTIME = 20;
	private static Semaphore downloads = new Semaphore(Integer.MAX_VALUE);
	private static volatile int maxSpeed;
	
	private static void update() {
		maxSpeed = PI.getInt(PI.downloadLimit);
		if (maxSpeed <= 0) {
			maxSpeed = Integer.MAX_VALUE /DRAINTIME ;
		}
	}
	
	static {
		new PreferenceChangedAdapter(PI.get(),PI.downloadLimit) {
			@Override
			public void preferenceChanged(String preference,String oldValue, String newValue) {
				update();
			}
		};
		
		update();
		DCClient.getScheduler().scheduleAtFixedRate(new Runnable() {		
			private int i = 0;
			private int overDue = 0;
			public void run() {
				if (++i % DRAINTIME == 0 ) {
					downloads.drainPermits();
				}
				downloads.release((maxSpeed+overDue)/10);
				overDue = (maxSpeed+overDue)%10;
			}
				
		},1,100,TimeUnit.MILLISECONDS);
	}
	
	
	
	private static final Logger logger = LoggerFactory.make(); 
	

	
	private volatile long bytesDownloaded;
	
	/**
	 * the interval where we are writing to
	 */
	private final AbstractWritableFileInterval wfc;
	
	private volatile ReadableByteChannel source;
	private volatile ByteChannel socketChannel; //channel representing the internet connection 
	
	public Download(FileTransferInformation fti,ClientProtocol cp, AbstractWritableFileInterval wfc) throws IOException {
		super(fti,cp);
		this.wfc = wfc;
	}


	public void cancel() {
		logger.debug("Cancelling download");
		GH.close(socketChannel,source);
	}


	public AbstractFileInterval getFileInterval() {
		return wfc;
	}
	
	

	@Override
	public boolean isUpload() {
		return false;
	}

	/**
	 * starts the upload..
	 */
	@Override
	public void transferData(ByteChannel src) throws IOException {
		logger.debug("in Download.transferData()");
		this.socketChannel = src;
	
		CompressionWrapper cw = getCompression().wrapIncoming(src);
		this.compressor = cw;
		source = cw.getRbc();  //add compression
		WritableByteChannel target = wfc.getWriteChannel();
		
		ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
		bb.clear();
		try {
			notifyObservers(TransferChange.STARTED);
			logger.debug("start transferring");
			int written;
			int toAcquire = 0;
		
			while ((source.isOpen() && source.read(bb) >= 0) || bb.position() != 0) { //second is for writing to  
				bb.flip();
				
				if ((written = target.write(bb)) < 0) {  //if no bytes can be written... break
					break;
				}
				bytesDownloaded += written;
				toAcquire += written;
				downloads.acquireUninterruptibly(toAcquire / 1024);
				toAcquire %= 1024 ;
			
				bb.compact();
				//break if no more bytes are left..
				if (wfc.getRelativeCurrentPos() == wfc.length()) {
					break;
				} 
			}
		
		} finally {
		//	logger.info("end download: "+wfc.getCurrentPosition() + "  "+wfc.length());
			GH.close(target); //source is not closed..
			
			notifyObservers(TransferChange.FINISHED);
			logger.debug("end transferData()");
		}
		
	}

	@Override
	public long getBytesTransferred() {
		return bytesDownloaded;
	}
	
	
}
