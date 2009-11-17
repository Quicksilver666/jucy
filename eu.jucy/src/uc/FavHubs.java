package uc;

import helpers.Observable;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import uc.FavHub.FavHubTranslater;
import uihelpers.ComplexListEditor;


/**
 * holds all favourite hubs
 * @author Quicksilver
 *
 */
public class FavHubs extends Observable<FavHub> implements IFavHubs {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	/*
	private static class Holder {
		private static final FavHubs singleton = new FavHubs(null);
	}
	
	public static FavHubs get() {
		return Holder.singleton;
	}  */

//	private boolean loadedInLegacyMode = false;
	
	@SuppressWarnings("serial")
	private final Map<Integer,FavHub> favHubs = Collections.synchronizedMap(new HashMap<Integer,FavHub>() {
		
		/**
		 * make sure new Order is set to the hub..
		 */
		@Override
		public FavHub put(Integer key, FavHub value) {
			if (!key.equals(value.getOrder())) {
				value.setOrder(key);
			}
			return super.put(key, value);
		}
	});
	
/*	private final Set<IFavHubsChangedListener> listeners = 
		Collections.synchronizedSet(new HashSet<IFavHubsChangedListener>()); */
	
	private final DCClient dcc;
	
	public FavHubs(DCClient dcc) {
		this.dcc = dcc;
		load();
	}
	
	/* (non-Javadoc)
	 * @see uc.IFavHubs#store()
	 */
	public void store() throws IOException {
	//	boolean legacymode = false;
	//	if (!legacymode) {
		logger.debug("Storeing FavHubs modern way");
		synchronized(favHubs) {
			String p = ComplexListEditor.createList(
					new ArrayList<FavHub>(favHubs.values()),
					new FavHubTranslater());
			
			
			/*String[] all = new String[favHubs.size()];
			for (int i = 0; i < favHubs.size() ; i++ ) {
				FavHub hub = favHubs.get(i);
				all[i] = hub.store();
			} */
			if (!PI.put(PI.favHubs2, p)) {
				throw new IOException("Could not store FavHub");
			}
		}
		/*	if (!PI.put(PI.favHubs2, PrefConverter.asString(all))) {
				logger.warn("Favourite hubs could not be stored");
			} else {
				
				if (loadedInLegacyMode) {
					//or may be not..
				}
			}*/
			
		/*} else {
			try {
				Preferences pref = PI.get().node(PI.favHubs);
				pref.removeNode();
				pref =  PI.get().node(PI.favHubs);
				synchronized(favHubs) {
					for (FavHub fh: favHubs.values()) {
						Preferences p = pref.node(""+fh.getOrder());
						fh.store(p);
					}
				}
				pref.flush();
				
			} catch(BackingStoreException bse) {
				logger.warn("Favorite hubs could not be stored",bse);
				throw new missing16api.IOException(bse);
			}
		} */
		notifyObservers(null);
	}
	
	private void load() {
		String fav = PI.get(PI.favHubs2);
		if (fav.equals(PI.LegacyMode)) {
			logger.debug("Loading FavHubs legacy way");
			Preferences pref = PI.get().node(PI.favHubs);
			Map<Integer,FavHub> ret = new HashMap<Integer,FavHub>();
			try {
				for (String child : pref.childrenNames()) {
					Preferences p = pref.node(child);
					FavHub fh = FavHub.loadHub(p);
					ret.put(fh.getOrder(),fh);
				}
			//	loadedInLegacyMode = true;
			} catch(BackingStoreException bse) {
				bse.printStackTrace();
			}
			favHubs.putAll(ret);
		} else {
			logger.debug("Loading FavHubs modern way");
			List<FavHub> fhList = ComplexListEditor.parseString(fav, new FavHubTranslater());
			
			//String[] all = PrefConverter.asArray(fav);
			for (FavHub fh:fhList) {
				favHubs.put(fh.getOrder(), fh);
				//favHubs.put(i, FavHub.loadHub(all[i], i));
			}
		}
		notifyObservers(null);
	}
	
	/* (non-Javadoc)
	 * @see uc.IFavHubs#getFavHubs()
	 */
	public List<FavHub> getFavHubs() {
		return Collections.unmodifiableList(new ArrayList<FavHub>(favHubs.values()));
	}
	
