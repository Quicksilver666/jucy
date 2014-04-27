/*
 * DCClient.java
 *
 * Created on 11. November 2005, 22:00
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package uc;



import helpers.Observable;
import helpers.Version;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;



import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;




import logger.LoggerFactory;


import org.apache.log4j.Logger;





import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;



import uc.IStoppable.IStartable;
import uc.Identity.DefaultIdentity;
import uc.InfoChange.IInfoChanged;

import uc.crypto.HashValue;
import uc.crypto.IHashEngine;



import uc.database.HashedFile;
import uc.database.IDatabase;
import uc.files.IUploadQueue;
import uc.files.UploadQueue;
import uc.files.downloadqueue.DownloadQueue;

import uc.files.filelist.FileList;
import uc.files.filelist.IFilelistProcessor;
import uc.files.filelist.IOwnFileList;
import uc.files.filelist.OwnFileList;
import uc.files.search.AutomaticSearchForAlternatives;
import uc.files.search.FileSearch;
import uc.files.search.ISearchResult;
import uc.files.search.SearchResult.ISearchResultListener;
import uc.files.transfer.SlotManager;


import uc.protocols.ConnectionState;

import uc.protocols.hub.Hub;
import uc.user.Population;
import uc.user.StoredPM;
import uc.user.User;



/** 
 * TODO when there is a TTHL downloading while the DQE for the tthl is deleted -> DQE without user will be left in DQ
 * TODO do not readd removed users to a file by an autosearch!
 * 
 * TODO when there is a download during upload idle statemachine shown in transfers view
 * 
 * TODO FavHubs  listener change on hub connection.. / not just listener needs more
 * 

 * TODO translation plugin needs to be changed because google translation api got deprecated!!!http://basharkokash.com/post/2010/04/19/Bing-Translator-for-developers.aspx
 * http://blog.gts-translation.com/2011/05/30/why-larry-page-killed-google-translate-api-and-other-assorted-thoughts/
 * TODO PFSR
 * TODO better upload bookkeeping (Size to full file .. Status  -> store past uploads of file) ..
 * TODO cooler looking transfers view with 3D instead of table.. -> OpenGL
 * 
 * TODO extensions  SEGA + Grouping of file extensions in SCH 
 * PFSR - Partial file sharing http://www.adcportal.com/wiki/PFSR_-_Partial_file_sharing
 * http://www.adcportal.com/forums/viewtopic.php?f=55&t=696 -> change SF flag on first downloaded blog to allow hub send new BLOM request
 * 
 * 
 BLOW - Encrypting chat messages with blowfish

This is protection in private hubs to encrypt messages from users until a correct password term is given to decrypt so that conversations remains private even if the hub is penetrated by unwanted users
The idea is that a client connects to the private hub supplies a pass for the chat thats encrypted unless the pass word requierment is meet the chat remains encrypted forcing the unwanted user not to see any messages posted in the chat.
 * 
 * https://adc.svn.sourceforge.net/svnroot/adc/trunk/ADC-EXT.txt
 * 
 *  adc://test.flexhub.org:8000
 *  test zlib
 *  
 *  http://adc.sourceforge.net/versions/ADC-EXT-1.0.6.html#_version_1_0_6  todo SEGA extension

 * TODO translater could be used for translating messages for sending too..

 * 
 * JXTA possibly ... as cooperation tool for ops..? or better via pm..
 * JXTA for cooperation of ops between different hubs viable.. not for normal work
 * 
 * 
 * 
 * TODO just warning the way it is without explanation for bad KP is not good...
 * TODO fix fontsize issue on MACOsX!
 * 
 * 
 * TODO think about option to place unfinished downloads next into target folder so no moving of files occur
 * after finishing the download..
 * 
 * TODO (discfree),total uptime .. total download..
 * TODO stats counter online time, total up+downloaded, average speed up/down
 * 
 * 
 * 
 * TODO triggers for external programs / or try grow plugin
 * 
 * 
 * TODO max upload limit per slot/User 
 *  - additional semaphore / value based on usercategory settings?
 *  - uploadlimit max = Min(limiting,slots*normlimit);
 *  
 * 
 * TODO Option in settings to auto open desired tabs after application start (like download queue, upload queue, finished downloads, finished uploads etc.)
 * 
 * TODO animated smileys
 *  
 * TODO CT64 = hidden -> for hiding users
 *  
 *  
 *  TODO upload/download logs to db .. not keeping in ram..
 * 
 * TODO identity management (adding identities i.e. a identity includes different FileList / CID / TCP/SSL-Ports / Certificate(KeyPrint) )
 *			
 * TODO discovery UI for installing extensions  https://bugs.eclipse.org/bugs/show_bug.cgi?id=295273
 * http://tasktop.com/blog/eclipse/mylyn-connector-discovery -> wait for 3.7
 *
 * TODO Presentation of magnet -> save as button could be added 
 *
 * TODO comments on favUsers.. 
 * 
 * 
 * TODO CO-OP plugin  cooperative OP work .. tagging user so other ops can read it and setting timecounters 
 * for them having something done.. i.e. distributed remember me function
 * 
 * TODO http://jid3.blinkenlights.org/ ID3 tag reading for search indexing... possibly there also was some lib for lucene that covered more filetypes??
 * 
 * TODO no gui option ..possibly simple commandline steering..
 * 
 * TODO implement transfer of uncompressed filelists..
 *  
 *  
 * 
 * 
 * TODO potentially Empty interleaves provided bug might show itself when file is already present on disc?
 * 

 TODO theoretical optimization of segment size by using Golden ration  instead of cutting in half!
 
 *
 * TODO ... new special user category for tons of settings..
 * may be create not one special user category but rather many categories... make FavUsers one such category..
 * i.e. special treatment for PMs ... and and and..
 * 
 * 
 * TODO transferred size boxes in statusline could on click change between showing total
 * vs showing only current session upload.. -> store uploaded/downloaded at end of session
 * or current upload vs average upload.. 
 * -> or may be rather add possibility for a total transferred box..
 * 
 *   
 * 
 * hideshare functionality.. -> TODO better feature would be different filelists
 * on a per hub basis.. hideshare = no filelist..
 * 
 * 
 * TODO (un)checkable actions in popup menu for do after download in DownloadQueue (open filelist,open file)
 * - possibly as extension point?
 * 
 * 
 * 
 * TODO DQ, finished downloads, uploaded , uploadQueue , Filelist -> move to file eu.jucy.gui.file plugin
 * search possibly in its own plugin ... or also in file plugin..
 * 
 * 
 * TODO show only text instead of flags... country plugin..
 * 
 * TODO upload folder    folder where people can upload files (maximum size) 
 * and possibly also delete files  (i.e. client behaves like a och  legality?)
 * 
 * TODO  may be some statistics window?? drawing what is currently transferred...
 *
 * 
 * TODO test maximise on jucy icon  under kde  3 doubleclicks or 
 * 3 times clicking on Maximise.. needed according to hali -
 * -> might be problem with no active workbench window if minimized..
 * --> tried and could not be verified.. i.e. works fine with Kubuntu
 * 
 *  
 * TODO warning when running out of space.. 
 * -> not possible due to Missing api in 1.5 
 * --> wait for 1.6 as requirement
 * 
 * TODO LuaJava plugin... -> wait till java 6 mandatory for "kahlua"
 * otherwise use lua java? probably better -> real lua..
 * use with scripting engine...
 * i.e. 
 * http://cadmium.x9c.fr/downloads.html -> ocaml scripting
 * Jython -> python
 * more: https://scripting.dev.java.net/
 *  
 * @author Quicksilver
 */
