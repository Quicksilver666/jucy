package mylyntoaster;

import org.eclipse.mylyn.internal.provisional.commons.ui.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;


public class Toaster extends AbstractNotificationPopup {

	private final Image img;
	private final String message;
	
	public Toaster(Display display,Image icon,String message,long milliseconds) {
		super(display,milliseconds);
		this.img = icon;
		this.message = message;
		setFadingEnabled(true);
	}

	@Override
	protected void createContentArea(Composite parent) {
		parent.setLayout(new FillLayout());
		
		CLabel label = new CLabel(parent, SWT.None);
		label.setText(message);
		label.setImage(img);
	}
	

}
