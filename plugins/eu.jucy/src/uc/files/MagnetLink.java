package uc.files;





import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




import uc.DCClient;
import uc.IUser;
import uc.crypto.HashValue;
import uc.crypto.TigerHashValue;
import uc.files.AbstractDownloadable.AbstractDownloadableFile;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.search.FileSearch;
import uc.protocols.IProtocolCommand;


/**
 * represents a magnet Link
 * can be used to download things just with a link received in chat
 * 
 * @author Quicksilver
 *
 */
public class MagnetLink extends AbstractDownloadableFile implements IDownloadableFile {


	private static final Map<String,String> pattern = new HashMap<String,String>();
	
	public static String KEYWORD_TOPIC = "kt";
	
	private static final String TEXT = "(?:[^\\x20\\x0C\\x0A&]*)";
	
	static {
		pattern.put("xt", "urn:tree:tiger:("+TigerHashValue.TTHREGEX+")" );
		pattern.put("xl", "("+IProtocolCommand.FILESIZE +")");
	}
	
//	private static final String // XT = "(?:xt\\=urn:tree:tiger:("+TigerHashValue.TTHREGEX+"))"
//							//	,XL = "(?:xl\\=("+IProtocolCommand.FILESIZE +"))"
//							//	,DN =  "(?:dn\\=("+IProtocolCommand.TEXT_NONEWLINE_NOSPACE+"))"
//							//	,KT = "(?:kt\\=("+IProtocolCommand.TEXT_NONEWLINE_NOSPACE+"))"
//								,OTHER = "(?:(..)(?:\\.\\d+)?\\=("+IProtocolCommand.TEXT_NONEWLINE_NOSPACE+"))";
	
	
	private static final String PARM = "(?:(\\w{2})(?:\\.\\d+)?\\=("+TEXT+"))"; //"(?:"+XT+"|"+XL+"|"+OTHER+")";
	
	
	public static final String MagnetURI = "magnet:\\?"+PARM+"(?:&"+PARM+")*";
	
//			"" +
//			"(?:xt\\=urn:tree:tiger:("+TigerHashValue.TTHREGEX+"))?" +
//	"&?(?:xl\\=("+IProtocolCommand.FILESIZE +"))?"
//	+"&?(?:dn\\=("+IProtocolCommand.TEXT_NONEWLINE_NOSPACE+"))?";
	
	

	public static final Pattern MAGNET_PAT = Pattern.compile(MagnetURI);
	
	/**
	 * 
	 * @param s
	 * @return a magnet link from the given string.. null if not a valid magnet link
	 */
	public static MagnetLink parse(String magnetURI) {

		Matcher magnet = MAGNET_PAT.matcher(magnetURI);
		if (magnet.matches()) {
			Map<String,String> allInfo = new HashMap<String,String>();
			for (int i = 1; i <= magnet.groupCount(); i+=2) {
				String key = magnet.group(i);
				String value = magnet.group(i+1);
				String mod = pattern.get(key);
				if (value != null && key != null) {
					if (mod != null) {
						Matcher m = Pattern.compile(mod).matcher(value);
						if (m.matches()) {
							value = m.group(1);
						} else {
							value = null;
						}
					} else {
						try {
							value = URLDecoder.decode(value, "utf-8");
						} catch (UnsupportedEncodingException e) {
							throw new IllegalStateException(e);
						} catch (NullPointerException npe) {
							throw new IllegalStateException("Causedby: "+magnetURI+" "+i,npe);
						}
					}
				}
				if (value != null) {
					allInfo.put(key, value);
				}
			}
			
			HashValue hash = null;
			if (allInfo.containsKey("xt")) {
				hash = HashValue.createHash(allInfo.get("xt"));
			}
			long size = -1;
			if (allInfo.containsKey("xl")) {
				size = Long.parseLong(allInfo.get("xl"));
			}
			String name = allInfo.get("dn");
	
			//boolean complete = hash != null && size != -1 && name != null;

		
			return new MagnetLink(magnetURI,hash,size,name,allInfo);
		}

		return null;
	}
	private final String fullString;
	
	private final Map<String,String> other = new HashMap<String,String>();
	private final HashValue hash;
	private final long filesize;
	private final String filename;
	
	
	
	


	public MagnetLink(IDownloadableFile idf) {
		this(null,idf.getTTHRoot(),idf.getSize(),idf.getName(),Collections.<String,String>emptyMap());
	}
	
	public MagnetLink(String fullString,HashValue hash, long filesize, String filename,Map<String,String> allInfo) {
		super();
		this.fullString = fullString;
		this.hash = hash;
		this.filesize = filesize;
		this.filename = filename;
		other.putAll(allInfo);
	}
	
	
	public String toString() {
		if (fullString != null) {
			return fullString;
		}
		try {
			return String.format("magnet:?xt=urn:tree:%s:%s&xl=%d&dn=%s", 
					hash.magnetString(),hash,filesize,URLEncoder.encode(filename, "utf-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		
	}
	
	@Override
	public AbstractDownloadQueueEntry download(File target)  {
		AbstractDownloadQueueEntry adqe = super.download(target);
		DCClient.get().search( new FileSearch(hash)); //always search for alternatives..
		return adqe;
	}
	
	public boolean isComplete() {
		return hash != null && filesize != -1 && filename != null;
	}
	
	
	

	public long getSize() {
		return filesize;
	}
	

	@Override
	public String getPath() {
		return filename;
	}

	@Override
	public HashValue getTTHRoot() {
		return hash;
	}

	@Override
	public IUser getUser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<IUser> getIterable() {
		return Collections.emptyList();
	}

	public String getName() {
		if (filename == null) {
			return null;
		} else {
			return super.getName();
		}
	}
	
	public String get(String parm) {
		return other.get(parm);
	}
	
	@Override
	public int nrOfUsers() {
		return 0;
	}

}