public final class DCClient {
 
	public static final Logger logger = LoggerFactory.make();
	
	private static Initializer init = new Initializer();
	
	public static void setInitializer(Initializer initializer) {
		synchronized(synchSingleton) {
			init = initializer;
		}
	}
	

	
	private static volatile long debugcount = 0;
	
	/*
	 * 
	 * @deprecated use scheduled executor
	 */
	public static void execute(final Runnable r) {
		if (++debugcount % 5000 == 0) {
			logger.debug("Totally submitted tasks " + debugcount);
		}
		exec.execute(new Runnable() {
			public void run() {
				try {
					r.run();
				} catch (Exception e) {
					logger.warn("execution of runnable failed.." + e
							+ " isShutdown:" + exec.isShutdown(), e);
				}
			}
		});
	}
	
	public void executeDir(final Runnable r) {
		scheduler.execute(new Runnable() {
			public void run() {
				try {
					r.run();
				} catch (Exception e) {
					logger.warn("sexecution of runnable failed.." + e
							+ " isShutdown:" + scheduler.isShutdown(), e);
				}
			}
		});
	}
	
	/**
	 * timer replacement..
	 */
	private final ScheduledExecutorService scheduler;
		
	public static ScheduledExecutorService getScheduler() {
		return get().scheduler;
	}
	
	public ScheduledExecutorService getSchedulerDir() {
		return scheduler;
	}
	
	{
		ScheduledThreadPoolExecutor ste = new ScheduledThreadPoolExecutor(5);
		ste.setRejectedExecutionHandler(new DiscardPolicy());
		
		scheduler = Executors.unconfigurableScheduledExecutorService(ste);
	}
	
	/**
	 * cached Thread pool executor with  CallerRunsPolicy to better find problems..
	 * at least better than discard policy..
	 */
	private static final ExecutorService exec = new ThreadPoolExecutor(1, 150,
            10L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new DiscardPolicy() 
            );
	


	private static DCClient singleton;
	private static final Object synchSingleton = new Object();
	
	/**
	 * 
	 * 
	 */
	public static DCClient get() {
		synchronized(synchSingleton) {
			return singleton;
		}
	}
	
	private final User fileListSelf; //the self that holds our own FileList

	
	
	private final Object synchAway = new Object();
	private  boolean away = false;
	private String awayMessage;
	private final Observable<String> awayObservable = new Observable<String>();
	
