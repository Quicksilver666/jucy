package eu.jucy.gui.representation;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.presentations.defaultpresentation.DefaultMultiTabListener;
import org.eclipse.ui.internal.presentations.defaultpresentation.DefaultSimpleTabListener;
import org.eclipse.ui.internal.presentations.defaultpresentation.DefaultTabFolder;
import org.eclipse.ui.internal.presentations.defaultpresentation.DefaultThemeListener;
import org.eclipse.ui.internal.presentations.util.PresentablePartFolder;
import org.eclipse.ui.internal.presentations.util.TabbedStackPresentation;
import org.eclipse.ui.presentations.IStackPresentationSite;
import org.eclipse.ui.presentations.StackPresentation;

import eu.jucy.gui.representation.UCEditorContributionItem.UCEditorSystemMenu;

@SuppressWarnings("restriction")
public class DefaultPartPresentation {

	private static int editorTabPosition = PlatformUI.getPreferenceStore()
	.getInt(IWorkbenchPreferenceConstants.EDITOR_TAB_POSITION);
	
	
	/**
	 * creates the normal Eclipse editor Presentation with the only difference of a changed systemMenu
	 */
	public static StackPresentation createEditorPresentation(Composite parent,
			IStackPresentationSite site) {
		DefaultTabFolder folder = new DefaultTabFolder(parent,
				editorTabPosition | SWT.BORDER, site
						.supportsState(IStackPresentationSite.STATE_MINIMIZED),
				site.supportsState(IStackPresentationSite.STATE_MAXIMIZED));

		/*
		 * Set the minimum characters to display, if the preference is something
		 * other than the default. This is mainly intended for RCP applications
		 * or for expert users (i.e., via the plug-in customization file).
		 * 
		 * Bug 32789.
		 */
		final IPreferenceStore store = PlatformUI.getPreferenceStore();
		if (store
				.contains(IWorkbenchPreferenceConstants.EDITOR_MINIMUM_CHARACTERS)) {
			final int minimumCharacters = store
					.getInt(IWorkbenchPreferenceConstants.EDITOR_MINIMUM_CHARACTERS);
			if (minimumCharacters >= 0) {
				folder.setMinimumCharacters(minimumCharacters);
			}
		}

		PresentablePartFolder partFolder = new PresentablePartFolder(folder);

		TabbedStackPresentation result = new TabbedStackPresentation(site,
				partFolder,  new UCEditorSystemMenu());  //new StandardEditorSystemMenu(site));

		DefaultThemeListener themeListener = new DefaultThemeListener(folder,
				result.getTheme());
		result.getTheme().addListener(themeListener);

		new DefaultMultiTabListener(result.getApiPreferences(),
				IWorkbenchPreferenceConstants.SHOW_MULTIPLE_EDITOR_TABS, folder);

		new DefaultSimpleTabListener(result.getApiPreferences(),
				IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS,
				folder);

		return result;
	}
	
}
