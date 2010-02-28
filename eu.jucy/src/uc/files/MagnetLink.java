package uc.files;


import java.io.File;
import java.util.Collection;
import java.util.Collections;
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


	
	public static final String MagnetURI = "magnet:\\?xt\\=urn:tree:tiger:("+TigerHashValue.TTHREGEX+")" +
	"&xl\\=("+IProtocolCommand.FILESIZE +")"
	+"&dn\\=("+IProtocolCommand.TEXT_NONEWLINE_NOSPACE+")";

	public static final Pattern MagnetPat = Pattern.compile(MagnetURI);
	/**
	 * 
	 * @param s
	 * @return a magnet link from the given string.. null if not a valid magnet link
	 */
	public static MagnetLink parse(String magnetURL) {
		Matcher magnet = MagnetPat.matcher(magnetURL);
		if (magnet.matches()) {
			HashValue hash = HashValue.createHash(magnet.group(1));
			long size = Long.parseLong(magnet.group(2));
			String name = magnet.group(3).replace('+', ' ').trim();
			
			return new MagnetLink(hash,size,name);
		}
		return null;
	}
	
	private final HashValue hash;
	private final long filesize;
	private final String filename;
	
	
	
	
	public MagnetLink(IDownloadableFile idf) {
		this(idf.getTTHRoot(),idf.getSize(),idf.getName());
	}
	
	public MagnetLink(HashValue hash, long filesize, String filename) {
		super();
		this.hash = hash;
		this.filesize = filesize;
		this.filename = filename;
	}
	
	
	public String toString() {
		return String.format("magnet:?xt=urn:tree:%s:%s&xl=%d&dn=%s", 
				hash.magnetString(),hash,filesize,filename.replace(' ', '+'));
	}
	
	@Override
	public AbstractDownloadQueueEntry download(File target) {
		AbstractDownloadQueueEntry adqe = super.download(target);
		DCClient.get().search( new FileSearch(hash)); //always search for alternatives..
		return adqe;
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

	@Override
	public int nrOfUsers() {
		return 0;
	}

}
