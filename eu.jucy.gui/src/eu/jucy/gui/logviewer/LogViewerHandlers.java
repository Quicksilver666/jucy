package eu.jucy.gui.logviewer;

import helpers.GH;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import eu.jucy.gui.itemhandler.DownloadableHandlers.RemoveDownloadableFromQueueHandler;


import uc.DCClient;
import uc.PI;
import uc.database.DBLogger;
import uc.database.IDatabase;
import uc.database.ILogEntry;
import uihelpers.SUIJob;

public abstract class LogViewerHandlers extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event);
		if (!selection.isEmpty()) {
			run((DBLogger)selection.getFirstElement(),event);
			
			IEditorPart part = HandlerUtil.getActiveEditor(event);
			if (part instanceof LogViewerEditor) {
				((LogViewerEditor)part).update(true);
			}
			
		}
		return null;
	}
	
	public abstract void run(DBLogger entity,ExecutionEvent event);
	
	public static class DeleteDBLogger extends  LogViewerHandlers {
		public static final String COMMAND_ID = RemoveDownloadableFromQueueHandler.COMMAND_ID;

		@Override
		public void run(final DBLogger entity,ExecutionEvent event) {
			final LogViewerEditor part =(LogViewerEditor)HandlerUtil.getActiveEditor(event);
			part.setFilter(entity);
			
			DCClient.execute(new Runnable() {
				public void run() {
					entity.deleteEntity();
					new SUIJob() {
						@Override
						public void run() {
							part.removeFilter(entity);
							part.update(true);
						}
					}.schedule();
				}
			});
		}
	}
	
	public static class ExportLog extends LogViewerHandlers {
		public static final String COMMAND_ID = "eu.jucy.gui.logviewer.exportlogs";

		@Override
		public void run(final DBLogger entity,ExecutionEvent event) {
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
				if (!fdir.isDirectory()) {
					fdir.mkdirs();
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
		int count = entity.countLogEntrys();
		SimpleDateFormat sdf = new SimpleDateFormat(PI.get(PI.logTimeStamps));
		monitor.subTask(f.getName());
		PrintStream ps = new PrintStream(f);
		try {
			int current = count;
			do {
				int toLoad = Math.min(current, 1000);
				current -= toLoad;
				
				List<ILogEntry> logs = entity.loadLogEntrys(toLoad, current);
				Collections.reverse(logs);
				for (ILogEntry log:logs) {
					ps.println(sdf.format(new Date(log.getDate())) +log.getMessage());
				}
				if (monitor.isCanceled()) {
					break;
				}
				monitor.worked(toLoad);
			} while (current > 0);
		} finally {
			ps.close();
		}
	}

}
