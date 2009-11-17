package eu.jucy.gui.statusline;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import helpers.SizeEnum;



import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;


import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import org.eclipse.ui.services.IServiceLocator;

import eu.jucy.gui.Lang;
import eu.jucy.gui.representation.PresentationImages;
import eu.jucy.gui.transferview.TransferColumns.UserColumn;


import uc.PI;
import uc.files.transfer.AbstractFileTransfer;
import uihelpers.SUIJob;

public class TotalSpeedLabel extends CLabel implements IStatusLineComp {

	
	private final boolean up;

	
	
	public TotalSpeedLabel(Composite comp,boolean up) {
		super(comp,SWT.BORDER);//up? UPID:DOWNID);
		this.up = up;
		new SUIJob() {
			@Override
			public void run() {
				if (!isDisposed()) {
					setText();
					schedule(1000);
				}
			}
		}.schedule(1000);
		setText();
		
		
		MenuManager menuManager = new MenuManager();
		menuManager.add(new SpeedContrib(up));
		Menu menu = menuManager.createContextMenu(this);
		setMenu(menu);
		

		setImage(up?UserColumn.UploadIcon:UserColumn.DownloadIcon);
	
	}
	

	
	public void setText() {
		int limit = PI.getInt(up? PI.uploadLimit: PI.downloadLimit);
		String text= limit > 0? "["+ SizeEnum.getShortSize(1024*limit)+"]":"";// = up? "U": "D";
		text += SizeEnum.toSpeedString(1000, AbstractFileTransfer.getTotalSpeed(up));

		setText(text);
	}


	public int getNumberOfCharacters() {
		return 21; //16
	}

	public static class SpeedContrib extends CompoundContributionItem {

		private final boolean up;

		
		public SpeedContrib(boolean up) {
			super();
			this.up = up;
		}
		
	
		
		
		@Override
		protected IContributionItem[] getContributionItems() {
			List<IContributionItem> topLevel = new ArrayList<IContributionItem>();
			
			int c = PI.getInt(up? PI.uploadLimit: PI.downloadLimit);
			int incmin = Math.max(1, c/100);
			for (int i : new int[] {
					Math.min(c-20*incmin,c/2),
					c-10*incmin,
					c-5*incmin,
					c-2*incmin,
					c-1*incmin,
					c,
					c+1*incmin,
					c+2*incmin,
					c+5*incmin,
					c+10*incmin,
					Math.max(c+20*incmin,2*c)}) {
				
				if (i > 0) {
					topLevel.add(create(i,SizeEnum.toSpeedString(1000,i*1024),i == c));
				}
			}
			topLevel.add(new Separator());
			topLevel.add(create(0,Lang.Unlimited,false));
			
			return topLevel.toArray( new IContributionItem[]{});
		}
		
		private IContributionItem create(int speed,String s,boolean marker) {
			IServiceLocator sl = PlatformUI.getWorkbench();
			CommandContributionItemParameter ccip = 
				new CommandContributionItemParameter(sl, null,
						SetSpeedHandler.COMMAND_ID,SWT.PUSH);
		
			Map<String,String> map = new HashMap<String,String>();
			ccip.parameters = map;
			map.put(SetSpeedHandler.SPEED,""+speed);
			map.put(SetSpeedHandler.UPLIMIT,""+up);

			ccip.label = s;
			if (marker) {
				ccip.icon = PresentationImages.getImageDescriptor(PresentationImages.CLOSE_VIEW);
			}
			
			return new CommandContributionItem(ccip);
		}

		
		
	}
	

}
