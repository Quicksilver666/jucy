package eu.jucy.gui.settings;

import helpers.SizeEnum;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.osgi.service.prefs.Preferences;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import uc.PI;
import uc.FavFolders.SharedDir;

public class SharedDirsFieldEditor extends FieldEditor {

	public static final Logger logger = LoggerFactory.make();
	


private Label totalSizeLabel;
private Button sharehiddenfiles;
private Spinner slotsSpinner;
private Button addbutton;
private Button removebutton;
private Button renamebutton;
private Table table;


	@Override
	protected void adjustForNumColumns(int numColumns) {
	}

	@Override
	protected void doFillIntoGrid(final Composite parent, int numColumns) {
		final Composite comp = new Composite(parent,SWT.NONE);
		GridData gd = new GridData(SWT.FILL,SWT.FILL,true,true);
		gd.horizontalSpan = numColumns ;
		
		comp.setLayoutData(gd);
		final GridLayout gridLayout_6 = new GridLayout();
		gridLayout_6.numColumns = 2;
		comp.setLayout(gridLayout_6);

		final Group shareddirectorysGroup = new Group(comp, SWT.NONE);
		shareddirectorysGroup.setText(Lang.SharedDirectorys);
		shareddirectorysGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		shareddirectorysGroup.setLayout(new GridLayout());

		table = new Table(shareddirectorysGroup, SWT.FULL_SELECTION | SWT.BORDER);
		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
			
				if (-1!=  table.getSelectionIndex()) {
					removebutton.setEnabled(true);
					renamebutton.setEnabled(true);
				} else {
					removebutton.setEnabled(false);
					renamebutton.setEnabled(false);
				}
					
			}
		});
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		

		final TableColumn newColumnTableColumn = new TableColumn(table, SWT.NONE);
		newColumnTableColumn.setWidth(80);
		newColumnTableColumn.setText(Lang.VirtualName);

		final TableColumn newColumnTableColumn_1 = new TableColumn(table, SWT.NONE);
		newColumnTableColumn_1.setWidth(139);
		newColumnTableColumn_1.setText(Lang.Directory);

		final TableColumn newColumnTableColumn_2 = new TableColumn(table, SWT.NONE);
		newColumnTableColumn_2.setWidth(78);
		newColumnTableColumn_2.setText(Lang.Size);

		final Composite composite_5 = new Composite(shareddirectorysGroup, SWT.NONE);
		composite_5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final GridLayout gridLayout_7 = new GridLayout();
		gridLayout_7.numColumns = 4;
		composite_5.setLayout(gridLayout_7);

		sharehiddenfiles = new Button(composite_5, SWT.CHECK);
		sharehiddenfiles.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false,4,1));
		sharehiddenfiles.setText(Lang.ShareHiddenFiles);
	
		
	//	new Label(composite_5, SWT.NONE);
	//	new Label(composite_5, SWT.NONE);
	//	new Label(composite_5, SWT.NONE);

		totalSizeLabel = new Label(composite_5, SWT.NONE);
		totalSizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	//	totalSizeLabel.setText(String.format(Lang.TotalSize,"0 B"));

	//	totalSizeLabel = new Label(composite_5, SWT.NONE);
	//	totalSizeLabel.setText("0 B"); 
	//	totalSizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		renamebutton = new Button(composite_5, SWT.NONE);
		renamebutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int i = table.getSelectionIndex();
				if (-1!= i ) {
					TableItem ti= table.getItem(i);
					SharedDir sd= (SharedDir)ti.getData();
					//get a new name from the user
					InputDialog input = new InputDialog(parent.getShell(),
							Lang.VirtualName,
							Lang.NameUnderWhichTheOthersSeeTheDirectory,
							sd.getName(),null);
					
					input.setBlockOnOpen(true);
					
				//	StringDialog strdiag= new StringDialog(parent.getShell(),SWT.NONE, sd.getName(),,  );
					String res = null; //strdiag.open();
					
					if (input.open() == InputDialog.OK) {
						res = input.getValue();
						
					
						for (TableItem test:table.getItems()) {  //check is something has the same name
							SharedDir testsd =(SharedDir)test.getData();
							
							if (testsd.getName().equals(res) && testsd != sd) { //another item has the same virtualname therefore we return
								return;
							}
							
						}
	
						sd.setName(res);
						setItem(ti,sd);
					} 
				}
			}
		});
		renamebutton.setEnabled(false);
		renamebutton.setLayoutData(new GridData(100, SWT.DEFAULT));
		renamebutton.setText(Lang.Rename);

		removebutton = new Button(composite_5, SWT.NONE);
		removebutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int i = table.getSelectionIndex();
				if (-1 != i  ) {
					table.getItem(i).dispose();
				}
			}
		}); 
		removebutton.setEnabled(false);
		removebutton.setLayoutData(new GridData(100, SWT.DEFAULT));
		removebutton.setText(Lang.Remove);

		addbutton = new Button(composite_5, SWT.NONE);
		addbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(parent.getShell());
				dd.setText(Lang.ChooseFolder);
				dd.setMessage(Lang.ChooseFolder);
				String folder = dd.open();
				if (folder == null) {
					return;
				}
				File f = new File(folder);
				
				InputDialog input = new InputDialog(parent.getShell(),
						Lang.VirtualName,
						Lang.NameUnderWhichTheOthersSeeTheDirectory,
						f.getName(),null);
				input.setBlockOnOpen(true);
				String vname = null;
				if (input.open() == InputDialog.OK) {
					vname = input.getValue();
					
//					for (TableItem tab : table.getItems()){ //check is something has the same name
//						SharedDir sd= (SharedDir)tab.getData();
//						if ( sd.getName().equals(vname)) { //another item has the same virtualname therefore we return
//							return;
//						}
//					}
//					//ok legal path and filename..  No more checking.. duplicate names are allowed..
					
					TableItem ti = new TableItem(table, SWT.NONE );
					SharedDir sharedDir=new SharedDir(vname,f);
					setItem(ti,sharedDir);

				}
			}
		});
		addbutton.setLayoutData(new GridData(100, SWT.DEFAULT));
		addbutton.setText(Lang.AddFolder);

		final Label label_10 = new Label(comp, SWT.NONE);
		label_10.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		label_10.setText(Lang.UploadSlots);

		slotsSpinner = new Spinner( comp , SWT.BORDER);
		slotsSpinner.setMinimum(1);
		slotsSpinner.setMaximum(99);

	}
	


	private static void setItem(TableItem ti,SharedDir dir ){
		ti.setData(dir);
		ti.setText(new String[] {dir.getName() , dir.getDirectory().getPath(), SizeEnum.getReadableSize(dir.getLastShared()) } );
	}
	
	@Override
	protected void doLoad() {
		long total=0;
		Preferences pref= uc.PI.get();
	
		Collection<SharedDir> shareddirs = ApplicationWorkbenchWindowAdvisor.get()
												.getFavFolders().getSharedDirs();
			
		for(SharedDir sharedDir: shareddirs){
			final TableItem ti = new TableItem(table,SWT.NONE);
			setItem(ti,sharedDir);
			total+=sharedDir.getLastShared();
		}
			
		//load slots
		slotsSpinner.setSelection( pref.getInt(PI.slots, 2));
		
		//load sharehidden
		sharehiddenfiles.setSelection(pref.getBoolean(PI.shareHiddenFiles, false));
	
		//set the totalsize label appropriately	
		totalSizeLabel.setText(String.format(Lang.TotalSize,SizeEnum.getReadableSize(total )));	
		
	}
	

	@Override
	protected void doLoadDefault() {
		slotsSpinner.setSelection( getPreferenceStore().getDefaultInt(PI.slots));
		sharehiddenfiles.setSelection(getPreferenceStore().getDefaultBoolean(PI.shareHiddenFiles));
	}

	@Override
	protected void doStore() {
		getPreferenceStore().setValue(PI.slots, slotsSpinner.getSelection());
		getPreferenceStore().setValue(PI.shareHiddenFiles, sharehiddenfiles.getSelection());
		
		ApplicationWorkbenchWindowAdvisor.get().getFavFolders().storeSharedDirs(getSharedDirs());
	}
	
	private List<SharedDir> getSharedDirs() {
		List<SharedDir> dirs = new ArrayList<SharedDir>();
		for (TableItem ti : table.getItems()) {
			SharedDir sd= (SharedDir)ti.getData();
			dirs.add(sd);
		}
		return dirs;
	}

	@Override
	public int getNumberOfControls() {
		return 1;
	}

}
