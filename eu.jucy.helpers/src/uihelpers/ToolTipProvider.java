package uihelpers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import org.eclipse.swt.widgets.Listener;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public abstract class ToolTipProvider<X> {

	private final Table table;

	public ToolTipProvider(Table t) {
		this.table = t;
		addListener();
	}
	
	private void addListener() {
	/*	final Listener labelListener = new Listener() {
			public void handleEvent(Event event) {
				Label label = (Label) event.widget;
				Shell shell = label.getShell();
				switch (event.type) {
				case SWT.MouseDown:
					Event e = new Event();
					e.item = (TableItem) label.getData("_TABLEITEM");
					// Assuming table is single select, set the selection as if
					// the mouse down event went through to the table
					table.setSelection(new TableItem[] { (TableItem) e.item });
					table.notifyListeners(SWT.Selection, e);
					// fall through
				case SWT.MouseExit:
					shell.dispose();
					break;
				}
			}
		}; */

		Listener tableListener = new Listener() {
		//	Shell tip = null;

		//	Label label = null;

			@SuppressWarnings("unchecked")
			public void handleEvent(Event event) {
				switch (event.type) {
			/*	case SWT.Dispose:
				case SWT.KeyDown:
				case SWT.MouseMove: {
			//		table.setToolTipText(null);
			//		if (tip == null)
			//			break;
			//		tip.dispose();
			//		tip = null;
			//		label = null;
					
					break; 
				}*/
				case SWT.MouseHover: {
					TableItem item = table.getItem(new Point(event.x, event.y));
					if (item != null) {
					//	if (tip != null && !tip.isDisposed()) {
					//		tip.dispose();
					//	}
						
					/*	tip = new Shell(table.getShell(), SWT.ON_TOP | SWT.TOOL);
						tip.setLayout(new FillLayout());
						label = new Label(tip, SWT.NONE);
						label.setForeground(Display.getDefault()
								.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
						label.setBackground(Display.getDefault()
								.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
						label.setData("_TABLEITEM", item);
						*/
						X x = (X)item.getData();
						
					/*	label.setText(getToolTip(x));
						label.addListener(SWT.MouseExit, labelListener);
						label.addListener(SWT.MouseDown, labelListener);
						Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
						Rectangle rect = item.getBounds(0);
						Point pt = table.toDisplay(rect.x, rect.y);
						tip.setBounds(event.x+pt.x- size.x/8, pt.y+35, size.x, size.y);
						tip.setVisible(true); */
						table.setToolTipText( getToolTip(x) );
					} else {
						table.setToolTipText(null);
					}
				}
				}
			}
		};
	//	table.addListener(SWT.Dispose, tableListener);
	//	table.addListener(SWT.KeyDown, tableListener);
	//	table.addListener(SWT.MouseMove, tableListener);
		table.addListener(SWT.MouseHover, tableListener);
	}
	
	protected abstract String getToolTip(X x);
	
}
