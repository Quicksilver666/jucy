package eu.jucy.gui.itemhandler;

import helpers.GH;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;


import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.ReplaceLine;

import eu.jucy.gui.itemhandler.DownloadQueueHandlers.DQRemoveHandler;
import eu.jucy.gui.search.OpenSearchEditorHandler;

import uc.DCClient;
import uc.IUser;
import uc.files.IDownloadable;
import uc.files.IHasDownloadable;
import uc.files.MagnetLink;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.DownloadQueue;
import uc.files.downloadqueue.AbstractDownloadQueueEntry.IDownloadFinished;
import uc.protocols.SendContext;


public abstract class DownloadableHandlers extends AbstractHandler {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	public static final String TargetPath = "TargetPath";
	
	private final boolean allowFolder;
	protected DownloadableHandlers() {
		this(true);
	}
	
	protected DownloadableHandlers(boolean allowFolder) {
		this.allowFolder = allowFolder;
	}
	
	
	public Object execute(ExecutionEvent event) throws ExecutionException {

		List<IDownloadable> files = new ArrayList<IDownloadable>();
		for (Object item: ((StructuredSelection)HandlerUtil.getCurrentSelectionChecked(event)).toArray()) {
			IDownloadable it = ((IHasDownloadable)item).getDownloadable();
			if (it != null && (allowFolder || it.isFile())) {
				files.add((IDownloadable)it);
			} 
			
		}
		if (!files.isEmpty()) {
			run(files,event);
		}
		return null;
	}
	
	
	protected abstract void run(List<IDownloadable> files,ExecutionEvent event);
	
	
	public static class DownloadHandler extends DownloadableHandlers {
		
		public static final String ID = "eu.jucy.gui.downloadable.download";
		
		@Override
		protected void run(final List<IDownloadable> files,ExecutionEvent event) {
			DCClient.execute(new Runnable() {
				public void run() {
					for (IDownloadable f : files) {
						f.download();
					}
				}
			});
		}
	}
	
	/**
	 * 
	 * downloads the selected into a chosen directory
	 * @author Quicksilver
	 *
	 */
	public static class DownloadBrowseHandler extends DownloadableHandlers {
		
		protected IWorkbenchWindow window;
		
		protected File getTargetDir() {
			DirectoryDialog dd= new DirectoryDialog(window.getShell());
			String target = dd.open();
			if (target == null) {
				return null;
			} else {
				return new File(target);
			}
		}	
		
		@Override
		protected void run(final List<IDownloadable> files,ExecutionEvent event) {
			DownloadQueue dq = ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue();
			window = HandlerUtil.getActiveWorkbenchWindow(event);
			if (files.size() == 1 && files.get(0).isFile()) {
				FileDialog fd = new FileDialog(window.getShell(),SWT.SAVE); 
				fd.setFileName(files.get(0).getName());
				String target = fd.open();
				if (target != null) {
					final File targetF = new File(target);
					dq.addPathForRecommendation(targetF.getParentFile());
					DCClient.execute(new Runnable() {
						public void run() {
							files.get(0).download(targetF);
						}
					});
				}
			} else {
				final File targetdir = getTargetDir();
				if (targetdir != null) {
					dq.addPathForRecommendation(targetdir);
					DCClient.execute(new Runnable() {
						public void run() {
							for (IDownloadable file:files) {
								file.download(new File(targetdir,file.getName()));
							}
						}
					});
				}
			}
		}
	}
	
	public static class DownloadToRecommendedPath extends DownloadableHandlers {
		
		public static final String ID = "eu.jucy.gui.downloadable.downloadtorecommendedpath";
		
		
		@Override
		protected void run(final List<IDownloadable> files,ExecutionEvent event) {
			
			final File target = new File(event.getParameter(TargetPath));
			logger.debug("DownloadTorecpath: "+ target);
			DCClient.execute(new Runnable() {
				public void run() {
					for (IDownloadable downloadable:files) {
						downloadable.download(target);
					}
				}
			});
		}
	}
	
