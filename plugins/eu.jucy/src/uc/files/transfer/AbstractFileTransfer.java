/**
 * 
 */
package uc.files.transfer;





import helpers.IObservable;
import helpers.Observable;
import helpers.StatusObject;
import helpers.StatusObject.ChangeType;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;
import org.apache.log4j.Logger;



import uc.ConnectionHandler;
import uc.DCClient;
import uc.IUser;
import uc.protocols.Compression;
import uc.protocols.ConnectionState;
import uc.protocols.Compression.ICompIO;
import uc.protocols.client.ClientProtocol;

/**
 * 
 * Replacement for the old FileTransfer..
 * as it lacked to many things..
 * 
 * @author Quicksilver
 *
 */
public abstract class AbstractFileTransfer extends Observable<TransferChange> implements IFileTransfer {

	private static final Logger logger = LoggerFactory.make();
	
	protected static final int BUFFER_SIZE = 4*1024;

	
	private static CopyOnWriteArrayList<AbstractFileTransfer> active = 
		new CopyOnWriteArrayList<AbstractFileTransfer>();
	
	
	/**
	 * retrieves the total speed of all running transfers..
	 * @param upload - 
	 * @return
	 */
	public static long getTotalSpeed(boolean upload) {
		long totalSpeed = 0;
	//	int counted = 0;
		for (AbstractFileTransfer c: active) {
			if (c.isUpload() == upload) {
				totalSpeed += c.getSpeed();
	//			counted++;
			}
		}

		//logger.info("counted: "+counted+"  "+active.size());
	//	
		return totalSpeed;
	}
	
	
	
	private final FileTransferInformation fti;
	




	private volatile Date starttime;
	/*
	protected CopyOnWriteArrayList<ITransferListener> transferListeners = 
			new CopyOnWriteArrayList<ITransferListener>(); */
	
	private volatile boolean finished = false;
	
	/**
	 * estimates the current speed..
	 */
	private volatile long currentSpeed;
	
	private static final float alpha = 0.9f ;
	private volatile long currentSmoothedSpeed;
	
	
	private volatile float currentCompressionRatio = 1.0f;
	
	protected ICompIO compressor;
	
	protected final ClientProtocol cp;
	

	
	protected AbstractFileTransfer(FileTransferInformation fti,ClientProtocol cp) throws IOException {
		this.cp = cp;
		this.fti = fti;
		if (!fti.isValid()) {
			throw new IOException("could not create a transfer from a non valid transfer info");
		}
		
		addObserver(new ServiceListener(cp.getDcc(),cp.getCh()));
//		registerTransferListener();
	}
	
	/**
	 * transfers the data to or from the provided internet connection
	 * @param internetConnection - ususally a socket to the other peer.
	 * @throws IOException
	 */
	public abstract void transferData(ByteChannel internetConnection) throws IOException ;
	

	
	
	public String getNameOfTransferred() {
		return fti.getNameOfTransferred();
	}

	
	public IUser getOther() {
		return fti.getOther();
	}

	
	public long getSpeed() {
		return currentSmoothedSpeed; //currentSpeed;
	}

	
	public Date getStartTime() {
		if (starttime != null) {
			return new Date(starttime.getTime());
		} else {
			return new Date(); //say now as it will start soon..
		}
	}
	
	public boolean hasStarted() {
		return starttime != null;
	}
	
	public abstract long getBytesTransferred();

	
	public Compression getCompression() {
		return fti.getCompression();
	}
	
	public FileTransferInformation getFti() {
		return fti;
	}

	/*
	 * 
	 * @return compression ration on the transfers..
	 */
	public float getCompressionRatio() {
		return currentCompressionRatio;
	}
	
	/*
	 * updates compression ratio to new value..
	 */
	protected void updateCompressionRatio() {
		long currentComp = compressor.getCompIO();
		long current = compressor.getIO();
		currentCompressionRatio = (float)currentComp/current;
	} 
	
	
	public long getTimeRemaining() {
		
		long speed = currentSpeed;
		if (speed == 0 || currentSmoothedSpeed == 0 ) {
			if (finished) {
				return 0;  // zero seconds for finished transfers
			} else {
				return Long.MAX_VALUE;
			}
		}
		long remaining =  getFileInterval().length() - getFileInterval().getRelativeCurrentPos();
		
		
		return remaining / currentSmoothedSpeed; 
	}

	
	public boolean isUpload() {
		return fti.isUpload();
	}
	
