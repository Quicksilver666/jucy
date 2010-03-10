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
		Listener tableListener = new Listener() {
			@SuppressWarnings("unchecked")
			public void handleEvent(Event event) {
				if (event.type == SWT.MouseHover) {
					TableItem item = table.getItem(new Point(event.x, event.y));
					if (item != null) {
						X x = (X)item.getData();
	
						table.setToolTipText( getToolTip(x) );
					} else {
						table.setToolTipText(null);
					}
				}
			}
		};
		table.addListener(SWT.MouseHover, tableListener);
	}
	
	protected abstract String getToolTip(X x);
	
}
