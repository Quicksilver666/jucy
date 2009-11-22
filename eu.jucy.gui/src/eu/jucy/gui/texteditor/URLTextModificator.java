package eu.jucy.gui.texteditor;

import helpers.SizeEnum;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;



import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;


import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import eu.jucy.gui.Application;
import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.Lang;
import eu.jucy.gui.favhub.FavHubEditor;
import eu.jucy.gui.texteditor.StyledTextViewer.Message;



import uc.FavHub;
import uc.IHub;
import uc.PI;
import uc.files.MagnetLink;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.AbstractDownloadQueueEntry.IDownloadFinished;
import uihelpers.SUIJob;


public class URLTextModificator implements ITextModificator {

	private static final Logger logger =  LoggerFactory.make(Level.INFO);

	private static final char URL_CHAR = '\uFFFC'; // (char)18;
	
	private static Image IMAGE_URL_ICON;
	
	private static Image getImageURLIcon() {
		if (IMAGE_URL_ICON == null) {
			IMAGE_URL_ICON = AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.VIEWIMAGEICON).createImage();
		}
		return IMAGE_URL_ICON;
	}
	
	
	public static final String ID = "eu.jucy.gui.URLTextModificator";
	
	private static final String URLENDING = "[\\w\\p{L}\\-_]+(\\.[\\w\\p{L}\\-_]+)+([\\w\\p{L}\\-\\.,@?^=%&amp;:/~\\+#\\(\\)]*[\\w\\p{L}\\-\\@?^=%&amp;/~\\+#])?";
	private static final String URL = "(http|ftp|https):\\/\\/"+URLENDING;


	
	private final AbstractLinkType[] LINK_TYPES = new AbstractLinkType[]{new HTTPLink(),new MagLink(),new HubLink()}; 
	
	private static final Pattern ANY_URL = Pattern.compile(
			"((?:"+URL+")|(?:"+MagnetLink.MagnetURI+")|(?:"+HubLink.HL_PAT+"))"); 
	
	private static final String[] IMAGE_ENDINGS =  new String[] {".png",".jpg",".bmp", ".gif"} ;
	

	
	private StyledText text;
	private StyledTextViewer viewer;
