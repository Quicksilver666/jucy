package uc.files.search;

import helpers.PreferenceChangedAdapter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;


import org.apache.log4j.Logger;


import uc.DCClient;
import uc.PI;
import uc.IStoppable.IStartable;
import uc.crypto.HashValue;
import uc.files.downloadqueue.AbstractFileDQE;
import uc.files.search.SearchResult.ISearchResultListener;

public class AutomaticSearchForAlternatives implements Runnable, ISearchResultListener ,IStartable {
	
	private static Logger logger = LoggerFactory.make();
	

	private final Set<HashValue> alreadySearched = new HashSet<HashValue>();
	private static final int SEARCHINTERVAL = PI.getInt(PI.autoSearchInterval) * 60 ; 
	
	private ScheduledFuture<?> task = null;
	private final PreferenceChangedAdapter pca;
	
	private final DCClient dcc;
	
	private final Comparator<AbstractFileDQE> comp = new Comparator<AbstractFileDQE>() {
		public int compare(AbstractFileDQE o1, AbstractFileDQE o2) {
	
			int i = Integer.valueOf(o1.getNrOfUsers()).compareTo(o2.getNrOfUsers());
			if (i == 0) { 
				i = Integer.valueOf(o1.getNrOfRunningDownloads()).compareTo(o2.getNrOfRunningDownloads());
			}
			if (i == 0) { //higher priority first..
				i = -Integer.valueOf(o1.getPriority()).compareTo(o2.getPriority());
			}
			
			return i;
		}
	};
	
	public AutomaticSearchForAlternatives(DCClient dcc) {
		this.dcc = dcc;
		pca = new PreferenceChangedAdapter(PI.get(),PI.autoSearchForAlternates) {
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				boolean newVal = Boolean.valueOf(newValue);
				if (newVal) {
					startS();
				} else {
					stopS();
				}
			}
		};
	}
	
	public void start() {
		startS();
		pca.reregister();
	}
	
	public void stop() {
		stopS();
		pca.dispose();
	}
	
	
	public void startS() {
		if (task == null) {
			dcc.register(this);
			task = dcc.getSchedulerDir().scheduleAtFixedRate(
					this,SEARCHINTERVAL,SEARCHINTERVAL, TimeUnit.SECONDS);
		}
	}
	
	public void stopS() {
		if (task != null) {
			dcc.unregister(this);
			task.cancel(false);
			task = null;
		}
	}
	
	private AbstractFileDQE nextOnSearch() {
		List<AbstractFileDQE> filedqes = dcc.getDownloadQueue().getAllFileDQE();
		Collections.sort(filedqes,comp);
		for (AbstractFileDQE afdqe:filedqes) {
			if (!alreadySearched.contains(afdqe.getTTHRoot())) {
				return afdqe;
			}
		}
		return null;
	}
	
	public void run() {
		searchNext();
	}
	
	public void searchNext() {
		AbstractFileDQE adqe = nextOnSearch();
		if (adqe != null) {
			alreadySearched.add(adqe.getTTHRoot());
			dcc.search( new FileSearch(adqe.getTTHRoot()));
			logger.debug("Searching for: "+adqe.getFileName()+"  "+alreadySearched.size());
		} else {
			alreadySearched.clear();
			logger.debug("Clearing already searched");
		}
	}
	
	public boolean isSearchForAlternatesEnabled() {
		return task != null;
	}

	public void received(ISearchResult sr) {
	    //add user if file is already in Queue..
	    if (sr.isFile() && 
	    		isSearchForAlternatesEnabled() && 
	    		dcc.getDownloadQueue().containsDQE(sr.getTTHRoot())) {
	    	logger.debug("found alt: "+sr.getName());
	    	sr.download();
	    }
		
	}
	
	
	
	
}
