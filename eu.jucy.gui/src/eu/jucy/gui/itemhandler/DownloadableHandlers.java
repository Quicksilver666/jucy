package eu.jucy.gui.itemhandler;

import helpers.GH;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;


import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.Lang;
import eu.jucy.gui.ReplaceLine;

import eu.jucy.gui.itemhandler.DownloadQueueHandlers.DQRemoveHandler;
import eu.jucy.gui.search.OpenSearchEditorHandler;

import uc.DCClient;
import uc.IUser;
import uc.PI;
import uc.files.IDownloadable;
import uc.files.IHasDownloadable;
import uc.files.MagnetLink;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.DownloadQueue;
import uc.files.downloadqueue.AbstractDownloadFinished;
import uc.files.filelist.FileList;
import uc.files.filelist.FileListDescriptor;
import uc.files.filelist.FileListFolder;
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
	
	
	protected abstract void run(List<IDownloadable> files,ExecutionEvent event) throws ExecutionException ;
	
	
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
		
		protected File getTargetDir(ExecutionEvent event) {
			return new File(event.getParameter(TargetPath));
		}
		
		@Override
		protected void run(final List<IDownloadable> files,ExecutionEvent event) {
			final File target = getTargetDir(event);
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
						downloadable.download(new File(target,downloadable.getName()));
					}
				}
			});
		}
	}
	
	public static class DownloadParentdDir extends DownloadableHandlers {
		
		public static final String ID = "eu.jucy.gui.downloadable.downloadparentdir";
	
		
		@Override
		protected void run(final List<IDownloadable> files,final ExecutionEvent event) {
			final IDownloadable id = files.get(0);
			final IUser usr = id.getUser();
			
			FileListDescriptor fd = usr.getFilelistDescriptor();
			if (fd != null) {
				downloadDirOf(id, fd.getFilelist(),event);
			} else {
				usr.downloadFilelist().addDoAfterDownload(new AbstractDownloadFinished() { 
					public void finishedDownload(File f) { 
						downloadDirOf(id,usr.getFilelistDescriptor().getFilelist(),event);	
					}
				}); 
			}
		}
		
		private void downloadDirOf(IDownloadable id,FileList fl,ExecutionEvent event) {
			String target = event.getParameter(TargetPath);
			
			FileListFolder flf =  fl.getRoot().getByPath(id.getOnlyPath());
			if (flf != null) {
				if (target == null) {
					flf.download();
				} else {
					flf.download( new File(target,flf.getName()));
				}
			}
		}
	}
	
	public static class DownloadParentdDirBrowse extends DownloadableHandlers {
		
		public static final String ID = "eu.jucy.gui.downloadable.downloadparentdirbrowse";
	
		protected File getTargetDir(ExecutionEvent event) {
			DirectoryDialog dd = new DirectoryDialog(HandlerUtil.getActiveShell(event));
			String target = dd.open();
			if (target == null) {
				return null;
			} else {
				return new File(target);
			}
		}	
		
		@Override
		protected void run(List<IDownloadable> files,final ExecutionEvent event) {
			final IDownloadable id = files.get(0);
			final IUser usr = id.getUser();

			final File target = getTargetDir(event);
			
		
			if (target != null) {
				if (usr.hasDownloadedFilelist()) {
					downloadDirOf(id,target);
				} else {
					usr.downloadFilelist().addDoAfterDownload(new AbstractDownloadFinished() { 
						public void finishedDownload(File f) { 
							downloadDirOf(id,target);	
						}
					}); 
				}
			}
		}
		
		private void downloadDirOf(IDownloadable id,File target) {
			FileList fl = id.getUser().getFilelistDescriptor().getFilelist();
			FileListFolder flf =  fl.getRoot().getByPath(id.getOnlyPath());
			if (flf != null && target != null) {
				flf.download( target );
			}
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
							adqe.addDoAfterDownload(new AbstractDownloadFinished() {
								public void finishedDownload(File f) {
									int i = f.getName().lastIndexOf('.');
									if (i != -1) {
										Program p = Program.findProgram(f.getName().substring(i));
										
										if (p != null) {
											p.execute(f.getPath());
										}
									}
								}
								
								public String showToUser() {
									return Lang.ExecuteAfterDownload;
								}
							});		
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
	
	public static class OpenDirectoryOfDownloadableHandler extends DownloadableHandlers {
		@Override
		protected void run(List<IDownloadable> files,ExecutionEvent event) {
			IDownloadableFile idf = (IDownloadableFile)files.get(0);
			DCClient dcc = ApplicationWorkbenchWindowAdvisor.get();
			File folderToShow = null;
			AbstractDownloadQueueEntry adqe = dcc.getDownloadQueue().get(idf.getTTHRoot());
			if (adqe != null && adqe.getTargetPath() != null) {
				folderToShow = adqe.getTargetPath().getParentFile();
			} else {
				File flf = dcc.getFilelist().getFile(idf.getTTHRoot());
				if (flf != null) {
					folderToShow = flf.getParentFile();
				}
			}
			if (folderToShow != null) {
				Program.launch(folderToShow.getPath());
			}
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
			List<MagnetLink> allMagnets = new ArrayList<MagnetLink>();
			for (IDownloadable file:files) {
				IDownloadableFile idf = (IDownloadableFile)file;
				allMagnets.add(new MagnetLink(idf));
			}
			
			GuiHelpers.copyTextToClipboard(GH.concat(allMagnets, "\n", ""));
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
			Map<String,String> reps = ReplaceLine.get().replaceLines(command);
			if (!GH.isNullOrEmpty(command)  && reps != null) {
				for (IDownloadable f:files) {
					for (IUser usr:f.getIterable()) {
						usr.getHub().sendRaw(command, new SendContext(f,usr,reps));
					}
				}
			}
		}
	}
	
	public static class ShowPreviewHandler extends DownloadableHandlers  {
		
		@Override
		protected void run(List<IDownloadable> files,ExecutionEvent event) throws ExecutionException {
			IDownloadableFile idf = (IDownloadableFile)files.get(0);
			AbstractDownloadQueueEntry adqe = ApplicationWorkbenchWindowAdvisor.get()
												.getDownloadQueue().get(idf.getTTHRoot());
			
			String playerpath = PI.get(PI.previewPlayerPath);
			if (GH.isNullOrEmpty(playerpath) || !new File(playerpath).isFile()) {
				MessageDialog.openInformation(HandlerUtil.getActiveShellChecked(event)
						, "Information", "Path for preview util not set: Preferences -> Misc");
			} else if (adqe != null) {
				try {
					Runtime.getRuntime().exec(new String[] {
							PI.get(PI.previewPlayerPath)
							,adqe.getTempPath().toString()
							});
					
				} catch (IOException e) {
					MessageDialog.openError(HandlerUtil.getActiveShellChecked(event)
							, "error", "Problem executing preview: "+e);
				}
			}
		}
	}


	
}
