package uihelpers;

import helpers.GH;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import javax.swing.Icon;
import javax.swing.ImageIcon;


import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;




/**
 * Singleton that will manage loaded file icons..
 * --> outsourced from the Editor icon providers..
 * 
 * @author quicksilver
 *
 */
public class IconManager {

	private static final class ManagerHolder {
		private static final IconManager singleton = new IconManager();
	}
	
	private static final PaletteData PALETTE_DATA = new PaletteData(0xFF0000, 0xFF00, 0xFF);
	
	private final 	Image folder ;
	private final 	Image unknownFile;  
		
	
	private static final Map<String,File> fileEnding = Collections.synchronizedMap(new HashMap<String,File>());
	
	private final Map<String,Image> imageCache = new HashMap<String,Image>();
	
	private IconManager() {
		//load unknown file icon and unknown folder icon
		
		
		File folder = new File(Platform.getInstanceLocation().getURL().getFile());//new File("unknownFolder");
		File f = new File(folder,"unknownFile");
		Image unknownFileImage = null;
		Image folderImage = null;
		try {
			
			if (folder.isDirectory() || folder.mkdirs()) {
				folderImage = loadImageFromFile(folder,null);
			}
			
			f.createNewFile();
			
			unknownFileImage = loadImageFromFile(f,null);
			
			if (!f.delete()) {
				f.deleteOnExit();
			}

		
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("File: "+f);
		}
		if (unknownFileImage == null) {
			unknownFileImage = AbstractUIPlugin.imageDescriptorFromPlugin(
					IImageKeys.PLUGIN_ID, 
					IImageKeys.UNKNOWNFILE_ICON).createImage();
		}
		if (folderImage == null) {
			folderImage = AbstractUIPlugin.imageDescriptorFromPlugin(
					IImageKeys.PLUGIN_ID, 
					IImageKeys.FOLDER_ICON).createImage(); 
		}
		
		
		this.unknownFile = unknownFileImage;
		this.folder = folderImage;
	}
	
	public static IconManager get() {
		return ManagerHolder.singleton;
	}
	
	
	public Image getIconByFilename(String filename) {
		int i = filename.lastIndexOf('.');
		if (i == -1) {
			return getIcon("");
		} else {
			return getIcon(filename.substring(i+1));
		}
	}
	
	/**
	 * this function may only be accessed by the UI thread
	 * 
	 * will return an item for the provided file ending
	 * with and without a dot is accepted.. i.e. ".avi" or "avi"
	 * @param ending a file ending
	 * @return an unknown image if no image can be determined..
	 * else the matching image for the ending..
	 */
	public Image getIcon(String ending) {
		if (GH.isEmpty(ending)) {
			return unknownFile;
		} else if (ending.charAt(0) == '.') {
			return getIcon(ending.substring(1));
		} else if (imageCache.containsKey(ending)){
			return imageCache.get(ending);
		} else if (fileEnding.containsKey(ending)) {
			File f = fileEnding.get(ending);
			Image fileIcon = null;
			if (f.isFile()) {
				fileIcon = loadImageFromFile(f,unknownFile);
			} else {
				fileEnding.remove(ending);
			}
			
			if (fileIcon != null) {
				imageCache.put(ending, fileIcon);
				return fileIcon;
			}
		} else {
			File f = null;
			try {
				File dir = new File(Platform.getInstanceLocation().getURL().getFile());
				f = new File(dir, "iconfile."+ending);
				f.createNewFile(); 
				Image fileIcon = loadImageFromFile(f,unknownFile);
				if (!f.delete()) {
					f.deleteOnExit();
				}
				if (fileIcon != null) {
					imageCache.put(ending, fileIcon);
					return fileIcon;
				}
				
			
			} catch(IOException ioe) {
				ioe.printStackTrace();
				if (f != null) {
					System.err.println(f);
					String path = f.getPath();
					for (int i= 0 ; i < path.length(); i++) {
						System.err.print(" "+ (int)path.charAt(i));
					}
				}
			}
		}
		
		return unknownFile;
	}
	
	/**
	 * 
	 * @return an icon to represent a folder..
	 */
	public Image getFolderIcon() {
		return folder;
	}
	
	
	/**
	 * loads source files and folders  of the provided folder
	 * and its children
	 * maps them to file endings so the files can be used 
	 * to create images..
	 * @param sourcefolder from where on files should be searched
	 * for producing images.. 
	 */
	public static void loadImageSources(File sourcefolder) {
		if (!sourcefolder.isDirectory()) {
			throw new IllegalArgumentException("provided path is not an existing folder "+sourcefolder);
		}
		File[] children =sourcefolder.listFiles();
		if (children != null) {
			for (File f:children) {
				if (f.isFile()) {
					checkFileForLoading(f);
				} else if (f.isDirectory()) {
					loadImageSources(f);
				}
			}
		}
	}
	
