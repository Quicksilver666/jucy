/**
 * 
 */
package eu.jucy.gui.texteditor;

import helpers.SizeEnum;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.fieldassist.IContentProposal;

import uc.IUser;

public class UserContentProposal implements IContentProposal {
	
	private final IUser usr;
	
	public static IContentProposal[] create(Collection<? extends IUser> users) {
		ArrayList<IContentProposal> list = new ArrayList<IContentProposal>(users.size());
		for (IUser usr: users) {
			list.add(new UserContentProposal(usr));
		}
		return list.toArray(new IContentProposal[users.size()]);
	}
	
	public UserContentProposal(IUser usr) {
		this.usr = usr;

	}
	

	public String getContent() {
		return usr.getNick(); 
	}


	public int getCursorPosition() {
		return getContent().length();
	}


	public String getDescription() {
		//return SizeEnum.getReadableSize(usr.getShared());
		return null;
	}

	public String getLabel() {
		return String.format("%-20s   %10s  %30s", usr.getNick(),SizeEnum.getReadableSize(usr.getShared()),usr.getDescription());
	}


	
}