	/**
	 * running hubs..
	 */
	private final Map<FavHub,Hub> hubs	= 
		Collections.synchronizedMap(new HashMap<FavHub,Hub>());
	
	private final Map<String,Identity> loadedIdentiies =
		Collections.synchronizedMap(new HashMap<String,Identity>());
	


	/**
	 * maps an IP-string  to an hub .. this is needed so SearchResults can resolve users
	 * ex.  87.35.157.86
	 * no DNS mapping!
	 */
	private final Map<String,WeakReference<Hub>> ipToHub = 
		Collections.synchronizedMap( new HashMap<String,WeakReference<Hub>>());


	private final CopyOnWriteArrayList<ISearchResultListener> searchListeners = 
			new CopyOnWriteArrayList<ISearchResultListener>();
	
	private final CopyOnWriteArrayList<IDCCControlListener> hublisteners = 
			new CopyOnWriteArrayList<IDCCControlListener>();
	
	private final CopyOnWriteArrayList<ISearchReceivedListener> srl =
			new CopyOnWriteArrayList<ISearchReceivedListener>();
	
	private final CopyOnWriteArrayList<IInfoChanged> changedInfo =
		new CopyOnWriteArrayList<IInfoChanged>();
	
	private final CopyOnWriteArrayList<ILogEventListener> logEventListener=
		new CopyOnWriteArrayList<ILogEventListener>();
	
	private final Set<InfoChange> changes = 
		Collections.synchronizedSet(new HashSet<InfoChange>());
	

//	/**
//	 * @deprecated -> Identity
//	 */
//	private final ConnectionHandler ch;  

	private final IUploadQueue upQueue ;
	private final IUploadQueue downQueue ;
	
	
	
//	private final IConnectionDeterminator connectionDeterminator;

	private final IFavHubs favHubs;
	private final FavFolders favFolders;

	private final ISlotManager slotManager;
	
	private final Population population;
	private final StoredPM storedPM;
	private final DownloadQueue downloadQueue;
	
//	/**
//	 * @deprecated -> Identity
//	 */
//	private final ICryptoManager cryptoManager;
	
	private final DefaultIdentity defaultIdentity;


	public Identity getDefaultIdentity() {
		return defaultIdentity;
	}



	private final Object infSynch = new Object();
	private ScheduledFuture<?> myInfoUpdater = null;
	
	/**
	 * current version as it is used in 
	 * Description
	 */
	public static final String VERSION; 
	
	/**
	 * longer version string for human readability..
	 */
	public static final String LONGVERSION;
	
	public static final String URL = "http://jucy.eu";
	