	/**
	 * will add the file to the caching MashMap..
	 * maps a local file to an ending
	 * so it can  be retrieved for loading images..
	 */
	private static void checkFileForLoading(File f) {
		int i = f.getName().lastIndexOf('.');
		if (i != -1) {
			String ending = f.getName().substring(i+1);
			if (!fileEnding.containsKey(ending)) {
				fileEnding.put(ending, f);
			}
		}
	}
	
	/**
	 * will load the image of the provided file to RAM
	 * @param f  - the file on the disc that will be used for loading
	 * @return the image that was loaded..
	 */
	private Image loadImageFromFile(File f,Image defaultImage) {
		//System.out.println("f: "+f);
	
		Icon ic = GH.getFileSystemView().getSystemIcon(f);

			
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		try {	
			
			return convert(window.getShell().getDisplay(),toBufferedImage(ic));  // toBufferedImage(i.getImage()));
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	
		return defaultImage ;
	}
	

	
	/**
	 * 
	 */
	private static BufferedImage toBufferedImage(Icon icon) {
		
		java.awt.Image image;
		 if (icon instanceof ImageIcon) {
             image = ((ImageIcon)icon).getImage();
         } else {
             int w = icon.getIconWidth();
             int h = icon.getIconHeight();
             GraphicsEnvironment ge =  GraphicsEnvironment.getLocalGraphicsEnvironment();
             GraphicsDevice gd = ge.getDefaultScreenDevice();
             GraphicsConfiguration gc = gd.getDefaultConfiguration();
             BufferedImage img = gc.createCompatibleImage(w, h);
             Graphics2D g = img.createGraphics();
             icon.paintIcon(null, g, 0, 0);
             g.dispose();
             image = img;
         }
		//following code taken from  The Java Developers Almanac
		
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }
    
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();
    
        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);
    
        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }
    
            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                image.getWidth(null), image.getHeight(null), transparency);
            
        
            
        } catch (HeadlessException e) {
            // The system does not have a screen
        }
    
        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }
    
        // Copy image to buffered image
        Graphics g = bimage.createGraphics();
    
        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();
    
        return bimage;
    }
	
	/**
	 * code taken from The Java Developers Almanac
	 */
    // This method returns true if the specified image has transparent pixels
    private static boolean hasAlpha(java.awt.Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }
    
        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
         PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }
    
        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }
	
	 /**
	  * Credit goes to (I think) Jody Schofield for this method.
	  *
	  * @param org.eclipse.swt.widgets.Display
	  * @param java.awt.image.BufferedImage
	  * @return org.eclipse.swt.graphics.Image
	  */
	 private static final Image convert(Display display, BufferedImage bufferedImage)
	 {
	     ImageData imageData = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), 24, PALETTE_DATA);
	
	     int scansize = (((bufferedImage.getWidth() * 3) + 3) * 4) / 4;
	
	     WritableRaster alphaRaster = bufferedImage.getAlphaRaster();
	     byte[] alphaBytes = new byte[bufferedImage.getWidth()];
	
	     for (int y = 0; y < bufferedImage.getHeight(); y++) {
	    	 
	         int[] buff = bufferedImage.getRGB(0, y, bufferedImage.getWidth(),
	                 1, null, 0, scansize);
	         imageData.setPixels(0, y, bufferedImage.getWidth(), buff, 0);
	
	         if (alphaRaster != null) {
	             int[] alpha = alphaRaster.getPixels(0, y, bufferedImage.getWidth(), 1, (int[]) null);
	             for (int i = 0; i < bufferedImage.getWidth(); i++) {
	                 alphaBytes[i] = (byte) alpha[i];
	             }
	             imageData.setAlphas(0, y, bufferedImage.getWidth(), alphaBytes, 0);
	         }
	     }
	     bufferedImage.flush();
	
	     return new Image(display, imageData);
	 } 
	
	
	/**
	 * creates image descriptor from a local file..
	 * if the file does not exist it will instead return image form the ending of the provided file 
	 * @param f  the file to provide an image for
	 * @return IMagedescriptor.. or null
	 */
	public ImageDescriptor getImageDescriptorFromFile(File f) {
		 Image img = null;
		 if (f.exists()) {
			 img = loadImageFromFile(f, null);

		 } else {
			 String ending = f.getName();
			 int i = ending.lastIndexOf('.');
			 if (i != -1) {
				 ending = ending.substring(i+1);
			 }
			 img = getIcon(ending);
		 }
		 if (img != null) {
			 return ImageDescriptor.createFromImage(img);
		 } else {
			 return null;
		 }
	 }
	 /*public static class FSImageDescriptor extends ImageDescriptor {

		 private final File f;
		public FSImageDescriptor(File f) {
			this.f= f;
		}
		@Override
		public ImageData getImageData() {
			Image img = IconManager.get().loadImageFromFile(f, null);
			if (img == null) {
				return null;
			}
			ImageData imgd = img.getImageData();
			img.dispose();
			
			return imgd;
		}
		 
	 } */
	 
}
