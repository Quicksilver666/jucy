package eu.jucy.gui.itemhandler;

import java.io.File;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GuiHelpers;

import uc.PI;

/**
 * Creates and sends a screenshot via PM or mainchat to another user..
 * 
 * @author Quicksilver
 *
 */
public class SendScreenHandler extends AbstractHandler {
	
	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	public static final String ID="eu.jucy.gui.itemhandler.SendScreen";
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
		if (rect == null) {
			rect = display.getBounds();
		}
	    Image image = new Image(display, rect);
	    gc.copyArea(image, rect.x, rect.y);
	    gc.dispose();
	    
	    
	    ImageLoader loader = new ImageLoader();
	    loader.data = new ImageData[] {image.getImageData()};
	    
	    File screenshot = new File(PI.getTempPath(),"Screenshot."+System.currentTimeMillis()+".png");
	    loader.save(screenshot.toString(), SWT.IMAGE_PNG);
	    
	    //TODO here get where we are...?better use the already present methods.. not this
	    //i.e. throw file in function that handles drops
	
	    image.dispose();    
		return null;
	}

}