	static {
		String v = " V:" + Version.getVersion()+ (Platform.inDevelopmentMode()?"d":"");
		VERSION = "UC"+v;  
		LONGVERSION = "jucy"+v;
		
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				logger.warn(e,e);
			}
		});
	}

	private final OwnFileList filelist;
	private final IHashEngine hashEngine; 
	private final IDatabase database;
	private final List<IFilelistProcessor> filelistProcessors;

	private final List<IOperatorPlugin> operatorPlugins;
	
	private final IUDPHandler udphandler;

	private final AutomaticSearchForAlternatives altSearch;
	

	private final IUserChangedListener databaseUserChanges = new IUserChangedListener() {
		public void changed(UserChangeEvent uce) {
			switch(uce.getDetail()) {
			case UserChangeEvent.DOWNLOADQUEUE_ENTRY_PRE_ADD_FIRST:
			case UserChangeEvent.DOWNLOADQUEUE_ENTRY_POST_REMOVE_LAST:
			case UserChangeEvent.FAVUSER_ADDED:
			case UserChangeEvent.FAVUSER_REMOVED:
			case UserChangeEvent.SLOT_GRANTED:
			case UserChangeEvent.SLOTGRANT_REVOKED:
			case UserChangeEvent.SLOTGRANT_CHANGED:
				logger.debug("Persistance change for user: "+uce.getChanged());
				database.addUpdateOrDeleteUser(uce.getChanged());
			}
		}
	};

    /**
     * Creates a new instance of DCClient 
     * and all its objects
     */
    public DCClient() {
    	logger.debug("creating client");
    	favFolders = new FavFolders();
    	population = new Population(this);
    	storedPM = new StoredPM(population);
    	altSearch =  new AutomaticSearchForAlternatives(this);
    	downloadQueue = new DownloadQueue(this);
    	
    	database = init.loadDB(this);
    	
    	defaultIdentity = new DefaultIdentity(this);
    	
//    	pid = loadPID();
//    	cryptoManager = new CryptoManager(this);
    	
    	hashEngine = init.loadHashEngine();
    	filelistProcessors = init.loadFilelistProcessors();
    	operatorPlugins = init.loadOperatorPlugins();
    //	connectionDeterminator = init.createConnectionDeterminator(this);
    	favHubs = init.getFavHubs(this);
    	slotManager = init.createSlotManager(this);
    	upQueue = init.createUploadQueue(this, true);
    	downQueue = init.createUploadQueue(this, false);
    	
    	fileListSelf = new User(this,PI.get(PI.nick), defaultIdentity.getPID()) { 
			@Override
			public String getNick() { 
				return PI.get(PI.nick);
			}
			@Override
			public HashValue getCID() {
				return defaultIdentity.getCID();
			}
			@Override
			public HashValue getPD() {
				return defaultIdentity.getPID();
			}
			
    	};
   
    	
    	filelist = new OwnFileList(fileListSelf,database,hashEngine,this); 
        
    //    ch = new ConnectionHandler(this); 
     
        udphandler = init.createUDPHandler(this); 
        
        PI.get().addPreferenceChangeListener(new IPreferenceChangeListener() {
			public void preferenceChange(PreferenceChangeEvent event) {
				String pref = event.getKey();
				logger.debug("pref changed: "+pref);
				if (pref.equals(PI.eMail) || 
						pref.equals(PI.description) || 
						pref.equals(PI.connectionNew)||
						pref.equals(PI.passive) ||
						pref.equals(PI.uploadLimit) ||
						pref.equals(PI.nick) ||
						pref.startsWith("socksProxy")) {
					
					notifyChangedInfo(InfoChange.Misc);
					
				} else if(pref.equals(PI.slots) ) {
					notifyChangedInfo(InfoChange.Slots);
				}
				
			}
        	
        });
        logger.debug("created client");
        
    }
    

    private Identity getIdentity(String identityname) {
    	Identity identity = loadedIdentiies.get(identityname);
    	if (identity == null) {
    		return defaultIdentity;
    	} else {
    		return identity;
    	}
    }

    
    /**
     * sends a search to all provided hubs ..
     * @param search  the search what should be searched.. and where the results should go to..
     * @param hubsToSearch which hubs should be searched..
     */
    public void search(FileSearch search, Set<IHub> hubsToSearch) {
    	if (!hubsToSearch.isEmpty()) {
	    	udphandler.addKeyExpected(search.getEncryptionKey());
	    	HashSet<Identity> usedBySearch = new HashSet<Identity>();
    		for (IHub hub:hubsToSearch) {
    			Identity i = hub.getIdentity();
    			if (usedBySearch.add(i)){
    				i.getConnectionDeterminator().searchStarted(search);
    			}
    			
	    		hub.search(search);
	    	}
    	}
    }
    
    /**
     * search all hubs..
     * @param search  the search what should be searched.. and where the results should go to.
     */
    public void search(FileSearch search) {
    	search(search,new HashSet<IHub>(hubs.values()));
    }
    
    /**
     * 
     * @return a user holding our filelist
     */
    public IUser getFilelistself() {
    	return fileListSelf;
    }
    
    /**
     * 
     * @return our own filelist..
     */
    public FileList getOwnFileList() {
    	return filelist.getFileList();
    }
    
    /**
     * starts a FileList refresh in a separate Thread
     */
    public void refreshFilelist() {
    	filelist.refresh(false);
    }
    
    /**
     * clears all HashData from the DownloadQueue
     * forcing rehashing all Files on the disc
     */
    public void rebuildFilelist() {
    	database.deleteAllHashedFiles();
    	refreshFilelist();
    }
    
    /**
     * deletes all File hashes of the DB 
     * that are currently not in use.
     * 
     * @return the number of files pruned from the database.. 
     */
    public int pruneHashes() {
    	Map<File,HashedFile> pruned =  database.pruneUnusedHashedFiles();
    	if (logger.isDebugEnabled()) {
    		for (File f:pruned.keySet()) {
    			logger.debug("Deleted hash for: "+f.getPath());
    		}
    	}
    	return pruned.size();
    }

