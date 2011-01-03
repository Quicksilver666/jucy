package eu.jucy.gui.itemhandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import eu.jucy.gui.Priority;



import uc.IUser;
import uc.crypto.HashValue;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.DownloadQueueFolder;
import uc.files.downloadqueue.FileListDQE;

public abstract class DownloadQueueHandlers extends AbstractHandler {

	private static final Logger logger =  LoggerFactory.make();
	
	private final boolean allowFilelists;
	
	protected DownloadQueueHandlers() {
		this(true);
	}
	
	protected DownloadQueueHandlers(boolean allowFilelists) {
		this.allowFilelists = allowFilelists;
	}
	
	
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		run(getSelected(HandlerUtil.getCurrentSelectionChecked(event),allowFilelists),event);
		return null;
	}
	
	public static List<AbstractDownloadQueueEntry> getSelected(ISelection sel,boolean allowFilelist) {
		ArrayList<AbstractDownloadQueueEntry> all = new ArrayList<AbstractDownloadQueueEntry>();
		IStructuredSelection selection = (IStructuredSelection) sel;
		
		for (Object o: selection.toArray()) {
			if (o instanceof AbstractDownloadQueueEntry) {
				AbstractDownloadQueueEntry adqe = (AbstractDownloadQueueEntry)o;
				if (allowFilelist || !(adqe instanceof FileListDQE)) {
					all.add(adqe);
				}
			} else if (o instanceof DownloadQueueFolder) {
				for (AbstractDownloadQueueEntry adqe : ((DownloadQueueFolder)o).getAllDQEChildren()) {
					if (allowFilelist || !(adqe instanceof FileListDQE)) {
						all.add(adqe);
					}
				}
			}
		}
		return all;
	}
	
	protected abstract void run(List<AbstractDownloadQueueEntry> dqe,ExecutionEvent event) throws ExecutionException;

	
	public static class DQMoveRenameHandler extends DownloadQueueHandlers {
		
		public DQMoveRenameHandler() {
			super(false);
		}

		protected File getTargetDir(File initial,ExecutionEvent event) throws ExecutionException  {
			DirectoryDialog dd= new DirectoryDialog(HandlerUtil.getActiveShellChecked(event));
			dd.setFilterPath(initial.getPath());
			dd.setMessage(Lang.MoveRename); 
			String target = dd.open();
			if (target == null) {
				return null;
			} else {
				return new File(target);
			}
		}	
		
		@Override
		protected void run(final List<AbstractDownloadQueueEntry> dqe,ExecutionEvent event) throws ExecutionException {
			if (dqe.size() == 1) {
				AbstractDownloadQueueEntry single = dqe.get(0);
				FileDialog fd = new FileDialog(HandlerUtil.getActiveShellChecked(event),SWT.SAVE); 
			//	fd.setFileName(single.getFileName());
			
				File path = single.getTargetPath();

				fd.setFilterPath(path.getParent());
				fd.setFileName(path.getName());
				
				
				String target = fd.open();
				if (target != null) {
					single.setTargetPath(new File(target));
				}
			} else {
				//retrieves a common parent for all folders.. then changes all paths..
				final File common = AbstractDownloadQueueEntry.getCommonParent(dqe);
				if (common != null) {
					final File target = getTargetDir(common,event);
					if (target != null) {
						ApplicationWorkbenchWindowAdvisor.get().executeDir(new Runnable() {
							public void run() {
								for (AbstractDownloadQueueEntry adqe : dqe) {
									String oldpath = adqe.getTargetPath()
											.getPath();
									String newTarget = oldpath.replace(common
											.getPath(), target.getPath());
									adqe.setTargetPath(new File(newTarget));
								}
							}
						});
					}
				}
			}
		}
		
		
		
	}

	/**
	 * moves a set of files to the given targetFolder ..
	 * 
	 * @param target
	 * @param dqes
	 * @param overrideCommon  null if move should just use the longest common parent path of the dqes
	 * otherwise the common will be replaced ..
	 */
	public static void move(DownloadQueueFolder folder,final List<AbstractDownloadQueueEntry> dqes ,File overrideCommon) {
		final File common = overrideCommon == null? 
					AbstractDownloadQueueEntry.getCommonParent(dqes):
					overrideCommon;
					
		final File target = folder.getShownPath();
		ApplicationWorkbenchWindowAdvisor.get().executeDir(new Runnable() {
			public void run() {
				for (AbstractDownloadQueueEntry adqe : dqes) {
					String oldpath = adqe.getTargetPath()
							.getPath();
					String newTarget = oldpath.replace(common.getPath(), target.getPath());
					
					logger.debug("New TargetPath: "+newTarget); 
					adqe.setTargetPath(new File(newTarget)); 
				}
			}
		});
	}
	
	

	
	
	

	

	/**
	 * removes an File from the DownloadQueue -> done by a  Downloadable Handler..
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class DQRemoveHandler extends DownloadQueueHandlers {
		
		public static final String COMMAND_ID = "eu.jucy.gui.Remove";
		
		@Override
		protected void run(final List<AbstractDownloadQueueEntry> dqe,ExecutionEvent event) {
			ApplicationWorkbenchWindowAdvisor.get().executeDir(new Runnable() {
				public void run() {
					for (AbstractDownloadQueueEntry adqe : dqe) {
						adqe.remove();
					}
				}
			});
		}
	} 
	
	/**
	 * Adds users  to a file that have already been deleted once..
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class ReaddSourceHandler extends DownloadQueueHandlers {
		public static final String COMMAND_ID= "eu.jucy.gui.dqReaddSource";
		@Override
		protected void run(List<AbstractDownloadQueueEntry> dqes,ExecutionEvent event) {
			for (AbstractDownloadQueueEntry dqe: dqes) {
				String userid = event.getParameter(UserHandlers.USER_BY_ID);

				for (IUser usr: dqe.getRemovedUsers()) {
					if (userid == null || usr.getUserid().equals(HashValue.createHash(userid))) {
						dqe.addUser(usr);
					}
				}
			}
		}
	}
	
	/**
	 * removes a user from a file
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class RemoveSourceHandler extends DownloadQueueHandlers {
		public static final String COMMAND_ID = "eu.jucy.gui.dqRemoveSource";
		@Override
		protected void run(List<AbstractDownloadQueueEntry> dqes,ExecutionEvent event) {
			for (AbstractDownloadQueueEntry dqe: dqes) {
				String userid = event.getParameter(UserHandlers.USER_BY_ID);

				for (IUser usr: dqe.getUsers()) {
					if (userid == null || usr.getUserid().equals(HashValue.createHash(userid))) {
						dqe.removeUser(usr);
					}
				}
			}
		}
	}
	
	/**
	 * Actions sets a priority to a DownloadQueue
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class SetPriorityHandler extends  DownloadQueueHandlers {
		public static final String PRIORITY = "PRIORITY";


		@Override
		protected void run(List<AbstractDownloadQueueEntry> dqe,ExecutionEvent event) {
			Priority p = Priority.valueOf(event.getParameter(PRIORITY));
			for (AbstractDownloadQueueEntry adqe:dqe) {
				adqe.setPriority(p.getDefaultValue());
			}
		}
	}
	

	public static class ChangePriorityUpHandler extends DownloadQueueHandlers {
		
		private final boolean up;
		

		public ChangePriorityUpHandler() {
			this(true);
		}
		private ChangePriorityUpHandler(boolean up) {
			this.up = up;
		}

		@Override
		protected void run(List<AbstractDownloadQueueEntry> dqe,ExecutionEvent event) {
			for (AbstractDownloadQueueEntry adqe:dqe) {
				int i = adqe.getPriority()+ (up?1:-1);
				adqe.setPriority(i);
			}
		}
	}
	
	public static class ChangePriorityDownHandler extends ChangePriorityUpHandler {
		public ChangePriorityDownHandler() {
			super(false);
		}
	}
	
	
//	
//	public abstract static class ToggleDoAfterHandler extends DownloadQueueHandlers {
//		public static final String COMMAND_ID= "eu.jucy.gui.dqtoggledoafter";
//		public static final String PARAM_TOGGLEID  = "TOGGLE_ID";
//		
//		
//		
//		@Override
//		protected void run(List<AbstractDownloadQueueEntry> dqes,ExecutionEvent event) throws ExecutionException {
//			String toggle =  event.getParameter(PARAM_TOGGLEID);
//			for (AbstractDownloadQueueEntry dqe: dqes) {
//				for (AbstractDownloadFinished adf:dqe.getDoAfterDownload()) {
//					if (toggle.equals(adf.getId())) {
//						adf.setExecute(!adf.isExecute());
//						HandlerUtil.updateRadioState(event.getCommand(), ""+adf.isExecute());
//					}
//				}
//			}
//		}
//	}
	
}
