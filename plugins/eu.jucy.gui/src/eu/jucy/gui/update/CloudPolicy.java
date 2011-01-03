/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package eu.jucy.gui.update;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.Policy;





/**
 * CloudPolicy defines the RCP Cloud Example policies for the p2 UI. The policy
 * is registered as an OSGi service when the example bundle starts.
 * 
 * @since 3.5
 */
public class CloudPolicy extends Policy {
	
	//Group Ids used..
	
	private static final String PREFIX = "eu.jucy.feature.",POSTFIX = ".feature.group", ID_UNEQAL = "id != $0" ;
			
	
	public CloudPolicy() {
		//  User has no access to manipulate repositories
		//setRepositoryManipulator(null);
		setRepositoriesVisible(false);

		setRestartPolicy(Policy.RESTART_POLICY_PROMPT);
		// Default view is by category
		//IUViewQueryContext queryContext = new IUViewQueryContext(
		//		IUViewQueryContext.AVAILABLE_VIEW_BY_CATEGORY);
		
		setGroupByCategory(true);
		
		
		
		List<IQuery<IInstallableUnit>> notShown = new ArrayList<IQuery<IInstallableUnit>>();
		notShown.add(QueryUtil.createIUGroupQuery());
		
		for (String partId:new String[]{"baseclient","rcp.help","libraries","rcp.p2","rcp","rcp.help"}) {
			notShown.add(QueryUtil.createMatchQuery(ID_UNEQAL,PREFIX+partId+POSTFIX));
		}
		notShown.add(QueryUtil.createMatchQuery(ID_UNEQAL, "org.eclipse.equinox.executable.feature.group"));
		notShown.add(QueryUtil.createMatchQuery(ID_UNEQAL, "eu.jucy.product1.product"));
		
		IQuery<IInstallableUnit> complete = QueryUtil.createCompoundQuery(
				notShown
				, true);
		
		setVisibleInstalledIUQuery(complete);
		setVisibleAvailableIUQuery(complete);
		
		
	
		
		//setQueryContext(queryContext);
	
	}
	
	
}
