package eu.jucy.gui.search;

import org.eclipse.jface.fieldassist.IContentProposal;

import uc.files.search.SearchType;

class SearchInfo  implements IContentProposal {
	
	private final String searchString;
	private final int results ;
	private final int files;
	private final SearchType st;
	
	public SearchInfo(String searchString,int results,int files,SearchType st) {
		this.searchString = searchString;
		this.results = results;
		this.files = files;
		this.st = st;
	}
	
	public String getContent() {
		return searchString;
	}

	public int getCursorPosition() {
		return getContent().length();
	}

	public String getLabel() {
		return String.format("Search: %-45s Results: %-7d Files: %-7d Type: %s", "\""+searchString+"\"",results,files,st);
	}

	public String getDescription() {
		return null;
	}
}