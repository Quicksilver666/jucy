package uc;

import java.util.Collection;

/**
 * Marks Objects that are associated with a user.
 * 
 * @author Quicksilver
 *
 */
public interface IHasUser {
	
	
	/**
	 * 
	 * @return the user associated with 
	 * this IHasUser object 
	 * used for actions ..
	 */
	IUser getUser();

	
	public static interface IMultiUser extends IHasUser {
		
		/**
		 * if an items holds multiple users..
		 * @return
		 */
		Collection<IUser> getIterable();
	}
}
