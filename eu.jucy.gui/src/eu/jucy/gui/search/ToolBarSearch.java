package eu.jucy.gui.search;





import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import eu.jucy.gui.Lang;


public class ToolBarSearch extends WorkbenchWindowControlContribution {

	private static final String ID = "eu.jucy.gui.search.ToolBarSearch";
	
	
	public ToolBarSearch() {
		super(ID);
	}

	@Override
	protected Control createControl(Composite parent) {
		Composite comp  = new Composite(parent,SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginHeight = 2;
		gl.marginWidth = 2;
		
//		gl.verticalSpacing = 0;
//		gl.horizontalSpacing = 0;
		comp.setLayout(gl);
		
		final Text searchText = new Text(comp, SWT.SEARCH|SWT.ICON_SEARCH);
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		gd.minimumWidth = 120;
		searchText.setLayoutData(gd);
		searchText.setMessage(String.format("%-25s", Lang.EnterSearch));
		searchText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				String search = searchText.getText().trim();
				//check for minimum length on enter..
				if((e.keyCode  == SWT.KEYPAD_CR ||   e.keyCode  == SWT.CR) && search.length() > 2  ) {
					e.doit = false;
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					OpenSearchEditorHandler.openSearchEditor(window, search);
				}
			}
		});
		searchText.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				if (e.detail == SWT.ICON_SEARCH) {
					OpenSearchEditorHandler.openSearchEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							, null);
				}
			}
		});
		comp.pack();
		
		return comp;
	}

}
