package eu.jucy.gui.texteditor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class AnimatedGifLabel extends Label {


	
	private final Display display;

	private GC labelGC;
	Color shellBackground;
	ImageLoader loader;
	ImageData[] imageDataArray;
	Thread animateThread;
	Image image;
	final boolean useGIFBackground = false;
	
	public AnimatedGifLabel(Composite parent, ImageData[] gif) {
		super(parent, SWT.NONE);
		display = parent.getDisplay();
		labelGC = new GC(this);
		animateThread = new Thread("Animation") {
	            public void run() {
	            	
	              /* Create an off-screen image to draw on, and fill it with the shell background. */
	              Image offScreenImage = new Image(display, loader.logicalScreenWidth, loader.logicalScreenHeight);
	              GC offScreenImageGC = new GC(offScreenImage);
	              offScreenImageGC.setBackground(shellBackground);
	              offScreenImageGC.fillRectangle(0, 0, loader.logicalScreenWidth, loader.logicalScreenHeight);
	                
	              try {
	                /* Create the first image and draw it on the off-screen image. */
	                int imageDataIndex = 0;  
	                ImageData imageData = imageDataArray[imageDataIndex];
	                if (image != null && !image.isDisposed()) image.dispose();
	                image = new Image(display, imageData);
	                offScreenImageGC.drawImage(
	                  image,
	                  0,
	                  0,
	                  imageData.width,
	                  imageData.height,
	                  imageData.x,
	                  imageData.y,
	                  imageData.width,
	                  imageData.height);

	                /* Now loop through the images, creating and drawing each one
	                 * on the off-screen image before drawing it on the shell. */
	                int repeatCount = loader.repeatCount;
	                while (loader.repeatCount == 0 || repeatCount > 0) {
	                  switch (imageData.disposalMethod) {
	                  case SWT.DM_FILL_BACKGROUND:
	                    /* Fill with the background color before drawing. */
	                 //   Color bgColor = null;
//	                    if (useGIFBackground && loader.backgroundPixel != -1) {
//	                      bgColor = new Color(display, imageData.palette.getRGB(loader.backgroundPixel));
//	                    }
	                    offScreenImageGC.setBackground( shellBackground);//
	                    offScreenImageGC.fillRectangle(imageData.x, imageData.y, imageData.width, imageData.height);
	                 //   if (bgColor != null) bgColor.dispose();
	                    break;
	                  case SWT.DM_FILL_PREVIOUS:
	                    /* Restore the previous image before drawing. */
	                    offScreenImageGC.drawImage(
	                      image,
	                      0,
	                      0,
	                      imageData.width,
	                      imageData.height,
	                      imageData.x,
	                      imageData.y,
	                      imageData.width,
	                      imageData.height);
	                    break;
	                  }
	                            
	                  imageDataIndex = (imageDataIndex + 1) % imageDataArray.length;
	                  imageData = imageDataArray[imageDataIndex];
	                  image.dispose();
	                  image = new Image(display, imageData);
	                  offScreenImageGC.drawImage(
	                    image,
	                    0,
	                    0,
	                    imageData.width,
	                    imageData.height,
	                    imageData.x,
	                    imageData.y,
	                    imageData.width,
	                    imageData.height);
	                  
	                  /* Draw the off-screen image to the shell. */
	                  labelGC.drawImage(offScreenImage, 0, 0);
	                  
	                  /* Sleep for the specified delay time (adding commonly-used slow-down fudge factors). */
	                  try {
	                    int ms = imageData.delayTime * 10;
	                    if (ms < 20) ms += 30;
	                    if (ms < 30) ms += 10;
	                    Thread.sleep(ms);
	                  } catch (InterruptedException e) {
	                  }
	                  
	                  /* If we have just drawn the last image, decrement the repeat count and start again. */
	                  if (imageDataIndex == imageDataArray.length - 1) repeatCount--;
	                }
	              } catch (SWTException ex) {
	                System.out.println("There was an error animating the GIF");
	              } finally {
	                if (offScreenImage != null && !offScreenImage.isDisposed()) offScreenImage.dispose();
	                if (offScreenImageGC != null && !offScreenImageGC.isDisposed()) offScreenImageGC.dispose();
	                if (image != null && !image.isDisposed()) image.dispose();
	              }
	            }
	          };
	          animateThread.setDaemon(true);
	          animateThread.start();
	        
	}

}
