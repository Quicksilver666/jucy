package eu.jucy.ui.smileys;



import helpers.GH;
import helpers.PreferenceChangedAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.custom.StyledText;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import org.osgi.framework.Bundle;

import uc.IHub;



import eu.jucy.gui.texteditor.ITextModificator;
import eu.jucy.gui.texteditor.StyledTextViewer;
import eu.jucy.gui.texteditor.StyledTextViewer.ImageReplacement;
import eu.jucy.gui.texteditor.StyledTextViewer.Message;
import eu.jucy.gui.texteditor.StyledTextViewer.TextReplacement;

/**
 * 
 * 
 * @author Quicksilver
 *
 */
public class SmileyTextModificator implements ITextModificator {

	private static final Logger logger = LoggerFactory.make();


	private static final String BUNDLE_NAME= "smileys.properties";
	
	
	private static final List<Image[]>  images = new CopyOnWriteArrayList<Image[]>();
	private static final Map<String,Integer> smileyToImageNumber = new HashMap<String,Integer>();
	private static final List<Entry<Integer,String>> smileyToCorrespondingText = new ArrayList<Entry<Integer,String>>();
	private static Pattern allSmileys;
	
	private static Properties loadProperties() {
		Properties p = new Properties();
		InputStream is = null;
		try {
			is = getInputStream(BUNDLE_NAME,SmileysPI.get(SmileysPI.SMILEYS_PATH));
			p.load(is);
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} finally {
			GH.close(is);
		}
		return p;
	}
	
	private static InputStream getInputStream(String relpath,String zipPath) {
		try {
			if (GH.isEmpty(zipPath) || !new File(zipPath).isFile()) {
				Bundle bundle = Platform.getBundle(SmileysPI.PLUGIN_ID);
				Path path = new Path(relpath);
				URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
				return url.openStream();
			} else {
				ZipFile zip = new ZipFile(zipPath);
				return zip.getInputStream(zip.getEntry(relpath));
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static boolean isValidZipFile(File f) {
		try {
			if (!f.isFile()) {
				return false;
			}
			ZipFile zip = new ZipFile(f);
			return zip.getEntry(BUNDLE_NAME) != null;
		} catch(IOException ioe) {
			return false;
		}
	}
	
	public static void loadAll() {
		images.clear();//ugly resource leak ...though unproblematic if not changed to often..
		smileyToImageNumber.clear();
		smileyToCorrespondingText.clear();
		
		Properties prop = loadProperties();
		String fullRegex = null; 

		int i = -1;
		for (Entry<Object,Object> e: prop.entrySet()) {
			i++;
			
			String key = e.getKey().toString();;
			String smiley = e.getValue().toString(); // prop.getProperty(key);
			String[] smileys = smiley.split( Pattern.quote("$$$"));
			
			ImageLoader il = new ImageLoader();
			ImageData[] imageData = il.load(getInputStream("smileys/"+key,SmileysPI.get(SmileysPI.SMILEYS_PATH)));
			Image[] imArray = new Image[imageData.length];
			for (int x = 0; x < imageData.length; x++) {
				imageData[x].transparentPixel = imageData[x].getPixel(0, 0);
				imArray[x] = new Image(Display.getCurrent(),imageData[x]);
			}
			images.add(imArray);
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
	
	static {
		loadAll();
		new PreferenceChangedAdapter(SmileysPI.PLUGIN_ID,SmileysPI.SMILEYS_PATH) {
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				loadAll();
				logger.info("reloading smileys");
			}
		};
	}
	
	
	
	
	static List<Entry<Integer, String>> getSmileyToCorrespondingText() {
		return smileyToCorrespondingText;
	}


	static List<Image[]> getImages() {
		return images;
	}
	
	public void init(StyledText st ,StyledTextViewer viewer, IHub hub) {}
	
	public void dispose() {}
	

	public void getMessageModifications(Message original, boolean pm,List<TextReplacement> replacement) {
		int offset = 0;
		Matcher m = allSmileys.matcher(original.getMessage());
		while (m.find(offset)) {
			int position = m.start(1);
			String smiley = m.group(1);
	
			Image img = images.get(smileyToImageNumber.get(smiley))[0];
			
			replacement.add(new ImageReplacement(position,smiley,img)); //TODO ImageReplacement for multiple images
			offset = m.end(1);
		}
	}

	


	

}