	/**
	 * adds the hub to favorites .. (setting the order if not set)
	 */
	public void addToFavorites(FavHub hub) {
		if (hub.getOrder() == -1) {
			hub.setOrder(favHubs.size());
		}
		favHubs.put(hub.getOrder(), hub);
		notifyObservers(null);
		try {
			store();
		} catch(IOException ioe) {
			logger.warn(ioe,ioe);
		}
		checkOrder();
	}
	
	/**
	 * removes a hub from FavHubs and moves all other hubs 
	 * @param hub
	 */
	public void removeFromFavorites(FavHub hub) {
		favHubs.remove(hub.getOrder());
		repairOrders();
		/*for (int i = hub.getOrder()+1 ; i < favHubs.size() ; i++ ) {
			favHubs.put(i-1,  favHubs.get(i));
		} */
		notifyObservers(null);
		checkOrder();
	}
	
	/**
	 * changes the order of the hub
	 * @param favHub - the hub 
	 * @param up
	 */
	public void changeOrder(FavHub favHub, boolean up) {
		int curOrder = favHub.getOrder();
		int newOrder = curOrder + (up? -1:1);
		if (newOrder >= 0 && newOrder < favHubs.size()) {
			FavHub other = favHubs.get(newOrder);
			favHubs.put(newOrder, favHub);
			favHubs.put(curOrder, other);
			notifyObservers(null);
		}
		checkOrder();
	}
	
/*	private void notifyListeners() {
		synchronized(listeners) {
			for (IFavHubsChangedListener listener: listeners) {
				listener.favHubsChanged();
			}
		}
	} */

	/* (non-Javadoc)
	 * @see uc.IFavHubs#openAutoStartHubs()
	 */
	public void openAutoStartHubs() {
		DCClient.execute(new Runnable() {
			public void run() {
				for (int i=0; i < favHubs.size(); i++) {
					FavHub favHub = favHubs.get(i);
					if (favHub == null) { //try repairing on startup if something goes wrong..
						repairOrders();
						try {
							store();
						} catch(IOException ioe) {
							logger.warn(ioe, ioe);
						}
					} else if (favHub.isAutoconnect()) {
						favHub.connect(dcc);
					}
				}
				checkOrder();
			}
		});
	}
	
	/*
	 * adds a listener that will be notified when 
	 * a hub is inserted, removed or the order of a hub changes..
	 * 
	 *
	public void addListener(IFavHubsChangedListener listener) {
		listeners.add(listener);
		checkOrder();
	} */
	
	/*
	public void removeListener(IFavHubsChangedListener listener) {
		listeners.remove(listener);
		checkOrder();
	}*/
	
	/* (non-Javadoc)
	 * @see uc.IFavHubs#contains(java.lang.String)
	 */
	public boolean contains(String hubaddress) {
		for (FavHub fh: favHubs.values()) {
			if (fh.getHubaddy().equals(hubaddress)) {
				return true;
			}
		}
		checkOrder();
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see uc.IFavHubs#contains(uc.FavHub)
	 */
	public boolean contains(FavHub hub) {
		return hub.equals(favHubs.get(hub.getOrder()));	
	}
	
	/*
	 * listener for the GUI to react to changes.
	 * @author Quicksilver
	 *
	 *
	public static interface IFavHubsChangedListener {
		
		/*
		 * just signals a change though not 
		 * what is changed..
		 *
		void favHubsChanged();
	} */
	
	/**
	 * if one Order is missing in between
	 * this shifts all elements a bit
	 */
	private void repairOrders() {
		int j= 0;
		for (int i = 0; i < favHubs.size() ; ) {
			FavHub hub = favHubs.get(i+j);
			if (hub == null) {
				j++;
			} else {
				if (j != 0) {
					favHubs.remove(i+j);
					favHubs.put(i, hub);
				}
				i++;
			}
		}
		checkOrder();
	}
	
	private void checkOrder() {
		for (int i = 0; i < favHubs.size() ; i++ ) {
			FavHub hub = favHubs.get(i);
			if (hub == null && Platform.inDevelopmentMode()) {
				logger.warn("hubs not in order",new Throwable());
			} 
		}
	}
	
}
