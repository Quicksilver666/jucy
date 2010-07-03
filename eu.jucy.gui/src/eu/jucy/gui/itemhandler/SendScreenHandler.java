package eu.jucy.gui.itemhandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;


import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.Lang;
import eu.jucy.gui.texteditor.UCTextEditor;

import uc.PI;

/**
 * Creates and sends a screenshot via PM or mainchat to another user..
 * 
 * @author Quicksilver
 *
 */
public class SendScreenHandler extends AbstractHandler {
	
	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	public static final String COMMAND_ID ="eu.jucy.gui.itemhandler.send_screen";
	public static final String SEND_BOUNDS = "SEND_BOUNDS";
	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Display display = Display.getCurrent();
		GC gc = new GC(display);
		String bound = event.getParameter(SEND_BOUNDS);
		if (bound == null) {
			bound = "";
		}
		Rectangle rect = GuiHelpers.fromString(bound);
		logger.debug("rect: "+rect+"  "+bound);
		if (rect == null) {
			rect = display.getBounds();
		}
	    Image image = new Image(display, rect);
	    
	    gc.copyArea(image, rect.x, rect.y);
	    gc.dispose();
	    
	    
	    ImageLoader loader = new ImageLoader();
	    loader.data = new ImageData[] {image.getImageData()};
	    
	    int i = 0;
	    File screenshot;
	    while ((screenshot = new File(PI.getTempPath(),"Screenshot."+i+".png")).isFile() ) {
	    	i++;
	    }
	    logger.debug("Storing file: "+screenshot);
	    
	    loader.save(screenshot.toString(), SWT.IMAGE_PNG);
	    image.dispose();
	    screenshot.deleteOnExit();
	    
	    
	    UCTextEditor textEdit = (UCTextEditor) HandlerUtil.getActiveEditorChecked(event);
	    textEdit.dropFile(screenshot, false);
	
	    
		return null;
	}
	
	public static class ScreenShotContributions extends CompoundContributionItem  {
		
		
		@Override
		public IContributionItem[] getContributionItems() {
			List<IContributionItem> items = new ArrayList<IContributionItem>();
			
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			
			Display display = Display.getCurrent();
			Monitor[] monitors = display.getMonitors();
			if (monitors.length > 1) {
				CommandContributionItemParameter cpAll = 
					new CommandContributionItemParameter(window, null,
							COMMAND_ID,SWT.PUSH);
				cpAll.label = Lang.All_cpscreen; 
				
				cpAll.icon = fromDisplay(Display.getCurrent().getBounds());
				
				items.add(new CommandContributionItem(cpAll));
				items.add(new Separator());
			}

			int i = 0;
			for (Monitor m : monitors) { 
				CommandContributionItemParameter ccip = 
					new CommandContributionItemParameter(window, null,
							COMMAND_ID,SWT.PUSH);
				i++;
				ccip.label = String.format(Lang.ScreenXYZ,i,m.getBounds().width,m.getBounds().height);		ccip.parameters = Collections.singletonMap(
						SEND_BOUNDS,m.getBounds().toString());
				
				
				
				ccip.icon = fromDisplay(m.getBounds()); 
		
				
				items.add(new CommandContributionItem(ccip));
				items.add(new Separator());
				
			}
			
			return items.toArray(new IContributionItem[]{});
		}
	}
	
	private static ImageDescriptor fromDisplay(Rectangle rect) {
		Display display = Display.getCurrent();
		GC gc = new GC(display);

		Image image = new Image(display, rect);
		gc.copyArea(image, rect.x, rect.y);
		
		int x = 128;
		int y =(int) (((double)x/rect.width) *rect.height); 
	
		
		Image small = new Image(display, x,y);
		gc.dispose();
		
		gc = new GC(small);
		gc.setInterpolation(SWT.HIGH);
		gc.drawImage(image, 0, 0, rect.width, rect.height, 0, 0, x, y);
		gc.dispose();
		image.dispose();
		
		ImageDescriptor id = ImageDescriptor.createFromImageData(small.getImageData());
		small.dispose();
		return id;
	}
}
