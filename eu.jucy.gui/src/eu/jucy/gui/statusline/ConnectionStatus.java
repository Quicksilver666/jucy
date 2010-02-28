package eu.jucy.gui.statusline;


import helpers.GH;
import helpers.IObservable;
import helpers.Observable.IObserver;

import java.net.InetAddress;




import org.eclipse.swt.SWT;


import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;




import uc.ConnectionDeterminator.CDState;
import uihelpers.SUIJob;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.texteditor.hub.HubEditor;

public class ConnectionStatus extends Label implements IObserver<String> {

	


	private final int size;
	
	
	
	public ConnectionStatus(Composite comp, int size) {
		super(comp,SWT.NONE);
		Rectangle rect = HubEditor.GREEN_LED.getBounds();
		int originalSize = rect.height;
		
	
		size = Math.min(originalSize, size);
		
		size =  size == originalSize ? SWT.DEFAULT : size;
		if (size != SWT.DEFAULT && size < 20) {
			size = 20;
		}
		this.size = size;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, "ConnectionStatus");
		updateLabel();
		pack();
		ApplicationWorkbenchWindowAdvisor.get().getConnectionDeterminator().addObserver(this);
	}
	

	
	public void update(IObservable<String> o, String arg) {
		new SUIJob(this) {
			@Override
			public void run() {
				updateLabel();
			}
			
		}.schedule();
	}
	
	public void dispose() {
		ApplicationWorkbenchWindowAdvisor.get().getConnectionDeterminator()
			.deleteObserver(ConnectionStatus.this);
		super.dispose();
	}

	
	private void updateLabel() {
		if (ApplicationWorkbenchWindowAdvisor.get().isActive()) {
			CDState cd = ApplicationWorkbenchWindowAdvisor.get().getConnectionDeterminator().getState();
			
			switch (cd.getWarningState()) {
			case 0:
				setImage(HubEditor.GREEN_LED);
				break;
			case 1:
				setImage(HubEditor.YELLOW_LED);
				break;
			case 2:
			case 3:
				setImage(HubEditor.RED_LED);
				break;

			}
			
			InetAddress ia = ApplicationWorkbenchWindowAdvisor.get().getConnectionDeterminator().getPublicIP();
			
			String publicIP = "";
			if (ia != null) {
				publicIP = ia.getHostAddress(); 
			}
			
			
			String s = String.format(Lang.CSToolTip, 
					publicIP,
					(cd.isNatPresent()?	
							Lang.CSNATDetected :
							Lang.CSNoNATDetected),
					getString(cd.getTcpWorking()),
					getString(cd.getTLSWorking()),
					getString(cd.getUdpWorking()),
					getString(cd.getUPnPWorking()) );
			

			String sol = cd.getProblemSolution();
			if (!GH.isEmpty(sol)) {
				s+="\n"+sol;
			}
			
			setToolTipText(s);
			
		
		} else {
			setImage(HubEditor.GREY_LED);
			setToolTipText(Lang.CSPassiveMode);
		}
		redraw();
	}
	
	private static String getString(Boolean b) {
		if (b == null) {
			return Lang.CSUNKNOWN;
		} else if (b) {
			return Lang.CSWORKING;
		}else {
			return Lang.CSNOTWORKING;
		}
	}

	
	public void setImage(Image img) {
		if (size == SWT.DEFAULT) {
			super.setImage(img);
		} else {
			Image old = getImage();
			Image newImage = new Image(img.getDevice(),size,size);
			GC gc = new GC(newImage);
			gc.setBackground(getBackground());
			gc.setForeground(getForeground());
			gc.setAdvanced(true);
			gc.setAntialias(SWT.ON);
			gc.setInterpolation(SWT.HIGH);
			gc.drawRectangle(newImage.getBounds());
			gc.fillRectangle(newImage.getBounds());
			gc.drawImage(img, 0, 0, img.getBounds().width, img.getBounds().height, 0, 0, size, size);
			
			gc.dispose();
			super.setImage(newImage);
			if (old != null) {
				old.dispose();
			}
		}
	}
	

	
	protected void checkSubclass () {}
	
}
