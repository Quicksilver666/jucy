package eu.jucy.gui;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import uc.DCClient;
import uihelpers.SUIJob;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "eu.jucy.gui.perspective";
	
	private static Logger logger = LoggerFactory.make();
	
	private ApplicationWorkbenchWindowAdvisor myAdvisor;

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return myAdvisor = new ApplicationWorkbenchWindowAdvisor(configurer);
    }

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}
	
	@Override
	public void initialize(final IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		//on new version .. forget WorkbenchLayout ...
		
		boolean save = GUIPI.get(GUIPI.lastStartupVersion).equals(DCClient.VERSION);
		
		configurer.setSaveAndRestore(save);
		if (!save) {
			GUIPI.put(GUIPI.lastStartupVersion, DCClient.VERSION);
			new SUIJob() {
				@Override
				public void run() {
					configurer.setSaveAndRestore(true);
				}
			}.schedule(10000);
			versionChanged();
		}
		configurer.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {

			public void postShutdown(IWorkbench workbench) {}

			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				if (!forced && GUIPI.getBoolean(GUIPI.askBeforeShutdown)) {
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					return MessageDialog.openQuestion(shell, Lang.Question, 
							Lang.AreYouSureYouWantToCloseJucy);
				}
				return true;
			}
			
		});
		
	}
	
	
	@Override
	public void eventLoopException(Throwable exception) {
		if (Platform.inDevelopmentMode()) {
			logger.warn(exception, exception);
		}
		super.eventLoopException(exception);
	}

	private void versionChanged() {
		LoggerFactory.clearErrorLog();
	}
	

	@Override
	public void postShutdown() {
		ApplicationWorkbenchWindowAdvisor.get().stop(true);
		myAdvisor.disposeTray();
		super.postShutdown();
	}
	
}
