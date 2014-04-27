package eu.jucy.gui.settings;

import helpers.GH;
import helpers.PrefConverter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import eu.jucy.gui.Application;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;

import uc.Command;
import uc.IHub;
import uc.Command.CommandTranslater;
import uihelpers.ComplexListEditor;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public class UserCommands extends UCPrefpage {


	
	public UserCommands() {
		super(Application.PLUGIN_ID);
	}
	
	@Override
	protected void createFieldEditors() {
		UserCommandsFieldEditor ucfe= new UserCommandsFieldEditor(
				Lang.UserCommands,
				GUIPI.userCommands,
				getFieldEditorParent());
		addField(ucfe);
	}
	
	public static List<Command> loadCommands() {
		return PrefConverter.parseString(
				GUIPI.get(GUIPI.userCommands), 
				new CommandTranslater());
	}
	
	/**
	 * convenience method to get all comamnds for on place
	 * mix them with the hub provided commands and filter them 
	 * 
	 * @param hub -applicable hub(s)
	 * @param multiUsers - if multiple possible receivers exist
	 * @param where - where argument in Command
	 * @return all commands that match hub as well as "where"
	 */
	public static List<Command> loadCommandAndAddHubCommnds(Collection<IHub> hubsExist,Collection<IHub> hubsAllAreIn,boolean multiUsers,int where) {
		List<Command> list = loadCommands();
		for (IHub in: hubsAllAreIn) {
			list.addAll(in.getUserCommands());
		}

		
		for (Iterator<Command> it = list.iterator(); it.hasNext();) {
			Command com = it.next();
			boolean remove = !com.isSeparator() && multiUsers && !com.isAllowMulti();
			
			if (hubsExist.isEmpty()) {
				remove = remove || !com.matches(where);
				remove = remove || !GH.isEmpty(com.getHub());
			} else {
				boolean matches = false;
				for (IHub hub:hubsAllAreIn) {
					matches = matches || com.matches(where, hub);
				}
				if (!matches) { 
					for (IHub hub : hubsExist) {
						remove = remove || !com.matches(where, hub);
					}
				} 
			}
			
			if (remove) {
				it.remove();
			} 
		}
	
		
		return list;
	}
	
	public static void storeCommands(List<Command> coms) {
		String s = PrefConverter.createList(coms, new CommandTranslater());
		GUIPI.put(GUIPI.userCommands, s);
	}
	
	
	public static class UserCommandsFieldEditor extends ComplexListEditor<Command> {

		
		
		public UserCommandsFieldEditor(String titleText, String prefID,Composite parent) {
			super(titleText, prefID, Arrays.asList(new NameCol(),new SentCommandCol(),new HubColumn()), parent,true, new CommandTranslater(),false);
		}


		@Override
		protected Command getNewInputObject() {
			UserCommandDialog ucd= new UserCommandDialog(getPage().getShell());
			ucd.setBlockOnOpen(true);
			if (ucd.open() == Dialog.OK) {
				return ucd.getCommand();
			}
			return null;
		}
		
		@Override
		protected void changeInputObject(Command v) {
			UserCommandDialog ucd = new UserCommandDialog(getPage().getShell(),v);
			ucd.setBlockOnOpen(true);
			ucd.open(); 
		}
		
		
	}
	

	
	public static class NameCol extends ColumnDescriptor<Command> {

		public NameCol() {
			super(200, Lang.Name, SWT.LEAD);
		}

		@Override
		public Image getImage(Command x) {
			return null;
		}

		@Override
		public String getText(Command x) {
			if (x.isSeparator()) {
				return x.getPath()+"/---------------";
			}
			return x.getPath();
		}
		
	}
	
	public static class SentCommandCol extends ColumnDescriptor<Command> {

		public SentCommandCol() {
			super(400, Lang.Command, SWT.LEAD);
		}

		@Override
		public Image getImage(Command x) {
			return null;
		}

		@Override
		public String getText(Command x) {
			if (x.isSeparator()) {
				return "---------------";
			}
			return x.getCommand();
		}
		
	}
	public static class HubColumn extends ColumnDescriptor<Command> {

		public HubColumn() {
			super(200, Lang.Hub, SWT.LEAD);
		}

		@Override
		public Image getImage(Command x) {
			return null;
		}

		@Override
		public String getText(Command x) {
			return x.getHub();
		}
		
	}

}
