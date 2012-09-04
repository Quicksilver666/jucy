package eu.jucy.gui.logviewer;

import helpers.GH;

import java.io.File;
import java.io.IOException;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.itemhandler.DownloadableHandlers.RemoveDownloadableFromQueueHandler;




import uc.database.DBLogger;
import uc.database.IDatabase;

import uihelpers.SUIJob;

public abstract class LogViewerHandlers extends AbstractHandler {
	
	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event);
		if (!selection.isEmpty()) {
			boolean update = run((DBLogger)selection.getFirstElement(),event);
			
			IEditorPart part = HandlerUtil.getActiveEditor(event);
			if (part instanceof LogViewerEditor && update) {
				logger.debug("Running update");
				((LogViewerEditor)part).update(true,false);
				logger.debug("Running update done");
			}
			
		}
		return null;
	}
	
	public abstract boolean run(DBLogger entity,ExecutionEvent event);
	
	public static class DeleteDBLogger extends  LogViewerHandlers {
		public static final String COMMAND_ID = RemoveDownloadableFromQueueHandler.COMMAND_ID;

		@Override
		public boolean run(final DBLogger entity,ExecutionEvent event) {
			logger.debug("Delete called");
			final LogViewerEditor part =(LogViewerEditor)HandlerUtil.getActiveEditor(event);
			part.setFilter(entity);
			
			new Job(Lang.DeleteLog) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					logger.debug("running delete");
					entity.deleteEntity(monitor);
					logger.debug("deleted");
					new SUIJob() {
						@Override
						public void run() {
							logger.debug("update start");
							part.removeFilter(entity);
							part.update(true,true);
							logger.debug("update done");
						}
					}.schedule();
					return Status.OK_STATUS;
				}
				
			}.schedule();
			return false;
		}
	}
	
	public static class ExportLog extends LogViewerHandlers {
		public static final String COMMAND_ID = "eu.jucy.gui.logviewer.exportlogs";

		@Override
		public boolean run(final DBLogger entity,ExecutionEvent event) {
			FileDialog fd = new FileDialog(HandlerUtil.getActiveShell(event),SWT.SAVE);
			fd.setText("Save");
			 
			fd.setFilterExtensions(new String[]{"*.txt","*.log"});
			fd.setFileName(GH.replaceInvalidFilename(entity.getName())+".txt");
		
			String file = fd.open();
			if (file != null) {
				final File f = new File(file);
				Job job = new Job(f.getName()) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							int count = entity.countLogEntrys();
							monitor.beginTask(f.getName(), count);
							if (f.createNewFile()) {
								writeTo(f,entity,monitor);
							}
							
						} catch (final IOException ioe) {
							new SUIJob() {
								@Override
								public void run() {
									MessageDialog.openWarning(getWindow().getShell(),
											"Warn", ioe.toString());
								}
							}.schedule();
						}
						monitor.done();
						return Status.OK_STATUS;
					}
				};
				job.setUser(true);
				job.schedule();
			}
			
			return false;
		}
		
	}
	
	public static class ExportAllLogs extends AbstractHandler {
		
		public static final String COMMAND_ID = "eu.jucy.gui.logviewer.exportalllogs";

		public Object execute(ExecutionEvent event) throws ExecutionException {
			
			String name = "";
			try {
				name = event.getCommand().getName();
			} catch (NotDefinedException e) {
				throw new ExecutionException(e.toString(),e);
			}
			
			DirectoryDialog dd = new DirectoryDialog(HandlerUtil.getActiveShell(event));
			dd.setMessage("Save where?");
			String dir = dd.open();
			if (dir != null) {
				final File fdir = new File(dir);
				if (!fdir.isDirectory() && !fdir.mkdirs()) {
					logger.warn("Could not create Dir: "+fdir);
				}
				final IDatabase db = ApplicationWorkbenchWindowAdvisor.get().getDatabase();
				Job job = new Job(name) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							int count = db.countLogentrys(null);
							monitor.beginTask("Save Logs", count);
							
							for (DBLogger entity: db.getLogentitys()) {
								File f = new File(fdir,GH.replaceInvalidFilename(entity.getName())+".txt");
								if (f.createNewFile()) {
									writeTo(f,entity,monitor);
								}
								if (monitor.isCanceled()) {
									break;
								}
							}
							
						} catch (final IOException ioe) {
							new SUIJob() {
								@Override
								public void run() {
									MessageDialog.openWarning(getWindow().getShell(),
											"Warn", ioe.toString());
								}
							}.schedule();
						}
						monitor.done();
						return Status.OK_STATUS;
					}
				};
			
				job.setUser(true);
				job.schedule();
								
			}
			return null;
		}
		
	}
	

	
	private static void writeTo(File f, DBLogger entity,IProgressMonitor monitor) throws IOException {
		entity.writeToFile(f, monitor);
	
//	//	int count = entity.countLogEntrys();
//		SimpleDateFormat sdf = new SimpleDateFormat(PI.get(PI.logTimeStamps));
//		monitor.subTask(f.getName());
//		PrintStream ps = null;
////		long readingFromDB= 0,writingToFile= 0;
//		List<Long> days = entity.getDays();
//		try {
//			ps = new PrintStream(f);
//			for (Long day:days) {
//				List<ILogEntry> logs = entity.loadLogEntrys(day, day+TimeUnit.DAYS.toMillis(1));
//				for (ILogEntry log:logs) {
//					ps.println(sdf.format(new Date(log.getDate())) +log.getMessage());
//				}
//				if (monitor.isCanceled()) {
//					break;
//				}
//				monitor.worked(logs.size());
//			}
////			int current = count;
////			do {
////				int toLoad = Math.min(current, 1000);
////				current -= toLoad;
////		//		long countA = System.currentTimeMillis();
////				List<ILogEntry> logs = entity.loadLogEntrys(toLoad, current);
////		//		long countB = System.currentTimeMillis();
////				Collections.reverse(logs);
////				for (ILogEntry log:logs) {
////					ps.println(sdf.format(new Date(log.getDate())) +log.getMessage());
////				}
////	//			long countC = System.currentTimeMillis();
////				if (monitor.isCanceled()) {
////					break;
////				}
////		//		readingFromDB+= countB-countA;
////		//		writingToFile += countC-countB;
////				monitor.worked(toLoad);
////			} while (current > 0);
//		} finally {
//			GH.close(ps);
//		}
//	//	logger.info("reading: "+readingFromDB +"  writing: "+writingToFile);
	}

}