	public boolean isDownload() {
		return fti.isDownload();
	}

	

	/**
	 * client protocol responsible for this transfer.
	 */
	public ClientProtocol getClientProtocol() {
		return cp;
	}



	/**
	 * used by the abstractFileTransfer itself
	 * to set values like the 
	 * time remaining or the startTime or
	 * the speed..
	 * 
	 *  easier for speed would be using
	 * current = a*oldvalue + (1-a) * newValue  changing alpha to to set adaption to changes.. 
	 * may be interesting for a longterm speed assumption...
	 * 
	 * @author Quicksilver
	 *
	 */
	class ServiceListener implements IObserver<TransferChange> {

		private static final int TIMESSPEEDMAYBEZERO = 120; //means speed may be 0 for one minute
		
		private int timesSpeedIsZero = 0; 
		private final Runnable update = new Runnable() {
			public void run() {
				notifyObservers(TransferChange.BYTESTRANSFERRED);
			}
		};
		private ScheduledFuture<?> sf;
		private final DCClient dcc;
		
		private final ConnectionHandler ch;
		
		public ServiceListener(DCClient dcc,ConnectionHandler ch){
			this.dcc = dcc;
			this.ch = ch;
		}
		

		/**
		 * list for measuring average speed over the last seconds.. 
		 */
		private final LinkedList<TimePositionTuple> measured = new LinkedList<TimePositionTuple>();
		

		
		public synchronized void update(IObservable<TransferChange> fileTransfer, TransferChange change) {
			switch(change) {
			case STARTED:
		//		logger.info("started has fired.."+getOther()+"  "+AbstractFileTransfer.this.cp.getState());
				sf = dcc.getSchedulerDir().scheduleAtFixedRate(update, 500, 500, TimeUnit.MILLISECONDS);
				starttime = new Date();
				active.addIfAbsent(AbstractFileTransfer.this);
				ch.notifyTransferObservers(new StatusObject(AbstractFileTransfer.this, ChangeType.ADDED));
				break;
			case BYTESTRANSFERRED:

				TimePositionTuple current = new TimePositionTuple(getFileInterval().getCurrentPosition());

				measured.add(current);
				while (measured.size() > 30) { 
					measured.removeFirst();
				}
				TimePositionTuple old = measured.getFirst();
				currentSpeed = old.calcSpeed(current);
				
				currentSmoothedSpeed =(long)(currentSmoothedSpeed * alpha + currentSpeed * (1-alpha));
				
				
				if (currentSpeed == 0) {
					timesSpeedIsZero++;

					if (timesSpeedIsZero > TIMESSPEEDMAYBEZERO) {
						timesSpeedIsZero = 0;

						if (AbstractFileTransfer.this.cp.getState() == ConnectionState.DESTROYED) {
							logger.debug("Speed zero "+getOther()+"  "+AbstractFileTransfer.this.cp.getState());
							update(AbstractFileTransfer.this,TransferChange.FINISHED);
							logger.debug("unnormal finished called "+getOther());
						} else {
							cancel();
						}
					}
				} else {
					timesSpeedIsZero = 0;
				}
				updateCompressionRatio();
				ch.notifyTransferObservers(new StatusObject(AbstractFileTransfer.this, ChangeType.CHANGED));
				break;
			case FINISHED:
				logger.debug("finished has fired: "+getOther()+"  "+AbstractFileTransfer.this.cp.getState()+"  "+timesSpeedIsZero);
				fti.getOther().deleteConnection(AbstractFileTransfer.this.cp); 
				sf.cancel(false);
				currentSpeed = 0;
				finished = true;
				active.remove(AbstractFileTransfer.this);
				ch.notifyTransferObservers(new StatusObject(AbstractFileTransfer.this, ChangeType.REMOVED));
				break;
			}

		}
	}
	
	private static class TimePositionTuple {
		
		private final long position;
		private final long date;
		
		public TimePositionTuple(long position) {
			this.position = position;
			this.date = System.currentTimeMillis();
		}
		
		public long calcSpeed(TimePositionTuple current) {
			if (date < current.date) {
				return (current.position - position)*1000/(current.date-date);
			} else {
				return 0;
			}
		}	
	}
	
}



