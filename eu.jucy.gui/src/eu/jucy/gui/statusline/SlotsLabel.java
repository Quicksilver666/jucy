package eu.jucy.gui.statusline;

import java.util.Set;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;

import uc.DCClient;
import uc.InfoChange;
import uc.InfoChange.IInfoChanged;
import uihelpers.SUIJob;

public class SlotsLabel extends CLabel implements IInfoChanged, IStatusLineComp {

	public static final String ID= "eu.jucy.gui.statusline.SlotsLabel";
	
	

	public SlotsLabel(Composite comp) {
		super(comp,SWT.BORDER|SWT.CENTER);
		ApplicationWorkbenchWindowAdvisor.get().register(this);
		setText(); //"Slots: 99/99"
	}

	
	/*@Override
	public void fill(Composite parent) {
	//	cl = new CLabel(parent,SWT.BORDER);
	//	StatusLineLayoutData data = new StatusLineLayoutData();
	//	data.widthHint = 60;
	//	cl.setLayoutData(data);
		

		
		
	} */
	
	public void dispose() {
		ApplicationWorkbenchWindowAdvisor.get().unregister(this);
		super.dispose();
		
	}

	public void infoChanged(Set<InfoChange> type) {
		
		if (type.contains(InfoChange.CurrentSlots) || type.contains(InfoChange.Slots)) {
			new SUIJob(this) {
				@Override
				public void run() {
					setText();
				}
			}.schedule();
		}
	}
	
	public void setText() {
		DCClient dcc= ApplicationWorkbenchWindowAdvisor.get();
		int cur = dcc.getCurrentSlots();
		int max = dcc.getTotalSlots();
		
		
		String text = String.format("S: %d/%d", cur,max);// "Slots: "+cur+"/"+max;
	//	text = String.format("%-8s", text);
		setText(text);
	}
	
	public int getNumberOfCharacters() {
		return 8;
	}
	
}
