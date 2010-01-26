package eu.jucy.gui.statusline;



import java.util.ArrayList;
import java.util.List;

import logger.LoggerFactory;



import org.apache.log4j.Logger;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.swt.SWT;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;





import eu.jucy.gui.GUIPI;

public class StatusComposite extends ContributionItem {

	private static Logger logger = LoggerFactory.make();
	
	public static final String ID = "eu.jucy.gui.statusline.comp";

	public StatusComposite() {
		super(ID);
	}
	
	@Override
	public void fill(final Composite parent) {
		
		final List<IStatusLineComp> labels = new ArrayList<IStatusLineComp>();
		final Composite comp = new Composite(parent,SWT.NONE);

		RowLayout rl = new RowLayout();
		rl.wrap = false;
		rl.marginBottom = 0;
		rl.marginTop = 0;
		
		comp.setLayout(rl);
		
		if (GUIPI.getBoolean(GUIPI.shareSizeContrib)) {
			SharesizeLabel sz = new SharesizeLabel(comp);
			labels.add(sz);
		}
		
		if (GUIPI.getBoolean(GUIPI.hubsContrib)) {
			HubsLabel hl = new HubsLabel(comp);
			labels.add(hl);
		}
		
		if (GUIPI.getBoolean(GUIPI.slotsContrib)){
			SlotsLabel sl = new SlotsLabel(comp);
			labels.add(sl);
		}
		
		Point px  = comp.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		if (GUIPI.getBoolean(GUIPI.connectionStatusContrib)) {
			ConnectionStatus cs = new ConnectionStatus(comp,px.y - 1); //px.y -1
			cs.setLayoutData(new RowData(SWT.DEFAULT,SWT.DEFAULT));
		}

		if (GUIPI.getBoolean(GUIPI.downContrib)) {
			TotalTransferredLabel ttl = new TotalTransferredLabel(comp,false);
			labels.add(ttl);
		}

		if (GUIPI.getBoolean(GUIPI.upContrib)) {
			TotalTransferredLabel ttl = new TotalTransferredLabel(comp,true);
			labels.add(ttl);
		}

		if (GUIPI.getBoolean(GUIPI.downSpeedContrib)) {
			TotalSpeedLabel tsl = new TotalSpeedLabel(comp,false);
			labels.add(tsl);
		}
		
		if (GUIPI.getBoolean(GUIPI.upSpeedContrib)) {
			TotalSpeedLabel tsl = new TotalSpeedLabel(comp,true);
			labels.add(tsl);
		}

		for (IStatusLineComp sc: labels) {
			RowData rd = new RowData(SWT.DEFAULT,SWT.DEFAULT);
			Composite c = (Composite)sc;
			GC gc = new GC(c);
			int cWidth = gc.getFontMetrics().getAverageCharWidth();
			gc.dispose();
			rd.width = (int)(cWidth * sc.getNumberOfCharacters()*1.1d); // 10% security margin..
			c.setLayoutData(rd);
		}
		
		StatusLineLayoutData stl = new StatusLineLayoutData();
		Point p = comp.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		stl.widthHint = p.x;
		stl.heightHint = p.y;
		comp.setLayoutData(stl);
		logger.debug("PX: "+px+"  P:"+p);
	}

}
