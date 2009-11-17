package eu.jucy.gui.statusline;

import helpers.SizeEnum;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;



import uc.InfoChange;
import uc.InfoChange.IInfoChanged;
import uihelpers.SUIJob;

public class SharesizeLabel extends CLabel implements IInfoChanged ,IStatusLineComp  {

	
	public SharesizeLabel(Composite parent) {
		super(parent,SWT.BORDER|SWT.CENTER);
		setText(); 
		ApplicationWorkbenchWindowAdvisor.get().register(this);
	}

	public void infoChanged(Set<InfoChange> type) {
		if (type.contains(InfoChange.Sharesize)) {
			new SUIJob() {
				@Override
				public void run() {
					if (!isDisposed()) {
						setText();
					} else {
						ApplicationWorkbenchWindowAdvisor.get().unregister(SharesizeLabel.this);
					}
				}
			}.schedule();
		}
		
	}
	
	public void setText() {
		String text = SizeEnum.getReadableSize(ApplicationWorkbenchWindowAdvisor.get().getFilelist().getSharesize());
		setText(text);
	}

	public int getNumberOfCharacters() {
		return 10;
	}
	
}
