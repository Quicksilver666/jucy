package eu.jucy.gui;



import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;


@SuppressWarnings("restriction")
public class CloudPolicy extends Policy {
	
	public CloudPolicy() {
		//  User has no access to manipulate repositories
		setRepositoryManipulator(null);
		
		//  Default view is by category
		IUViewQueryContext queryContext = new IUViewQueryContext(
				IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);

		setQueryContext(queryContext);
	}
	

}

