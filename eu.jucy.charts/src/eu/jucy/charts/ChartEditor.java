package eu.jucy.charts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import eu.jucy.gui.UCEditor;

public class ChartEditor extends UCEditor {

	private Composite composite;
	
	private Canvas canvas;
	
	@Override
	public void createPartControl(Composite parent) {
		final GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		parent.setLayout(gridLayout);

		composite = new Composite(parent, SWT.NONE);
		final GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false);
		composite.setLayoutData(gridData);
		
		canvas = new Canvas(parent,SWT.DOUBLE_BUFFERED);
		canvas.addPaintListener(new PaintListener() {
		      public void paintControl(PaintEvent e) {
		        // Do some drawing
		        Rectangle rect = ((Canvas) e.widget).getBounds();
		        e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
		        e.gc.drawFocus(5, 5, rect.width - 10, rect.height - 10);
		        e.gc.drawText("You can draw text directly on a canvas", 60, 60);
		      }
		    });
		final GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		canvas.setLayoutData(gridData2);
		

	}
	
	private void draw(GC gc, DataElement from, DataElement to, float maxValue,long oldest) {
		
	}

	@Override
	public void setFocus() {
		canvas.setFocus();
	}

}