//    /**
//     * initialises the own FileList, as this takes the most
//     * time here separate method that can be called from the 
//     * splashScreen is provided..
//     */
//    synchronized Future<?> initialiseFilelist() {
//    	if (null == fileListInitialised) {
//    		fileListInitialised = start(filelist);
//    	}
//    	return fileListInitialised;
//    } 

    /**
     * starts the DCClient
     * 
     * called after the GUI is ready...
     *
     */
    public synchronized void start(IProgressMonitor monitor) {
    	if (get() == this) {
    		throw new IllegalStateException("DCClient already started");
    	}
    	synchronized (synchSingleton) {
    		singleton = this;
    	}
    	monitor.beginTask(String.format(LanguageKeys.StartingJucy, DCClient.LONGVERSION), 10);
    	
    	// favs are loaded.. time to register Database to be notified on userchanges..
    	population.registerUserChangedListener(databaseUserChanges); 
    	
    	monitor.worked(1);
    	
    	Future<?> filelistInit =   start(filelist); //initialiseFilelist(); 
    	Future<?> identity = start(defaultIdentity);
 
    	upQueue.start();
    	downQueue.start();
    	
    	
    	udphandler.start();
    	monitor.worked(1);


    	downloadQueue.loadDQ(); 
    	monitor.worked(2);
    	
    	
    	try { //wait for deferred tasks...
    		if (PI.getLong(PI.lastFilelistSize) == 0) {
    			filelistInit.get();
    		}
    		identity.get();
    	} catch(InterruptedException e) {
    		logger.warn(e,e);
    	} catch (ExecutionException e) {
    		logger.warn(e,e);
		}
    	monitor.worked(3);
    	storedPM.start();
    	
    	logEvent(LanguageKeys.StartingHubs); 
    	//downloads can be started..
    	favHubs.openAutoStartHubs();
    	monitor.worked(3);
    	
    	altSearch.start();
    	slotManager.init();

    	monitor.done();
    	
    	while (true) { //Its a DC Client after all .. wouldn't work without this , kudos to NEV!
    		break;
    	}
    	
    	Security.setProperty("networkaddress.cache.ttl", "60"); //cache positive results only for 1 minute... because of dyndns..
    	
    }
    
    /**
     * called to on closing by the GUI....hopefully..
     * @param shutDownExec .. if set to false
     * executor is let alive.. making it possible to create
     * a new DCClient object..
     */
    public void stop(boolean shutDownExec) {
    	logEvent(LONGVERSION+" is stopping");
    	if (PI.getBoolean(PI.deleteFilelistsOnExit)) {
    		FileList.deleteFilelists();
    	}
    	storedPM.stop();
    	for (IHub hub:getHubs().values()) {
    		hub.close();
    	}
    	Future<?> stop = stopAll(hashEngine,downloadQueue,altSearch,filelist,udphandler,defaultIdentity);
    	
    	try {
			stop.get();
		} catch (InterruptedException e) {
			logger.warn(e, e);
		} catch (ExecutionException e) {
			logger.warn(e, e);
		}

    	if (shutDownExec) {
    		exec.shutdown();
    	}
    	scheduler.shutdown();
    	
    	population.unregisterUserChangedListener(databaseUserChanges);
    	logger.debug("shut down executors");
    	database.shutdown();
    	logger.debug("shut down database");
    	logEvent(LONGVERSION+" stopped");
    	
    }
    
    public void restart() {
    	for (IDCCControlListener c:hublisteners) {
    		c.requireRestart();
    	}
    }

    
  
    

	/**
     * creates tag  
     * (i.e. something like <UC 0.03,M:A,H:0/0/1,S:2> )
     * 
     */
    public String getTag(Identity id) {
    	int[] hubs = getNumberOfHubs(false);
    	return String.format("<%s,M:%c,H:%d/%d/%d,S:%d%s>", VERSION ,
    			id.getMode().getModeChar(),hubs[0],hubs[1],hubs[2],getTotalSlots(),getAdditionalTag());
    }
    
    private String getAdditionalTag() {
    	String s = "";
    	int ul = PI.getInt(PI.uploadLimit);
    	if (ul > 0) {
    		s+=",L:"+ul;
    	}
    	
    	return s;
    }
    
    /**
     * 
     * @return a 3 dimensional array containing <p>
     * on index 0 - normal hubs <br>
     * on index 1 - registered hubs <br>
     * on index 2 - OP hubs 
     */
    public int[] getNumberOfHubs(boolean countChatOnly) {
    	int normal = 0,registered = 0, ophub = 0;
    
    	for (Hub hub:hubs.values()) {
    		if (hub.getState() == ConnectionState.LOGGEDIN && 
    				(countChatOnly || !hub.getFavHub().isChatOnly())) {
    			
	    		if (hub.isOpHub()) {
	    			ophub++;
	    		} else if (hub.isRegistered()) {
	    			registered++;
	    		} else {
	    			normal++;
	    		}
    		}
    	}
    	return new int[] {normal,registered,ophub};
    }
    
