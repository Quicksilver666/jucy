package eu.jucy.op;

import helpers.GH;

import java.util.Collection;
import java.util.Collections;

import java.util.Map;



import eu.jucy.gui.filelist.FilelistHandler;
import eu.jucy.op.CounterFactory.WorkingCounter;

import uc.IHub;
import uc.IUser;
import uc.files.filelist.FileListFile;
import uihelpers.SUIJob;

public class OPAction {



	
	/**
	 * raw to send
	 */
	protected String raw = "";
	
	/**
	 * open the FileList into the view of the user
	 */
	protected boolean openFileList;

	/**
	 * increment some other counter by name
	 */
	protected String incrementCounter = "";
	
	/**
	 * how much the counter should be incremented..
	 */
	protected int incrementByWhat;
	
	
	protected OPAction(String[] arr,int extrafields) {
		raw = arr[extrafields];
		openFileList = Boolean.parseBoolean(arr[extrafields+1]);
		incrementCounter = arr[extrafields+2];
		incrementByWhat = Integer.parseInt(arr[extrafields+3]);
	}
	
	protected OPAction() {}
	
	protected void execute(final IUser usr,FileListFile f, Collection<FileListFile> allFiles,int count,
			String comment,Map<String,WorkingCounter> otherCounters) {
		
		IHub hub = usr.getHub();
		if (usr == null || hub == null)
			return;
		
		if (openFileList) {
			new SUIJob() {
				public void run() {
					FilelistHandler.openFilelist(usr, getWindow());
				}
			}.schedule();
		}
		
		if (!GH.isNullOrEmpty(raw)) {
			hub.sendRaw(raw, new OpADLSendContext(hub,f,allFiles ,count,comment));
		
		}
		if (incrementByWhat != 0 && !GH.isNullOrEmpty(incrementCounter)) {
			WorkingCounter other = otherCounters.get(incrementCounter);
			if (other != null) {
				other.addFiles(allFiles, incrementByWhat);
			}
		}
	}
	
	protected void execute(IUser usr,FileListFile f,int count,
			String comment,Map<String,WorkingCounter> otherCounters) {
		execute(usr,f, Collections.singleton(f),count,comment,otherCounters);
	}
	
	public String[] toStringAR(int extraFields) {
		String[] s = new String[extraFields+4];
		s[extraFields] = raw;
		s[extraFields+1] = ""+openFileList;
		s[extraFields+2] = incrementCounter;
		s[extraFields+3] = ""+incrementByWhat;
		return s;
	}
	
	
	
}