	public static class DownloadToRecommendedDir extends DownloadableHandlers {
		
		public static final String ID = "eu.jucy.gui.downloadable.downloadtorecommendeddir";
	
		
		@Override
		protected void run(final List<IDownloadable> files,ExecutionEvent event) {
			
			final File target = new File(event.getParameter(TargetPath));
			logger.debug("DownloadTorecdir: "+ target);
			DCClient.execute(new Runnable() {
				public void run() {
					for (IDownloadable downloadable:files) {
						//new File(getTargetDir(),downloadable.getName())
						downloadable.download(new File(target,downloadable.getName()));
					}
				}
			});
		}
	}
	
	public static class ExecuteAfterDownloadHandler extends DownloadableHandlers {
		
		@Override
		protected void run(final List<IDownloadable> files,ExecutionEvent event) {
			DCClient.execute(new Runnable() {
				public void run() {
					for (IDownloadable f: files) {
						AbstractDownloadQueueEntry adqe = f.download();
						if (adqe != null) {
							adqe.addDoAfterDownload(			
								new IDownloadFinished() {//implement  hash code and equals methods so only one execution can be set
		
									public int hashCode() {
										return getClass().hashCode();
									};
									
									public boolean equals(Object o) { 
										return o != null && getClass().equals(o.getClass());
									}
									
									public void finishedDownload(File f) {
										int i = f.getName().lastIndexOf('.');
										if (i != -1) {
											Program p = Program.findProgram(f.getName().substring(i));
											
											if (p != null) {
												p.execute(f.getPath());
											}
										}
									}
									
								}
							);		
						}
						
					}
				}
			});
		}
	}
	
	public static class SearchForAlternatesHandler extends DownloadableHandlers {

		@Override
		protected void run(List<IDownloadable> files,ExecutionEvent event) {
			IDownloadableFile idf = (IDownloadableFile)files.get(0);
			OpenSearchEditorHandler.openSearchEditor(HandlerUtil.getActiveWorkbenchWindow(event), 
					idf.getTTHRoot().toString());

		}
	}
	
	public static class CopyTTHToClipboardHandler extends DownloadableHandlers {


		@Override
		protected void run(List<IDownloadable> files,ExecutionEvent event) {
			IDownloadableFile idf = (IDownloadableFile)files.get(0);
			GuiHelpers.copyTextToClipboard(idf.getTTHRoot().toString());
		}
	}
	
	public static class CopyMagnetToClipboardHandler extends DownloadableHandlers  {

		@Override
		protected void run(List<IDownloadable> files,ExecutionEvent event) {
			IDownloadableFile idf = (IDownloadableFile)files.get(0);
			String magnetLink = new MagnetLink(idf).toString();
			GuiHelpers.copyTextToClipboard(magnetLink);
		}
	}
	
	
	public static class RemoveDownloadableFromQueueHandler extends DownloadableHandlers  {

		public static final String COMMAND_ID = DQRemoveHandler.COMMAND_ID;
		
		@Override
		protected void run(List<IDownloadable> files,ExecutionEvent event) {
			IDownloadableFile idf = (IDownloadableFile)files.get(0);
			AbstractDownloadQueueEntry adqe = ApplicationWorkbenchWindowAdvisor.get()
												.getDownloadQueue().get(idf.getTTHRoot());
			if (adqe != null) {
				adqe.remove();
			}
		}
	}
	
	public static class DownloadableUserCommandHandler extends DownloadableHandlers  {

	//	public static final String ID = "eu.jucy.gui.downloadable.usercommand";
		
		@Override
		protected void run(List<IDownloadable> files,ExecutionEvent event) {
			String command = event.getParameter(UCContributionItem.SEND); 
			command = ReplaceLine.get().replaceLines(command);
			if (!GH.isNullOrEmpty(command)) {
				for (IDownloadable f:files) {
					for (IUser usr:f.getIterable()) {
						usr.getHub().sendRaw(command, new SendContext(f,usr));
					}
				}
			}
		}
	}
	

}