//    /**
//     * @return the connection set by the user
//     * i.e. 0.02 or 50  
//     */
//    public String getConnection() {
//    	return SizeEnum.toShortSpeedString(PI.getLong(PI.connectionNew));
//    }
    

    

    
//	/**
//	 * check if TLS was enabled at start of Program .. and is now in a usable state.
//	 * @return true if TLS is fully initialized and running..
//	 * 
//	 */
//	public boolean currentlyTLSSupport() {
//		return  ch.isTLSRunning() &&  PI.getBoolean(PI.allowTLS);
//	}
    
    
   /**
    * removes a mapping for a hub
    * from the list of all open hubs..
    * !! this is only called by the hub's destroy method..
    */
    public void internal_unregisterHub(FavHub favHub) {
    	
    	//first remove mapping from hubId to hub
    	Hub hub = hubs.remove(favHub);
    	if (hub != null) {
	    	Identity id = hub.getIdentity();
	    	id.removeRunningHub(favHub);
	    	
	    	//the inform hubs slowly of change..
	    	notifyChangedInfo(InfoChange.Hubs);
    	}
    }
    
    
    public Hub getHub(FavHub favHub, final boolean showInUI) {

    	Hub hub = hubs.get(favHub);
    	if (hub != null) {
    		return hub;
    	}
    	synchronized (this) {
    		hub = hubs.get(favHub);
    		if (hub == null) {
    			final FavHub ifavHub = favHubs.internal(favHub);
    			Identity id = getIdentity(ifavHub.getIdentityName());
    			id.addRunningHub(ifavHub);
    			hub = new Hub(ifavHub,id,this);
    			final Hub iHub = hub;
    			hubs.put(ifavHub, hub);
    			final Semaphore creation = new Semaphore(0,false);
    		
    			DCClient.execute(new Runnable() {
    				public void run() {
    					Semaphore sem = new Semaphore(0,false);

    					for (IDCCControlListener hubl:hublisteners) {
    						hubl.hubCreated(ifavHub,showInUI,sem);
    					}
    					creation.release();
    					sem.acquireUninterruptibly(hublisteners.size());
    					iHub.start();
    					notifyChangedInfo(InfoChange.Hubs);
    				}
    			});
    			creation.acquireUninterruptibly();
    		}
    	}

    	return hub;
    }
    
    public boolean isRunningHub(FavHub favHub) {
    	return hubs.containsKey(favHub);
    }
    
    /**
     * @return an unmodifiable view of the currently open hubs..
     */
    public Map<FavHub,Hub> getHubs() {
    	return Collections.unmodifiableMap(hubs);
    }
    
    
    /**
     *  registers a ISearchresult listener with the client
     *  
     * @param sirl - a search result listener .. for example a SearchEditor
     */
    public void register(ISearchResultListener sirl) {
    	searchListeners.addIfAbsent(sirl);
    }
    
    /**
     *  unregisters a ISearchresult listener with the client so he will no 
     *  longer receive notification for incoming searches
     *  
     * @param sirl - a search result listener .. for example a SearchEditor
     */
    public void unregister(ISearchResultListener sirl) {
    	searchListeners.remove(sirl);	
    }
    
    public int searchListenersRegistered() {
    	return searchListeners.size();
    }
    

    public void register(IInfoChanged iic) {
    	changedInfo.addIfAbsent(iic);
    }
    
 
    public void unregister(IInfoChanged iic) {
    	changedInfo.remove(iic);	
    }
    
    /**
     * this method is called by Hubs 
     * it delegates the searchResults to anyone interested..
     * 
     * @param sr - the sr received
     */
    public void srReceived(ISearchResult sr) {
	    for (ISearchResultListener srl: searchListeners) {
	    	srl.received(sr);
	   	}
    }
    
//    public boolean isActive() {
//    	return !PI.getBoolean(PI.passive) ;
//    }
   
    
    /**
     * resolves a user only by his nick ... may result in the wrong user
     * @param nick - the nick searched
     * @return the user found  null if none
     */
    public User getUserByNick(String nick) {
    	synchronized(hubs){
    		for (Hub hub : hubs.values()) {
    			if (hub.isNMDC()) {
    				User usr = hub.getUserByNick(nick);
    				if (usr != null) {
    					return usr;
    				}
    			}
    		}
    	}
    	return null;
    }
    
