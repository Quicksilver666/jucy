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


	//public static final String UPID = "eu.jucy.gui.statusline.TotalUP";
	//public static final String DOWNID = "eu.jucy.gui.statusline.TotalDO";
	
	//private static Font small =null;
	
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

	/*@Override
	public void fill(Composite parent) {
		cl = new CLabel(parent,SWT.BORDER);
		
	 *	if (small == null) {
			FontData standard = cl.getFont().getFontData()[0];
			
			small = new Font(null,new FontData(standard.getName(),7,SWT.NORMAL ));
		}
		cl.setFont(small); *
		
		UploadQueue.get(upload).deleteObserver(this);
		UploadQueue.get(upload).addObserver(this);
		cl.addDisposeListener(new DisposeListener() {
		
			public void widgetDisposed(DisposeEvent e) {
				UploadQueue.get(upload).deleteObserver(TotalTransferredLabel.this);
			}
		});
		StatusLineLayoutData data = new StatusLineLayoutData();
		data.widthHint = 80;
		cl.setLayoutData(data);
		
		setText();
		
		
	}*/
	
	

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
		
	//	text = String.format("%-13s", text); //13
		setText(text);
	}

	public int getNumberOfCharacters() {
		return 13; 
	}

	

}
