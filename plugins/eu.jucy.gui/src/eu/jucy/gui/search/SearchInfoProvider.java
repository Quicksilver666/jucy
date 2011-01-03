package eu.jucy.gui.search;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

class SearchInfoProvider extends TextContentAdapter implements IContentProposalProvider {
	public IContentProposal[] getProposals(String contents, int position) {
		return SearchEditor.PAST_SEARCHES.toArray(new IContentProposal[]{});
	}
	/**
	 * modified insert function .. 
	 */
	@Override
	public void insertControlContents(Control control, String text,int cursorPosition) {
		Text t = (Text)control;
		t.setText(text);
		t.setSelection(text.length(), text.length());
	}
}