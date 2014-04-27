package eu.jucy.gui.settings;


import helpers.GH;

import java.io.File;
import java.util.Arrays;



import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;


import eu.jucy.gui.Lang;
import uc.FavFolders.FavDir;
import uc.FavFolders.FavDirTranslater;
import uihelpers.ComplexListEditor;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public class FavoriteDirsFieldEditor extends ComplexListEditor<FavDir> {

	
	

	public FavoriteDirsFieldEditor(String titleText, String prefID, Composite parent) {
		super(titleText, prefID, Arrays.asList(new FavDirNameColumn(),new FavDirsPathColumn()),parent, new FavDirTranslater());
	}



	@Override
	protected FavDir getNewInputObject() {
		
		DirectoryDialog dd = new DirectoryDialog(getPage().getShell());
		dd.setText(Lang.ChooseFolder);
		dd.setMessage(Lang.ChooseFolder);
		String folder = dd.open();
		if (folder == null) {
			return null;
		}
		File f = new File(folder);
		
		InputDialog input = new InputDialog(getPage().getShell(),
				Lang.VirtualName,
				Lang.VirtualName,
				f.getName(),new IInputValidator() {
	
					public String isValid(String newText) {
						if (GH.isEmpty(newText)) {
							return "";
						} else {
							return null;
						}
					}
		});
		input.setBlockOnOpen(true);
		String vname = null;
		if (input.open() == InputDialog.OK) {
			vname = input.getValue();
			if (vname != null && !GH.isEmpty(vname)) {
				return new FavDir(vname,f);
			}
		}
		
		return null;
	}


	public static class FavDirNameColumn extends ColumnDescriptor<FavDir> {

		public FavDirNameColumn() {
			super(100, Lang.VirtualName);
		}

		@Override
		public Image getImage(FavDir x) {
			return null;
		}

		@Override
		public String getText(FavDir x) {
			return x.getName();
		}
	}
	
	public static class FavDirsPathColumn extends ColumnDescriptor<FavDir> {
		
		public FavDirsPathColumn() {
			super(300, Lang.Directory);
		}

		@Override
		public Image getImage(FavDir x) {
			return null;
		}

		@Override
		public String getText(FavDir x) {
			return x.getDirectory().getPath();
		}
		
	}


}
