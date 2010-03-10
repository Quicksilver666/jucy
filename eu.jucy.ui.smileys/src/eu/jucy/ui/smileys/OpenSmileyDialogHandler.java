package eu.jucy.ui.smileys;


import java.util.Map.Entry;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.texteditor.UCTextEditor;

public class OpenSmileyDialogHandler extends AbstractHandler implements
		IHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		final Shell shell = new Shell(
				HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell(),
				SWT.SHELL_TRIM |SWT.TOOL);
		GridLayout rl = new GridLayout();
		rl.horizontalSpacing = 1;
		rl.verticalSpacing = 1;
		rl.numColumns = (int) Math.sqrt(SmileyTextModificator.getImages().size());
	
		shell.setLayout(rl);
		SmileyPoster sp = new SmileyPoster(shell);

		for (Entry<Integer,String> e: SmileyTextModificator.getSmileyToCorrespondingText()) {
			ToolBar toolBar = new ToolBar (shell, SWT.FLAT);
			toolBar.setLayoutData(new GridData());
			ToolItem item = new ToolItem (toolBar, SWT.PUSH);
			item.setData(e.getValue());
			item.setImage (SmileyTextModificator.getImages().get(e.getKey())[0]);
			item.addSelectionListener(sp);
		}
		
		shell.pack();
		shell.open();
	
		return null;
	}

	private static class SmileyPoster extends SelectionAdapter {
		private final Shell shell;
		
		private SmileyPoster(Shell shell){
			this.shell = shell;
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			String smiley = (String)(e.item != null? e.item.getData(): e.widget.getData());
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				
			IEditorPart part = window.getActivePage().getActiveEditor();
			if (part instanceof UCTextEditor) {
				((UCTextEditor)part).getWriteline().insert(" "+smiley+" ");
				shell.close();
			}
		}
	}
	
}
