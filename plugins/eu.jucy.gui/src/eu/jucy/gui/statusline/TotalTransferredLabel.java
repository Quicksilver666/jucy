package eu.jucy.gui.statusline;

import helpers.IObservable;
import helpers.SizeEnum;
import helpers.StatusObject;
import helpers.Observable.IObserver;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;

import org.eclipse.swt.widgets.Composite;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;

import uc.files.IUploadQueue;
import uihelpers.SUIJob;


/**
 * label that watches the Transfers .. when ever a transfer
 * is finished the total transferred information changes.. and will be presented to the User
 * 
 * 
 * @author Quicksilver
 *
 */
public class TotalTransferredLabel extends CLabel implements IObserver<StatusObject> , IStatusLineComp{



	
	private final boolean upload;
	//private CLabel cl;
	private final IUploadQueue queue;
	
	public TotalTransferredLabel(Composite comp,boolean upl) {
		super(comp,SWT.BORDER);  //upload?UPID:DOWNID
		this.upload = upl;
		this.queue = ApplicationWorkbenchWindowAdvisor.get().getUpDownQueue(upload);
		queue.addObserver(this);
		setText(); //(upload? "U": "D")+": 999,99 MiB" 
	}
	
	public void dispose() {
		queue.deleteObserver(TotalTransferredLabel.this);
		super.dispose();
	}

	
	
	

	public void update(IObservable<StatusObject> o, StatusObject arg) {
		new SUIJob(this) {
			@Override
			public void run() {
				setText();
			}
		}.schedule();
		
	}
	
	public void setText() {
		String text = upload? "U": "D";
		text+=": "+ SizeEnum.getReadableSize(queue.getTotalSize());
		setText(text);
	}

	public int getNumberOfCharacters() {
		return 13; 
	}

	

}
