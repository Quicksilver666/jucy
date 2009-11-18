/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package eu.jucy.gui.update;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;

import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ElementQueryDescriptor;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import uihelpers.SUIJob;

import eu.jucy.gui.Activator;
import eu.jucy.gui.Lang;

/**
 * UpdateHandler invokes the check for updates UI
 * 
 * @since 3.4
 */
@SuppressWarnings("restriction")
public class UpdateHandler extends PreloadingRepositoryHandler {

	private static Logger logger = LoggerFactory.make();
	
	boolean hasNoRepos = false;

	/**
	 * The constructor.
	 */
	public UpdateHandler() {
		// constructor
	}

	
	protected void doExecute(String profileId, QueryableMetadataRepositoryManager manager) {
		if (hasNoRepos) {
			boolean goToSites = MessageDialog.openQuestion(getShell(), "No Updates Found", 
					"There are no update sites to search.  Do you wish" +
					" to open the \"Available Software Sites\" preferences?");
			
			if (goToSites) {
				Policy.getDefault().getRepositoryManipulator().manipulateRepositories(getShell());
			}
			return;
		}
		// get the profile roots
		ElementQueryDescriptor queryDescriptor = Policy.getDefault().getQueryProvider().getQueryDescriptor(new ProfileElement(null, profileId));
		Collection<?> collection = queryDescriptor.performQuery(null);
		final IInstallableUnit[] roots = new IInstallableUnit[collection.size()];
		Iterator<?> iter = collection.iterator();
		int i = 0;
		while (iter.hasNext()) {
			roots[i] = (IInstallableUnit) ProvUI.getAdapter(iter.next(), IInstallableUnit.class);
			i++;
		}
	
		// now create an update action whose selection is all the roots
		UpdateAction action = new UpdateAction(Policy.getDefault(), new ISelectionProvider() {

			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				// not dynamic
			}

			public ISelection getSelection() {
				return new StructuredSelection(roots);
			}

			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				// not dynamic
			}

			public void setSelection(ISelection selection) {
				// not mutable

			}
		}, profileId, false);
		action.setRepositoryManager(manager);
		action.run();
	}

	protected boolean preloadRepositories() {
		hasNoRepos = false;
		RepositoryManipulator repoMan = Policy.getDefault().getRepositoryManipulator();
		if (repoMan != null && repoMan.getKnownRepositories().length == 0) {
			hasNoRepos = true;
			return false;
		}
		return super.preloadRepositories();
	}
	
	public static void checkForUpdates() {
		// Before we show a progress dialog, at least find out that we have
		// installed content and repos to check.
		final IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper
				.getService(Activator.bundleContext, IProfileRegistry.class
						.getName());
		if (profileRegistry == null)
			return;
		logger.debug("UpdateHandler pos1");
		final IProfile profile = profileRegistry
				.getProfile(IProfileRegistry.SELF);
		if (profile == null)
			return;
		logger.debug("UpdateHandler pos2");
		// We are going to look for updates to all IU's in the profile. A
		// different query could be used if we are looking for updates to
		// a subset. For example, the p2 UI only looks for updates to those
		// IU's marked with a special property.
		final Collector collector = profile.query(InstallableUnitQuery.ANY,
				new Collector(), null);
		if (collector.isEmpty())
			return;
		logger.debug("UpdateHandler pos3");
		final IMetadataRepositoryManager metaManager = (IMetadataRepositoryManager) ServiceHelper
				.getService(Activator.bundleContext,
						IMetadataRepositoryManager.class.getName());
		if (metaManager == null)
			return;
		logger.debug("UpdateHandler pos5");
		final URI[] reposToSearch = metaManager
				.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		if (reposToSearch.length == 0)
			return;
		logger.debug("UpdateHandler pos6");
		final IPlanner planner = (IPlanner) ServiceHelper.getService(
				Activator.bundleContext, IPlanner.class.getName());
		if (planner == null) {
			logger.debug("planner null");
			return;
		}
		logger.debug("UpdateHandler pos7");
		// Looking in all known repositories for updates for each IU in the profile
	//	final boolean[] didWeUpdate = new boolean[1];
	//	didWeUpdate[0] = false;
		//IRunnableWithProgress runnable = new IRunnableWithProgress() {
			Job j=new Job("checkupdates") {
				public IStatus run(IProgressMonitor monitor) {
					try { 
				// We'll break progress up into 4 steps.
				// 1.  Load repos - it is not strictly necessary to do this.
				//     The planner will do it for us.  However, burying this
				//     in the planner's progress reporting will not 
				//     show enough progress initially, so we do it manually.
                // 2.  Get update list
				// 3.  Build a profile change request and get a provisioning plan
				// 4.  Perform the provisioning plan.
					SubMonitor sub = SubMonitor.convert(monitor,
						"Checking for application updates...", 400);
				// 1.  Load repos
					SubMonitor loadMonitor = sub.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS);
					for (int i=0; i<reposToSearch.length; i++)
						try {
							if (loadMonitor.isCanceled())
								return Status.CANCEL_STATUS;
							metaManager.loadRepository(reposToSearch[i], loadMonitor.newChild(100/reposToSearch.length));
						} catch (ProvisionException e) {
							e.printStackTrace();
						}
						loadMonitor.done();
				
				// 2.  Get update list.
				// First we look for replacement IU's for each IU
				ArrayList<IInstallableUnit> iusWithUpdates = new ArrayList<IInstallableUnit>();
				ArrayList<IInstallableUnit> replacementIUs = new ArrayList<IInstallableUnit>();
				Iterator<?> iter = collector.iterator();
				ProvisioningContext pc = new ProvisioningContext(reposToSearch);
				SubMonitor updateSearchMonitor = sub.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS);
				logger.debug("UpdateHandler pos8 "+collector.size());
				while (iter.hasNext()) {
					if (updateSearchMonitor.isCanceled()) {
						logger.debug("UpdateHandler cancel6");
						return Status.CANCEL_STATUS;
					}
					IInstallableUnit iu = (IInstallableUnit) iter.next();
					IInstallableUnit[] replacements = planner.updatesFor(iu,
							pc, updateSearchMonitor.newChild(100/collector.size()));
					if (replacements.length > 0) {
						iusWithUpdates.add(iu);
						if (replacements.length == 1)
							replacementIUs.add(replacements[0]);
						else {
							IInstallableUnit repl = replacements[0];
							for (int i = 1; i < replacements.length; i++)
								if (replacements[i].getVersion().compareTo(
										repl.getVersion()) > 0)
									repl = replacements[i];
							replacementIUs.add(repl);
						}
					}
				}

				// Did we find any updates?
				boolean updateFound = !iusWithUpdates.isEmpty();
				logger.debug("UpdateHandler pos9 "+updateFound);
				if (updateFound) {
					new SUIJob() {
						public void run() {
							IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
							MessageDialog.openInformation(window.getShell(),
									Lang.UpdateAvailable, Lang.UpdateAvailableMessage);
						}
					}.schedule();
				}
					} catch (Exception e) {
						logger.warn("end of updatecheck: "+e, e);
					}
					
				return Status.OK_STATUS;
				
			}
		};
		//j.setUser(true);
		j.schedule();
	
	//	return didWeUpdate[0];
	}
}
