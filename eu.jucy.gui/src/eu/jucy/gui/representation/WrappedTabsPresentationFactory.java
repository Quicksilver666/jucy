/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package eu.jucy.gui.representation;




import org.eclipse.swt.widgets.Composite;



import org.eclipse.ui.presentations.AbstractPresentationFactory;
import org.eclipse.ui.presentations.IStackPresentationSite;
import org.eclipse.ui.presentations.StackPresentation;
import org.eclipse.ui.presentations.WorkbenchPresentationFactory;

import eu.jucy.gui.GUIPI;


/**
 * Presentation for jucy ..
 * depending on setting editor presentation is changed to Wrapped tabs presentation
 * or the normal Presentation
 * 
 */
public class WrappedTabsPresentationFactory extends AbstractPresentationFactory {


	/*
	 * changed added workbecn representation .. for the other stuff...
	 */
	private final WorkbenchPresentationFactory wpf = new WorkbenchPresentationFactory();
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.presentations.AbstractPresentationFactory#createEditorPresentation(org.eclipse.swt.widgets.Composite, org.eclipse.ui.presentations.IStackPresentationSite)
	 */
	public StackPresentation createEditorPresentation(Composite parent,
			IStackPresentationSite site) {
		//
		if (GUIPI.getBoolean(GUIPI.alternativePresentation)) {
			return new WrappedTabsPartPresentation(parent, site, true);
		} else {
			return DefaultPartPresentation.createEditorPresentation(parent, site);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.presentations.AbstractPresentationFactory#createViewPresentation(org.eclipse.swt.widgets.Composite, org.eclipse.ui.presentations.IStackPresentationSite)
	 */
	public StackPresentation createViewPresentation(Composite parent,
			IStackPresentationSite site) {
		
		return wpf.createViewPresentation(parent, site);
		//return new WrappedTabsPartPresentation(parent, site, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.presentations.AbstractPresentationFactory#createStandaloneViewPresentation(org.eclipse.swt.widgets.Composite, org.eclipse.ui.presentations.IStackPresentationSite, boolean)
	 */
	public StackPresentation createStandaloneViewPresentation(Composite parent,
			IStackPresentationSite site, boolean showTitle) {
		return wpf.createStandaloneViewPresentation(parent, site, showTitle);
		//return new WrappedTabsPartPresentation(parent, site, false);
	}

}
