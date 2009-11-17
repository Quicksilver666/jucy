package geoip;

import helpers.GH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

public final class GEOIP {

	private static final String PLUGIN_ID = "eu.jucy.geoipmaxmind";
	
	private static final boolean country =  GEOPref.getBoolean(GEOPref.countryOnly);
	private static final String GZPath = country ? "db/GeoIP.dat.gz" : "db/GeoLiteCity.dat.gz" ; //
	private static final String FilePath = country ? "db/GeoIP.dat":"db/GeoLiteCity.dat";// ;//
	private static final String LastVersion = "LastVersion";
	

	public static final Logger logger = LoggerFactory.make();
	
	private final LookupService ls ;
	
	private static class GEOIPHolder {
		private static final GEOIP singleton = new GEOIP();
	}
	
	public static GEOIP get() {
		return GEOIPHolder.singleton;
	}
	
	private GEOIP() {
		LookupService ls = null;
		try {
			if (!getDBFile().isFile() || isVersionChanged()) {
				copyToWorkspace();
			}
			ls = new LookupService(getDBFile(),
					LookupService.GEOIP_MEMORY_CACHE );
		} catch(IOException ioe) {
			logger.warn(ioe, ioe);
		}
		this.ls = ls;
	}
	
	
	private boolean isVersionChanged() {
		String lastModified = new InstanceScope().getNode(PLUGIN_ID).get(LastVersion, "");
		String currentVersion = Platform.getBundle(PLUGIN_ID).getVersion().toString();
		return !lastModified.equals(currentVersion);
	}
	
	/**
	 * 
	 * @return true if only country information is available
	 * meaning:
	 * true: getLocation() /may not be called
	 * false .. all may be called..
	 */
	public boolean isCountryOnly() {
		return country;
	}
	
	/**
	 * @param ip - an IP that should be resolved
	 * @return the location to the IP
	 */
	public Location getLocation(String ip) {
		return ls.getLocation(ip);
	}
	
	/**
	 * @param ip - an IP that should be resolved
	 * @return the location to the IP
	 */
	public Location getLocation(InetAddress ip) {
		return ls.getLocation(ip);
	}
	
	
	public Country getCountry(InetAddress ip) {
		if (country) {
			return ls.getCountry(ip);
		} else {
			Location loc = getLocation(ip);
			if (loc != null) {
				return new Country(loc.countryCode,loc.countryName);
			}
			return null;
		}
	}
	
	public String getCountryCode(InetAddress ip) {
		if (country) {
			Country c = getCountry(ip);
			return c == null ? null : c.getCode();
		} else {
			Location loc = getLocation(ip);
			return loc == null? null: loc.countryCode;
		}
	}
	

	
	
	private File getDBFile() {
		return new File(new File(Platform.getInstanceLocation().getURL().getFile()),FilePath);
	}
	
	private void copyToWorkspace() {
		logger.info("Unpacking GEOIP DB to workspace!");
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		Path path = new Path(GZPath); 
		URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
		ReadableByteChannel rbc = null;
		FileChannel fc = null;
		try {
			InputStream is = url.openStream();
			GZIPInputStream gis = new GZIPInputStream(is);
			rbc = Channels.newChannel(gis);
			fc = new FileOutputStream(getDBFile()).getChannel();
			fc.truncate(0);//delete if already exists..
			ByteBuffer buf = ByteBuffer.allocate(1024*4);
		
			while (-1 != rbc.read(buf) || buf.position() > 0) {
				buf.flip();
				fc.write(buf);
				buf.compact();
			}
			IEclipsePreferences prefs = new InstanceScope().getNode(PLUGIN_ID);
			prefs.put(LastVersion, Platform.getBundle(PLUGIN_ID).getVersion().toString());
			try {
				prefs.flush();
			} catch(BackingStoreException bse) {
				logger.warn(bse, bse);
			}
			
		} catch(IOException ioe) {
			logger.warn(ioe, ioe);
		} finally {
			GH.close(fc,rbc);
		}
		
	}
	
	
}
