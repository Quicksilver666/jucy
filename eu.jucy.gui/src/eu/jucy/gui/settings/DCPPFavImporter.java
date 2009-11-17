package eu.jucy.gui.settings;

import java.io.File;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;

import uc.Command;
import uc.FavFolders;
import uc.FavHub;
import uc.FavFolders.FavDir;

/**
 * imports  settings from DC++ like clients..
 * 
 * @author Quicksilver
 *
 */
public class DCPPFavImporter {

	/*
	 * 
<Favorites>
    <Hubs>
  		<Hub Name="[* Deutsche Movie Zone *] (dc.homelinux.net:5555)" Connect="0" Description="[* Deutsche Movie Zone *] (dc.homelinux.net:5555)" Nick="Quicksilver" Password="as9d8ua9e" Server="dc.homelinux.net:5555" UserDescription="echt selten hier" Encoding="" /> 
  		<Hub Name="Deutscher Underground1" Connect="0" Description="<R:100%,S:Online,C:de> ** 1 GB minshare ** Music ** Movies ** Others **" Nick="testasda" Password="" Server="du-hub1.dnsalias.com" UserDescription="" Encoding="" /> 
  	</Hubs>
- 	<Users>
  		<User LastSeen="1180222015" GrantSlot="1" UserDescription="" Nick="°^Rockoco^°" URL="127.0.0.1:6999" CID="5FQHKIMCNF4PNYWLJDMPYU4LZV77JTW3ULPQMKY" /> 
  	</Users>
- 	<UserCommands>
  		<UserCommand Type="1" Context="0" Name="Benachrichtigen" Command="$To: °^Sekretär^° From: %[myNI] $<%[myNI]> !message %[line:user] %[line:message]|" Hub="" /> 
  		<UserCommand Type="2" Context="2" Name="Quit" Command="<%[mynick]> +Quit %[nick]|" Hub="op" /> 
  		<UserCommand Type="2" Context="2" Name="kennylize" Command="$To: °^Goose^° From: %[mynick] $<%[mynick]> #kennylize %[nick]|" Hub="op" /> 
  		<UserCommand Type="2" Context="2" Name="unkennylize" Command="$To: °^Goose^° From: %[mynick] $<%[mynick]> #unkennylize %[nick]|" Hub="op" /> 
  		<UserCommand Type="1" Context="12" Name="TTH Porn" Command="$To: °^Schnüffelt^° From: %[mynick] $<%[mynick]> replace0 %[tth] replace1 %[file] replace2|" Hub="op" /> 
  		<UserCommand Type="1" Context="2" Name="Raw me" Command="<%[mynick]> +rawme $MyINFO $ALL %[nick] %[description] %[tag]$ $DSL1$%[email]$%[share]$|" Hub="op" /> 
  		<UserCommand Type="1" Context="1" Name="Testscript" Command="$To: °^Goose^° From: %[mynick] $<%[mynick]> #settextcommand 01 $To: %[mynick] From: °^Tower^° $<°^Tower^°> %[nick] asdfghjkl test||<%[mynick]> +01|" Hub="op" /> 
  		<UserCommand Type="1" Context="2" Name="Connecttomefake" Command="$ConnectToMe °^Toppy@work^° %[nick] 84.159.17.109:6911|" Hub="op" /> 
  	</UserCommands>
- 	<FavoriteDirs>
  		<Directory Name="Deutsche Filme">D:\Deutsche Filme\</Directory> 
  		<Directory Name="UCDownloads">C:\Users\christian\Desktop\downloads\UCDownloads\</Directory> 
  	</FavoriteDirs>
</Favorites>
	 * 
	 */
	
	
	public static void importFavs(File favouritesxml) throws Exception {
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		saxParser.parse(favouritesxml, new FavouritesHandler());
	}
	
	
	private static class FavouritesHandler extends DefaultHandler {

		
		List<FavDir> dirs = FavFolders.getFavDirs();
		List<Command> commands = UserCommands.loadCommands();
		
		
		private FavDir currentfd = null;
		private String file = "";
		
		@Override
		public void startElement(String uri, String localName, String qname,
				Attributes attributes) throws SAXException {

			
			if ("Hub".equals(qname)) {
				String addy = attributes.getValue("Server");
				FavHub fh = new FavHub(addy);
				if (!ApplicationWorkbenchWindowAdvisor.get().getFavHubs().contains(addy)) {
					fh.setHubname(attributes.getValue("Name"));
					fh.setAutoconnect(!attributes.getValue("Connect").equals("0"));
					fh.setDescription(attributes.getValue("Description"));
					fh.setNick( attributes.getValue("Nick") );
					fh.setPassword( attributes.getValue("Password") );
					fh.setUserDescription( attributes.getValue("UserDescription") );
					fh.addToFavHubs(ApplicationWorkbenchWindowAdvisor.get().getFavHubs());
				}
				
			} else if ("UserCommand".equals(qname)) {
				int type = Integer.valueOf(attributes.getValue("Type"));
				int context = Integer.valueOf(attributes.getValue("Context"));
				String hub = attributes.getValue("Hub");
				Command com;
				if (type == 1 || type == 2) {
					com = new Command(attributes.getValue("Name"),
							type == 2, 
							context, 
							attributes.getValue("Command"),
							hub);
				} else {
					com = new Command(context,"", hub);
				}
				commands.add(com);
				
			} else if ("Directory".equals(qname)) {
				currentfd = new FavDir(attributes.getValue("Name"), null);
			}
			
		}
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (currentfd != null) {
				file += new String(ch,start,length);
			}
		}
		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			if (currentfd != null) {
				currentfd.setDirectory(new File(file));
				dirs.add(currentfd);
				currentfd = null;
			}
		}
		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
			FavFolders.storeFavDirs(dirs);
			UserCommands.storeCommands(commands);
		}
		
		
	
	}
	
	
}
