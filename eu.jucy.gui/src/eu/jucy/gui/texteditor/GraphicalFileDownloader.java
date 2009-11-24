package eu.jucy.gui.texteditor;


import java.io.IOException;
import java.net.URL;


import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import uihelpers.SUIJob;

public class GraphicalFileDownloader {

	private static final Logger logger = LoggerFactory.make();
	
	
	private final URL url;
	private final ObjectPoint<Control> point;
	private final URLTextModificator mod;
	/**
	 * 
	 * @param url - where the image is..
	 * 
	 * @param where - the StyledText where the URL is located 
	 * @param positionx - point.x contains the carret offset for the position
	 */
	public GraphicalFileDownloader(URL url,ObjectPoint<Control> point,URLTextModificator mod) {
		this.url = url;
		this.point = point;
		this.mod = mod;
	}
	
	
	public void start() {
		point.obj.setEnabled(false);
		new Job("loading image") {
			public IStatus run(IProgressMonitor monitor) {
				try {
					ImageDescriptor imagedesc = ImageDescriptor.createFromURL(url);
					final ImageData imagedata = imagedesc.getImageData();
					if (imagedata == null) {
						throw new IOException("failed loading image");
					}
					
					new SUIJob() {
						@Override
						public void run() {
							Image image =  scaleIfNeeded(imagedata);
							mod.addLabelReplacementImage(point.x, url.toString(), image);
							return;
						}
					}.schedule();
				} catch(Exception e) {
					try {
					
						IWorkbenchBrowserSupport browserSupport =
							PlatformUI.getWorkbench().getBrowserSupport();
						
						IWebBrowser  browser = browserSupport.createBrowser("myid");
						browser.openURL(url);
						
					} catch (PartInitException io2) {
						logger.warn(io2, io2);
					}
					logger.debug(e,e);
				} finally {
					new SUIJob() {
						@Override
						public void run() {
							if (!point.obj.isDisposed()) {
								point.obj.setEnabled(true);
							}
						}
						
					}.schedule();
				}
				
				return Status.OK_STATUS;
			}
		}.schedule();
	}
	
	public static Image scaleIfNeeded(ImageData  imaged) {
		if (imaged.width > 500 || imaged.height > 800 ) {
			double scale = Math.max(imaged.width / 500d, imaged.height/800d);
			
			imaged = imaged.scaledTo((int)(imaged.width/scale),(int) (imaged.height/scale));
		}
		return  ImageDescriptor.createFromImageData(imaged).createImage();
	}
	
}