//	private MouseAdapter listener;
	
	

	public void init(StyledText st,StyledTextViewer viewer, IHub hub) {
		if (st.isDisposed()) {
			throw new IllegalStateException("can't init on disposed Text: "+hub.getName()+"  "+hub.getFavHub().getHubaddy());
		}
		this.viewer = viewer;
		this.text = st;
		text.setBackgroundMode(SWT.INHERIT_FORCE);
	
	}
	
	
	
	public void dispose() {
//		if (!text.isDisposed()) {
//			text.removeMouseListener(listener);
//		}
	}
	

	public String modifyMessage(String message, Message original, boolean pm) {
		if (message.indexOf(URL_CHAR) != -1 ) {
			message = message.replace(URL_CHAR, ' '); //replace invalid chars..
		}
		
		Matcher m = ANY_URL.matcher(message);
		int minimumSearchpos = 0;
		while (minimumSearchpos < message.length() && m.find(minimumSearchpos)) {
			String uri = m.group();
			AbstractLinkType alt = getMatching(uri);
			
			int start = m.start();
			logger.debug("found image URI: "+uri+" "+uri.length() );
			message = message.substring(0, start)+URL_CHAR
					+(alt.hasImageAfterURI(uri)?" ":"")+message.substring(m.end());
			m = ANY_URL.matcher(message);
		
			minimumSearchpos = start+2;
		}
	
		return message;
	}
	
	private static final SelectionAdapter adapter = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			String uri = (String)e.widget.getData();
			GuiHelpers.copyTextToClipboard(uri);
		}
	};
	
	public void getStyleRange(String message, int start,
			Message originalMessage, List<StyleRange> ranges, List<ObjectPoint<Image>> images) {
		
		Matcher m = ANY_URL.matcher(originalMessage.getMessage());
		int messagePos = 0;
		int charPos = 0;
		while (m.find(messagePos)) {
			String foundURI = m.group();
			AbstractLinkType alt = getMatching(foundURI);
			String linkText = alt.getTextReplacement(foundURI);
			
			int posOfURLChar = message.indexOf(URL_CHAR,charPos);
			int posURI = posOfURLChar+start; // to full text..
			Link link = new Link(text, SWT.NONE);
			link.setBackground( GUIPI.getColor(GUIPI.urlModCol));
			link.setFont(GUIPI.getFont(GUIPI.urlModFont));
			link.setData(foundURI);
			link.setToolTipText(foundURI);
			link.setText("<a>"+linkText+"</a>");
		
			viewer.addControl(link,posURI, 0.8f); 
			Menu menu = new Menu(link);
			MenuItem mi = new MenuItem(menu,SWT.PUSH);
			mi.setData(foundURI);
			mi.setText("Copy URI to Clipboard");
			mi.addSelectionListener(adapter);
			link.setMenu(menu);
			
			link.addListener (SWT.Selection, new Listener () {
				public void handleEvent(Event event) {
					logger.debug("Selection: " + event.text+ " "+event.widget.getData());
					String uri = (String)event.widget.getData();
					getMatching(uri).execute(uri);
				}
			});
			
			if (alt.hasImageAfterURI(foundURI)) {
				logger.debug("added image: "+foundURI);
				addLabelImage(posURI+1,foundURI);
			}
			
			messagePos = m.end();
			charPos = posOfURLChar+1;
		}
	}
	
	
	void addLabelImage(int pos,String uri) {
		AbstractLinkType alt = getMatching(uri);
		Label lab = new Label(text,SWT.NONE);
		Image img = alt.getImageAfterURI(uri);
		lab.setImage(img);
		lab.setData(uri);
		final ObjectPoint<Control> op =  viewer.addControl(lab, pos, 2f/3f);
		lab.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				String uri = (String)e.widget.getData();
				getMatching(uri).executeImageClick(uri,op,URLTextModificator.this);
			}
		});
	}
	
	void addLabelReplacementImage(int pos,String uri,final Image img) {
		Label lab = new Label(text,SWT.NONE);
		lab.setImage(img);
		lab.setData(uri);
		final ObjectPoint<Control> op = viewer.addControl(lab, pos, 2f/3f);
		lab.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				String uri = (String)e.widget.getData();
				addLabelImage(op.x,uri);
			}
		});
		lab.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				img.dispose();
			}
		});
	}
	

	
	public AbstractLinkType getMatching(String uri) {
		for (AbstractLinkType alt: LINK_TYPES) {
			Matcher m	= alt.getLinkPat().matcher(uri);
			if (m.matches()) {
				return alt;
			}
		}
		throw new IllegalStateException();
	}
	
	public static abstract class AbstractLinkType {
		
		
		private final Pattern linkPat;

		private AbstractLinkType(String linkPat) {
			super();
			this.linkPat = Pattern.compile(linkPat);
		}
		
		public abstract void execute(String matched);
	
		public String getTextReplacement(String matched) {
			return matched;
		}
		
		/**
		 * 
		 * @param uri - provided URI
		 * @return an image if there should be an image put after the URI ...
		 */
		public Image getImageAfterURI(String uri) {
			return null;
		}
		
		public boolean hasImageAfterURI(String uri) {
			return getImageAfterURI(uri) != null;
		}
		
		/**
		 * executed if the image after the URI was clicked instead of the URI itself
		 * @param uri - the URI before the image..
		 */
		public void executeImageClick(String uri,ObjectPoint<Control> point,URLTextModificator mod) {}
		
		public Pattern getLinkPat() {
			return linkPat;
		}	
	}
	
	private static class HTTPLink extends AbstractLinkType {
		private HTTPLink() {
			super(URL);
		}

		@Override
		public void execute(String matched) {
			try {
				URL url = new URL(matched);
				
				IWorkbenchBrowserSupport browserSupport =
				PlatformUI.getWorkbench().getBrowserSupport();
				
				IWebBrowser  browser = browserSupport.createBrowser("myid");
				browser.openURL(url);
				
			} catch (IOException ioe) {
				logger.warn(ioe,ioe);
			} catch (PartInitException io2) {
				logger.warn(io2,io2);
			}
		}
		
//		@Override
//		public void getStyleRanges(List<StyleRange> ranges, int start, int length,String matched, List<ObjectPoint<Image>> images) {
//			ranges.add(getURLRange(start, length, GUIPI.getColor(GUIPI.urlModCol)) );
//		}

		@Override
		public void executeImageClick(String uri,ObjectPoint<Control> point,URLTextModificator mod) {
			logger.debug("Image url icon pressed: "+uri);
			try {
				new GraphicalFileDownloader(new URL(uri), point,mod).start();
			} catch (MalformedURLException e) {
				logger.warn(e,e);
			}
		}

		public boolean hasImageAfterURI(String uri) {
			for (String ending: IMAGE_ENDINGS) {
				if (uri.endsWith(ending)) {
					return true;
				}
			}
			return false;
		}
		@Override
		public Image getImageAfterURI(String uri) {
			for (String ending: IMAGE_ENDINGS) {
				if (uri.endsWith(ending)) {
					return getImageURLIcon();
				}
			}
			return null;
		}
	}
	
	private static class MagLink extends AbstractLinkType {
		private MagLink() {
			super(MagnetLink.MagnetURI);
		}

		@Override
		public void execute(String matched) {
			MagnetLink magnetLink; 
			if ((magnetLink = MagnetLink.parse(matched)) != null) {	
				magnetLink.download();
				logger.info(String.format(Lang.AddedFileViaMagnet,magnetLink.getName()));
			}
		}

		

		@Override
		public void executeImageClick(final String uri,
				final ObjectPoint<Control> point,final URLTextModificator mod) {
			MagnetLink magnetLink; 
			
			if ((magnetLink = MagnetLink.parse(uri)) != null) {	
				point.obj.setEnabled(false);
				File target = new File(PI.getStoragePath(),magnetLink.getName());
				if (target.isFile()) {
					 openFile(target,uri,point,mod);
				} else {
					AbstractDownloadQueueEntry adqe = magnetLink.download();
					adqe.addDoAfterDownload(new IDownloadFinished() {
						public void finishedDownload(final File f) {
							new SUIJob() {
								@Override
								public void run() {
									openFile(f,uri,point,mod);
									f.deleteOnExit();
								}
							}.schedule();
						}
					});
				}
				logger.info(String.format(Lang.AddedFileViaMagnet,magnetLink.getName()));
			}
		}
		
		private void openFile(File f,String uri,ObjectPoint<Control> point,URLTextModificator mod) {
			try {
				ImageData imgda = ImageDescriptor.createFromURL(f.toURL()).getImageData();
				Image img  = GraphicalFileDownloader.scaleIfNeeded(imgda);
				mod.addLabelReplacementImage(point.x, uri, img);
			} catch (Exception e) {
				logger.info("Download failed: "+e,e);
			}
		}

		@Override
		public Image getImageAfterURI(String uri) {
			MagnetLink magnetLink; 
			if ((magnetLink = MagnetLink.parse(uri)) != null) {	
				String end = "."+magnetLink.getEnding();
				logger.debug("found ending: "+end);
				for (String ending: IMAGE_ENDINGS) {
					if (end.equalsIgnoreCase(ending)) {
						return getImageURLIcon();
					}
				}
			}
			return null;
		}

		

		@Override
		public String getTextReplacement(String matched) {
			MagnetLink ml = MagnetLink.parse(matched);
			return String.format("%s (%s)",ml.getName(),SizeEnum.getReadableSize(ml.getSize()));
		}
		
		
	}
	
	private static class HubLink extends AbstractLinkType {
		private static final String HL_PAT = "((?:dchub)|(?:nmdc)|(?:adc))s?:\\/\\/"+URLENDING;
		
		private static final Image FAVHUB_ICON = AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.FAVHUBS).createImage();
		
		private HubLink() {
			super(HL_PAT);
		}
		
		@Override
		public void execute(String matched) {
			new FavHub(matched).connect(ApplicationWorkbenchWindowAdvisor.get());
		}

		@Override
		public void executeImageClick(String uri, ObjectPoint<Control> point,
				URLTextModificator mod) {
			FavHub fh = new FavHub(uri);
			fh.addToFavHubs(ApplicationWorkbenchWindowAdvisor.get().getFavHubs());
			GuiHelpers.executeCommand(FavHubEditor.OPEN_FAVHUBS_COMMAND_ID);
		}

		@Override
		public Image getImageAfterURI(String uri) {
			return FAVHUB_ICON;
		}

		
	}
	
	
 

}
