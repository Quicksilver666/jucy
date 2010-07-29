package eu.jucy.gui.texteditor;

import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

public class ObjectPoint<V> {
	
	private static final Logger logger = LoggerFactory.make();
	
	/**
	 * create a StylePoint and add the matching StyleRange
	 * to the ranges list
	 * 
	 * @param x - position in the text
	 * @param length - how many characters the image point is long must be 1 ;-)
	 * @param img
	 * @param ranges
	 * @return
	 */
	public static ObjectPoint<Image> create(int x,int length,String replacementText,Image img,List<StyleRange> ranges) {
		ObjectPoint<Image> ip = new ObjectPoint<Image>();
		ip.x = x;
		ip.length = length;
		ip.replacementText = replacementText;
		ip.obj = img;
		StyleRange style = new StyleRange(x,length,null,null);
		Rectangle rect = img.getBounds();
		style.metrics = new GlyphMetrics(rect.height, 0, rect.width / style.length);
		ranges.add(style);
		return ip;
	}
	
	/**
	 * create a StylePoint and add the matching StyleRange
	 * to the ranges list
	 * 
	 * @param x - position in the text
	 * @param length - how many characters the image point is long  should be 1 .. otherwise problematic..
	 * @param control   the control that should be added there..
	 * @param ranges
	 * @return
	 */
	public static ObjectPoint<Control> create(int x,String replacementText,float ascentPerc,Control control,List<StyleRange> ranges) {
		ObjectPoint<Control> ip = new ObjectPoint<Control>();
		ip.x = x;
		ip.length = 1;
		ip.replacementText = replacementText;
		ip.obj = control;
		StyleRange style = new StyleRange(x,1,null,null);
		control.pack();
		Rectangle rect = control.getBounds();
		
		logger.debug("Rect for object: heigth"+rect.height+" width:"+rect.width+ "  "+ip.x+ "  "+ip.length);
		int ascent = (int)(rect.height *ascentPerc);  //2 * rect.height / 3;
//	    if (control instanceof Link) {
//	    	ascent = rect.height-3;
//	    }
	    int descent = rect.height - ascent;
	//    style.metrics = new GlyphMetrics(ascent + MARGIN, descent + MARGIN, rect.width + 2 * MARGIN);
		style.metrics = new GlyphMetrics(ascent, descent, rect.width / style.length ); //  / style.length
		ranges.add(style);
		return ip;
	}
	
	
//	public static ObjectPoint<Runnable> createRunnablePoint(int x,int length,Runnable runnable,Color foreground,Color background,Font font,List<StyleRange> ranges) {
//		ObjectPoint<Runnable> ip = new ObjectPoint<Runnable>();
//		ip.x = x;
//		ip.length = length;
//		ip.obj = runnable;
//		StyleRange style = new StyleRange(x,length,foreground,background);
//		style.underline = true;
//		style.font = font;
//		ranges.add(style);
//		return ip;
//	}
	
	
	/**
	 * position in the text
	 */
	public int x;
	
	public int length;
	
	public String replacementText;
	
//	public int ascent;
	
	/**
	 * possibly an image
	 */
	public V obj;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + length;
		result = prime * result + x;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectPoint<?> other = (ObjectPoint<?>) obj;
		if (length != other.length)
			return false;
		if (x != other.x)
			return false;
		return true;
	}
	
	

}
