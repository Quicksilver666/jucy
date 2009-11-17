package eu.jucy.gui.statusline;

import java.util.Set;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;


import uc.InfoChange;
import uc.InfoChange.IInfoChanged;
import uihelpers.SUIJob;

public class HubsLabel extends CLabel implements IInfoChanged, IStatusLineComp {

	public static final String ID = "eu.jucy.gui.statusline.HubsLabel";
	
	//private CLabel cl;
	
	
	public HubsLabel(Composite comp) {
		super(comp,SWT.BORDER|SWT.CENTER);
		setText(); //"99/99/99");
		ApplicationWorkbenchWindowAdvisor.get().register(this);
	}
	
	
	
	/*
	@Override
	public void fill(Composite parent) {
		cl = new CLabel(parent,SWT.BORDER);
		StatusLineLayoutData data = new StatusLineLayoutData();
		data.widthHint = 50;
		cl.setLayoutData(data);
		

	} */
	

	public void infoChanged(Set<InfoChange> type) {
		
		if (type.contains(InfoChange.Hubs)) {
			new SUIJob() {
				@Override
				public void run() {
					if (!isDisposed()) {
						setText();
					} else {
						ApplicationWorkbenchWindowAdvisor.get().unregister(HubsLabel.this);
					}
				}
			}.schedule();
		}
	}

	public void setText() {
		int[] hubs = ApplicationWorkbenchWindowAdvisor.get().getNumberOfHubs(true);
		//String text = hubs[0]+"/"+hubs[1]+"/"+hubs[2];
		String text = String.format("%d/%d/%d", hubs[0],hubs[1],hubs[2]);
		//text = String.format("%-8s",text);
		setText(text);
	}
	
	public int getNumberOfCharacters() {
		return 8;
	}
	
}