//    public IUser getUserForCID(HashValue cid) {
//    	synchronized(hubs){
//    		for (Hub hub : hubs.values()) {
//    			if (!hub.isNMDC()) {
//	    			User usr = hub.getUserByCID(cid);
//	    			if (usr != null) {
//	    				return usr;
//	    			}
//    			}
//    		}
//    	}
//    	
//    	return null;
//    }
    
    public List<IUser> getUsersForCID(HashValue cid) {
    	List<IUser> users = new ArrayList<IUser>();
    	synchronized(hubs){
    		for (Hub hub : hubs.values()) {
    			if (!hub.isNMDC()) {
	    			User usr = hub.getUserByCID(cid);
	    			if (usr != null) {
	    				users.add(usr);
	    			}
    			}
    		}
    	}
    	return users;
    }

    
    /**
     * sends MyInfo to every hub about a changed Info
     *  (message may be delayed based on the type)
     */
    public void notifyChangedInfo(InfoChange type) {
    	if (type.isSeparateRefresh()) {
    		for (IInfoChanged iic: changedInfo) {
				iic.infoChanged(Collections.singleton(type));
			}
    	} else {
	    	if (changes.add(type)) {
		    	synchronized(infSynch) {
			    	if (myInfoUpdater != null && myInfoUpdater.getDelay(TimeUnit.MILLISECONDS) > type.getDelay()) {
			    		myInfoUpdater.cancel(false);
			    		myInfoUpdater = null;
			    	}
			    	
			    	if (myInfoUpdater == null) {
			    		myInfoUpdater = scheduler.schedule(new Runnable() {
			    			public void run() {
			    				synchronized(infSynch) {
			    					myInfoUpdater = null;
			    				}
			    				for (Hub hub : hubs.values()) {
			    					synchronized(hub) {
				    					if (hub.getState() == ConnectionState.LOGGEDIN) {
				    						hub.sendMyInfo();
				    					}
			    					}
			    					Set<InfoChange> copy;
			    					synchronized(changes) {
			    						copy = new HashSet<InfoChange>(changes);
			    						changes.clear();
			    					}
			    					for (IInfoChanged iic: changedInfo) {
			    						iic.infoChanged(copy);
			    					}
			    				}
			    						
			    			}
			    		}, type.getDelay(), TimeUnit.MILLISECONDS);
			    	}
		    	}
	    	}
    	}
    }
    
    public static interface ILogEventListener {
    	void logEvent(String event);
    }
    
    public void logEvent(String event) {
    	for (ILogEventListener loel:logEventListener) {
    		loel.logEvent(event);
    	}
    }
    
    /**
     * determines hub by nick of sender and hub-ip
     * @param nick - nick of SR sender
     * @param hubip - the hubIP in the SR string
     * @return  the hub that matches the provided values
     * null if none can be found
     */
    public Hub hubForNickAndIP(String nick, String hubip) {
    	WeakReference<Hub> refHub = ipToHub.get(hubip);
    	if (refHub != null && refHub.get()!= null) {
    		return refHub.get();
    	}
    	User usr = getUserByNick(nick);
    	if (usr != null) {
    		ipToHub.put(hubip, new WeakReference<Hub>(usr.getHub()));
    		return usr.getHub();
    	}
    	
    	return null;
    }
    
 
    public void register(IDCCControlListener hubl) {
    	hublisteners.addIfAbsent(hubl);
    }
    
    public void unregister(IDCCControlListener hubl) {
    	hublisteners.remove(hubl);
    }
    
	public boolean addLogEventListener(ILogEventListener element) {
		return logEventListener.addIfAbsent(element);
	}

	public boolean removeLogEventListener(ILogEventListener element) {
		return logEventListener.remove(element);
	}

	public void registerSRL(ISearchReceivedListener listener) {
		srl.addIfAbsent(listener);
	}
	
	public void unregisterSRL(ISearchReceivedListener listener) {
		srl.remove(listener);
	}
	
	/**
	 * (only used for SearchSpy)
	 * 
	 * notifies the client that a search was received
	 * @param searchStrings what search strings are searched for.. size will be one if its a tth
	 * @param source - if active this will eb an inetSocketAddress if passive a User
	 */
	public void searchReceived(Set<String> searchStrings,Object source, int nrOfResults) {
		for (ISearchReceivedListener listener:srl) {
			listener.searchReceived(searchStrings, source, nrOfResults);
		}
	}

	/**
	 * @return the hashEngine
	 */
	public IHashEngine getHashEngine() {
		return hashEngine;
	}
	
	/**
	 * 
	 * @return TODO for multiple identities this needs to be changed..
	 */
	public List<ConnectionHandler> getAllConnectionHandler() {
		return Collections.singletonList(defaultIdentity.getConnectionHandler());
	}


	public IDatabase getDatabase() {
		return database;
	}

	public IOwnFileList getFilelist() {
		return filelist;
	}

	public IUDPHandler getUDPhandler() {
		return udphandler;
	}


	
	/**
	 * 
	 * @return value= total slots - currently in use slots.
	 * so it returns the value usually sent in search messages.  
	 */
	public int getCurrentSlots() {
		return slotManager.getCurrentSlots();
	}


	/**
	 * 
	 * @return totalSlots  - the current maximum of slots
	 */
	public int getTotalSlots() {
		return slotManager.getTotalSlots();
	}


	public ISlotManager getSlotManager() {
		return slotManager;
	}

	public List<IFilelistProcessor> getFilelistProcessors() {
		return filelistProcessors;
	}

	public boolean isAway() {
		synchronized(synchAway) {
			return away;
		}
	}


	/**
	 * 
	 * @param away - true for away - false for back
	 * away message is default away message if true
	 */
	public void setAway(boolean away) {
		if (away) {
			setAway(PI.get(PI.defaultAFKMessage));
		} else {
			synchronized(synchAway) {
				this.away = false;
				awayObservable.notifyObservers(awayMessage);
			}
		}
	}
	
	/**
	 * sets a way to true
	 * @param awayMessage - with provided away message
	 */
	public void setAway(String awayMessage) {
		synchronized(synchAway) {
			this.awayMessage = awayMessage;
			this.away = true;
			awayObservable.notifyObservers(awayMessage);
		}
	}


	public Observable<String> getAwayObservable() {
		return awayObservable;
	}

	public String getAwayMessage() {
		synchronized(synchAway) {
			return awayMessage+" <"+LONGVERSION+">";
		}
	}

	


	public List<IOperatorPlugin> getOperatorPlugins() {
		return Collections.unmodifiableList(operatorPlugins);
	}


	public IUploadQueue getUpQueue() {
		return upQueue;
	}

	public IUploadQueue getDownQueue() {
		return downQueue;
	}
	
	public IUploadQueue getUpDownQueue(boolean up) {
		return up? upQueue: downQueue;
	}

	

	public IFavHubs getFavHubs() {
		return favHubs;
	}


	public FavFolders getFavFolders() {
		return favFolders;
	}


	public Population getPopulation() {
		return population;
	}

	public DownloadQueue getDownloadQueue() {
		return downloadQueue;
	}

    public StoredPM getStoredPM() {
		return storedPM;
	}




	public static class Initializer {
		
		protected IFavHubs getFavHubs(DCClient dcc) {
			return new FavHubs(dcc);
		}
		

		
		protected IUploadQueue createUploadQueue(DCClient dcc,boolean upload) {
			return new UploadQueue(dcc);
		}
		
		protected IUDPHandler createUDPHandler(DCClient dcc) {
			return new UDPhandler(dcc);
		}
		
	    
	    /**
	     * loads the database plug-in
	     *
	     */
	    protected IDatabase loadDB(DCClient dcc) {
	    	String idToLoad = PI.get(IDatabase.ExtensionpointID);
	    	IExtensionRegistry reg = Platform.getExtensionRegistry();
	    	
			IConfigurationElement[] configElements = reg
			.getConfigurationElementsFor(IDatabase.ExtensionpointID);
			
			IDatabase db = null;
			
			for (IConfigurationElement element : configElements) {
				logger.debug(element.getAttribute("id"));
				try {
					if(idToLoad.equals(element.getAttribute("id")) ) {
						
						db = (IDatabase) element.createExecutableExtension("class");
						
						db.init(PI.getStoragePath(),dcc);
					}
				} catch (CoreException e) {
					logger.error("Can't load the Database!",e);
					
				} catch (Exception e) {
					throw new Error("Error initializing database: "+e.getMessage(),e);
				}
			}
			if (db == null) {
				throw new Error("no database found");
			} 
			return db;
	    }
	    
	    
	    /**
	     * loads the hash-engine-plug-in
	     */
	    protected IHashEngine loadHashEngine() {
	    	String idToLoad= PI.get(IHashEngine.ExtensionpointID);
	    	IExtensionRegistry reg = Platform.getExtensionRegistry();
	    	
			IConfigurationElement[] configElements = reg
			.getConfigurationElementsFor(IHashEngine.ExtensionpointID);

			IHashEngine he = null;
			
			for (IConfigurationElement element : configElements) {
				try {
					if (idToLoad.equals(element.getAttribute("id")) ) {
						he = (IHashEngine) element.createExecutableExtension("class");
					}
				} catch (CoreException e) {
					logger.error("Can't load the HashEngine!",e);
				} 
			}
			if (he == null ) {
				throw new Error("no hashEngine found");
			} 
			he.init();
			
			return he;
	    }
	    
	    /**
	     * loads all FileList processor plug-ins
	     * @return All plug-ins available..
	     */
	    protected List<IFilelistProcessor> loadFilelistProcessors() {
	    
	    	IExtensionRegistry reg = Platform.getExtensionRegistry();
	    	
			IConfigurationElement[] configElements = reg
			.getConfigurationElementsFor(IFilelistProcessor.ExtensionpointID);

			List<IFilelistProcessor> processor = new ArrayList<IFilelistProcessor>(configElements.length);
			
			for (IConfigurationElement element : configElements) {
				try {
					IFilelistProcessor fp = (IFilelistProcessor) element.createExecutableExtension("class");
					processor.add(fp);
				} catch (CoreException e) {
					logger.error("Can't load the FilelistProcessor: "+element.getAttribute("id"),e);
				} 
			}
		
			return processor;
	    }
	    
	    protected  List<IOperatorPlugin> loadOperatorPlugins() {
	    	IExtensionRegistry reg = Platform.getExtensionRegistry();
	    	
			IConfigurationElement[] configElements = reg
			.getConfigurationElementsFor(IOperatorPlugin.PointID);

			List<IOperatorPlugin> opPlugins = new ArrayList<IOperatorPlugin>();
			
			for (IConfigurationElement element : configElements) {
				try {
					
					IOperatorPlugin fp = (IOperatorPlugin) element.createExecutableExtension("class");
					opPlugins.add(fp);
				} catch (CoreException e) {
					logger.error("Can't load the Op Plugin: "+element.getAttribute("name")+"  "+element.getAttribute("id"),e);
				} 
			}
		
			return opPlugins;
	    }
	    
	    protected ISlotManager createSlotManager(DCClient dcc) {
	    	return new SlotManager(dcc);
	    }
		
	}
	
 
	public Future<?> start(final IStartable startable) {
		return scheduler.submit(new Runnable() {
			public void run() {
				startable.start();
			}
		});
	}
	
	
	public Future<?> stop(final IStoppable startable) {
		return scheduler.submit(new Runnable() {
			public void run() {
				startable.stop();
			}
		});
	}
	
	private Future<?> stopAll(IStoppable... stoppables) {
		final List<Future<?>> future = new ArrayList<Future<?>>();
		for (IStoppable s:stoppables) {
			Future<?> f= stop(s);
			future.add(f);
		}
		return scheduler.submit(new Runnable() {
			public void run() {
				for (Future<?> f:future) {
					try {	
						f.get();
					} catch (InterruptedException e) {
						logger.warn(e,e);
					} catch (ExecutionException e) {
						logger.warn(e, e);
					}
				}
			}
		});
	}

	
}
