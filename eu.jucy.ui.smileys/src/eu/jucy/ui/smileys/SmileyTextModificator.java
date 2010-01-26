package eu.jucy.ui.smileys;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.eclipse.swt.custom.StyledText;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import uc.IHub;



import eu.jucy.gui.texteditor.ITextModificator;
import eu.jucy.gui.texteditor.StyledTextViewer;
import eu.jucy.gui.texteditor.StyledTextViewer.ImageReplacement;
import eu.jucy.gui.texteditor.StyledTextViewer.Message;
import eu.jucy.gui.texteditor.StyledTextViewer.TextReplacement;

/**
 * http://www.java2s.com/Tutorial/Java/0280__SWT/StyledTextembedimages.htm
 * 
 * 
 * @author Quicksilver
 *
 */
public class SmileyTextModificator implements ITextModificator {

	


	public static final String PLUGIN_ID = "eu.jucy.ui.smileys"; //$NON-NLS-1$
	
	//private StyledText text;
	
	private static final List<Image>  images = new CopyOnWriteArrayList<Image>();
	private static final Map<String,Integer> smileyToImageNumber = new HashMap<String,Integer>();
	private static final List<Entry<Integer,String>> smileyToCorrespondingText = new ArrayList<Entry<Integer,String>>();
	private static final Pattern allSmileys;
	
	
	static {
		String BUNDLE_NAME = "smileys"; //$NON-NLS-1$

		ResourceBundle RESOURCE_BUNDLE = ResourceBundle
				.getBundle(BUNDLE_NAME);
	
		String fullRegex = null; 
		Enumeration<String> keys = RESOURCE_BUNDLE.getKeys();
		int i = 0;
		for (; keys.hasMoreElements();i++) {
			String key = keys.nextElement();
			String smiley = RESOURCE_BUNDLE.getString(key);
			String[] smileys = smiley.split( Pattern.quote("$$$"));
			
			ImageData imageData = AbstractUIPlugin.imageDescriptorFromPlugin(
					PLUGIN_ID, "smileys/"+key).getImageData();
			imageData.transparentPixel = imageData.getPixel(0, 0);
				//imageData.palette.getPixel();
			Image image = new Image(Display.getCurrent(),imageData);
			images.add(image);
			smileyToCorrespondingText.addAll(Collections.singletonMap(i,smileys[0]).entrySet());
			for (String value:smileys) {
				smileyToImageNumber.put(value, i);
				
				String regex  = "(?:"+Pattern.quote(value)+")";
				if (fullRegex == null) {
					fullRegex=" ("+regex;
				} else {
					fullRegex += "|"+regex;
				}
			}
		}
		fullRegex += ") ?";
		allSmileys = Pattern.compile(fullRegex);
	}
	
	
	
	static List<Entry<Integer, String>> getSmileyToCorrespondingText() {
		return smileyToCorrespondingText;
	}


	static List<Image> getImages() {
		return images;
	}
	
	
	//private int[] offsets;
	
	/**
	 * use point for offset and image in image-array in one...
	 * x is offset 
	 * y is image
	 */
//	private final List<Point> imagePoints = new CopyOnWriteArrayList<Point>();
	
	public void init(StyledText st ,StyledTextViewer viewer, IHub hub) {}
	
	
	public void dispose() {}
	
	
//	public void getStyleRange(String message, int startpos,Message originalMessage,List<StyleRange> range,List<ObjectPoint<Image>> imagepoints) {
//		//Image image, int offset
//		//if matches some smiley -> add style Range.. call imae
//		int offset = 0;
//		Matcher m = allSmileys.matcher(message);
//		while (m.find(offset)) {
//			int position = m.start(1);
//			String smiley= m.group(1);
//	
//			Image img = images.get(smileyToImageNumber.get(smiley));
//			
//			ObjectPoint<Image> p = ObjectPoint.create(position+startpos, smiley.length(),smiley, img, range);
//			imagepoints.add(p);
//			
//			//Point image = new Point(position+startpos, smileyToImageNumber.get(smiley));
//			//list.add(addImage(image,smiley));
//			offset = m.end(1);
//		}
//		
//		//return list;
//	}
//
//
//	public String modifyMessage(String message, Message original, boolean pm) {
//		return message;
//	}


	public void getMessageModifications(Message original, boolean pm,List<TextReplacement> replacement) {
		int offset = 0;
		Matcher m = allSmileys.matcher(original.getMessage());
		while (m.find(offset)) {
			int position = m.start(1);
			String smiley = m.group(1);
	
			Image img = images.get(smileyToImageNumber.get(smiley));
			
			replacement.add(new ImageReplacement(position,smiley,img));
//			
//			ObjectPoint<Image> p = ObjectPoint.create(position+startpos, smiley.length(), img, range);
//			imagepoints.add(p);
			
			//Point image = new Point(position+startpos, smileyToImageNumber.get(smiley));
			//list.add(addImage(image,smiley));
			offset = m.end(1);
		}
		
	}

	


	

}
