package eu.jucy.gui.itemhandler;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.swt.program.Program;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;

import uc.files.IDownloadable;
import uc.files.IHasDownloadable;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.search.SearchType;

public class PropertyTesterHasDownloadable extends PropertyTester {

	private static final Logger logger = LoggerFactory.make();
	
	public boolean test(Object receiver, String property, Object[] args,Object expectedValue) {
		
		IDownloadable ihd = ((IHasDownloadable)receiver).getDownloadable();
		if ("isEmpty".equals(property)) {
			boolean found = ihd == null;
			logger.debug("found is Empty: "+found+ " "+expectedValue+ "  "+expectedValue.getClass().getSimpleName());
			return expectedValue.equals(found);
		} else if ("isFile".equals(property)) {
			return expectedValue.equals(ihd != null && ihd.isFile());
		} else if ("isExecutable".equals(property)) {
			boolean executable = ihd != null && ihd.isFile() && 
					Program.findProgram(((IDownloadableFile)ihd).getEnding()) != null;
			return expectedValue.equals(executable);
		} else if ("isInQueue".equals(property)) {
			boolean inQueue = ihd != null && ihd.isFile();
			if (inQueue) {
				AbstractDownloadQueueEntry adqe =  ApplicationWorkbenchWindowAdvisor.get()
								.getDownloadQueue().get(ihd.getTTHRoot());
				inQueue = adqe != null;
			}
			return expectedValue.equals(inQueue);
		} else if ("isPreviewable".equals(property)) {
			boolean previewable = ihd != null && ihd.isFile();
			if (previewable) {
				IDownloadableFile idf = (IDownloadableFile)ihd;
				AbstractDownloadQueueEntry adqe =  ApplicationWorkbenchWindowAdvisor.get()
								.getDownloadQueue().get(ihd.getTTHRoot());
				previewable = adqe != null && (adqe.getSize()*0.01d < adqe.getTempPath().length());
				previewable = previewable && (SearchType.Audio.matches(idf) || SearchType.Video.matches(idf));
			}
			return expectedValue.equals(previewable);
		}
		
		throw new IllegalStateException();
	}

}
