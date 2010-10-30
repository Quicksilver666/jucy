package eu.jucy.gui.texteditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;

public class WrittenTextContentProposal extends TextContentAdapter implements IContentProposalProvider {

	private final SendingWriteline line;
	
	
	
	public WrittenTextContentProposal(SendingWriteline line) {
		super();
		this.line = line;
	}



	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		List<IContentProposal> props = new ArrayList<IContentProposal>();
		for (String s:line.getSentMessages()) {
			props.add(new StringContentProposal(s));
		}
		return props.toArray(new IContentProposal[0]);
	}
	
	private static class StringContentProposal implements IContentProposal {

		private final String s;
		
		
		public StringContentProposal(String s) {
			super();
			this.s = s;
		}

		@Override
		public String getContent() {
			return s;
		}

		@Override
		public int getCursorPosition() {
			return s.length();
		}

		@Override
		public String getLabel() {
			if (s.length() > 80) {
				return s.substring(0, 77)+"...";
			}
			return s;
		}

		@Override
		public String getDescription() {
			return null;
		}
		
	}
	
	

}
