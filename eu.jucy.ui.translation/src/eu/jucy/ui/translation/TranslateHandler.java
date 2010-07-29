package eu.jucy.ui.translation;

import java.io.IOException;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import uihelpers.SUIJob;


import com.google.api.translate.Translate;

import eu.jucy.gui.texteditor.UCTextEditor;

public class TranslateHandler extends AbstractHandler implements IHandler {

	private static final Logger logger = LoggerFactory.make();
	
	static {
		Translate.setHttpReferrer("http://jucy.eu");
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			IStructuredSelection ssel = (IStructuredSelection)sel;
			final String s = ssel.getFirstElement().toString();
			final UCTextEditor ucte = (UCTextEditor)HandlerUtil.getActiveEditorChecked(event);
			new Job(Lang.Translate) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						final String translated = translate(s);
						new SUIJob() {
							@Override
							public void run() {
								ucte.replaceSelectedText(translated, s);
							}
						}.schedule();
					} catch(IOException ioe) {
						logger.warn(ioe,ioe);
					}
					return Status.OK_STATUS;
				}
			}.schedule();
			
		}
		
		logger.debug("Sel: "+sel.toString() +"  "+sel.isEmpty()+ "   "+sel.getClass().getName());
		
		return null;
	}

	private static String translate(String text) throws IOException {
		try {
			return Translate.execute(text
					, TransPI.getLang(TransPI.sourceLanguage)
					, TransPI.getLang(TransPI.targetLanguage));
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
}
