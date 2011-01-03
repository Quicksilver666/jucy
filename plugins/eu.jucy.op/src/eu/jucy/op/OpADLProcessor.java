package eu.jucy.op;


import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import eu.jucy.adlsearch.ADLFileListFile;


import eu.jucy.op.CounterFactory.WorkingCounter;
import eu.jucy.op.ui.OpADLFieldEditor;


import uc.DCClient;
import uc.IHub;
import uc.IUser;
import uc.files.filelist.FileList;
import uc.files.filelist.FileListFile;
import uc.files.filelist.IFilelistProcessor;

public class OpADLProcessor implements IFilelistProcessor {

	private static final Logger logger = LoggerFactory.make();
	
	private final List<CounterFactory> allCounters  = new CopyOnWriteArrayList<CounterFactory>(); 
	
	private final List<OpADLEntry> entries = new CopyOnWriteArrayList<OpADLEntry>();
	
	
	
	public OpADLProcessor() {
		refresh();
	}
	
	private void refresh() {
		allCounters.clear();
		allCounters.addAll(OPI.getCounterFactories());
		
		entries.clear();
		entries.addAll(OpADLFieldEditor.LoadOPADLFromString(OPI.get(OPI.opADLEntries)));
		
	}
	

	/*
	 * (non-Javadoc)
	 * @see UC.files.filelist.IFilelistProcessor#processFilelist(UC.files.filelist.FileList, boolean)
	 */
	public void processFilelist(final FileList fileList, boolean onDownload) {
		
		if (onDownload) {
			IUser usr = fileList.getUsr();
			final IHub hub = usr.getHub();
			if (hub != null && Activator.getOPPlugin().isInCheck(usr)) {
				String check = hub.getFavHub().get(OPI.fh_checkUsers);
				if (Boolean.parseBoolean(check)) {
					DCClient.execute(new Runnable() {
						public void run() {
							checkFilelist(fileList, hub);
						}
					});
				}
			}
		}
	}
	
	
	private void checkFilelist(FileList filelist,IHub hub) {
		logger.info("start checking filelist of: "+filelist.getUsr().getNick());
		List<WorkingCounter>  counters = CounterFactory.getWorkingCounter(allCounters, filelist.getUsr());
		
		Map<String,WorkingCounter> counterByName = new HashMap<String,WorkingCounter>();
		for (WorkingCounter c: counters) {
			counterByName.put(c.getName(), c);
		}
		
		boolean stopchecking = false;
		
		out: for (FileListFile f: filelist.getRoot()) {
			if (f.getClass().equals(ADLFileListFile.class)) { //don't check files added by ADLsearch
				continue;
			}
			boolean matched = false;
			for (OpADLEntry adl: entries) {
				if (adl.matches(f)) {
					matched = true;
					boolean breakExec = adl.execute(f,counterByName,hub);
					if (breakExec) {
						stopchecking = true;
						break out;
					}
				}
			}
			if (matched) { //Check the per file counter if they want to finish checking..
				for (WorkingCounter c: counters) {
					boolean breakExec = c.fileFinished();
					if (breakExec) {
						stopchecking = true;
						break out;
					}
				}
			}
		}
		
		for (OpADLEntry adl: entries) {
			adl.finishedSearch();
		}
		
		if (!stopchecking) {
			for (WorkingCounter c : counters) {
				c.evaluate();
			}
		}
		
		Activator.getOPPlugin().checkedUser(filelist.getUsr());

	}


}
