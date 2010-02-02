package uihelpers;

import helpers.GH;
import helpers.PrefConverter;

import java.util.ArrayList;
import java.util.List;


import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

import uihelpers.TableViewerAdministrator.IColumnDescriptor;

public abstract class ComplexListEditor<V> extends FieldEditor {

	
	
	private Table list;

	private TableViewer tableViewer;
	
	private TableViewerAdministrator<V> tva;
	
	private final boolean changeAllowed;
	
	private List<V> items = new ArrayList<V>();
	
    /**
     * The button box containing the Add, Remove, Up, and Down buttons;
     * <code>null</code> if none (before creation or after disposal).
     */
    private Composite buttonBox;
    
    
    /**
     * The Add button.
     */
    private Button addButton;

    /**
     * The Remove button.
     */
    private Button removeButton;

    /**
     * The Up button.
     */
    private Button upButton;

    /**
     * The Down button.
     */
    private Button downButton;
    
    /**
     * The Change Button
     */
    private Button changeButton;
    
    /**
     * The selection listener.
     */
    private SelectionListener selectionListener;
    
    private final List<? extends IColumnDescriptor<V>> columns;
    
    
    private final IPrefSerializer<V> translater;
    
    private final boolean allowSorting;
    
    /**
     *  
     * 
     * @param titleText
     * @param prefID
     * @param columns
     * @param parent - if parent is null the ComplexListEditor is created with
     *  no change is allowed..
     */
	public ComplexListEditor(String titleText,String prefID,List<? extends IColumnDescriptor<V>> columns,
			Composite parent, IPrefSerializer<V> translater) {
		this(titleText,prefID,columns,parent,false,translater);
	}
	
	
	/**
	 * 
	 * 
	 * @param titleText - title Text
	 * @param prefID - the pref IS to which information will be stored
	 * @param columns - the columns used to create the table
	 * @param parent - parent widget
	 * @param changeAllowed - if a change Button should also be added
	 */
	public ComplexListEditor(String titleText,String prefID,List<? extends IColumnDescriptor<V>> columns,
			Composite parent, boolean changeAllowed, IPrefSerializer<V> translater) {
		this(titleText,prefID,columns,parent,changeAllowed,translater,true);
	}
	
	/**
	 * 
	 * 
	 * @param titleText - title Text
	 * @param prefID - the pref IS to which information will be stored
	 * @param columns - the columns used to create the table
	 * @param parent - parent widget
	 * @param changeAllowed - if a change Button should also be added
	 */
	public ComplexListEditor(String titleText,String prefID,List<? extends IColumnDescriptor<V>> columns,
			Composite parent, boolean changeAllowed, IPrefSerializer<V> translater, boolean allowSorting) {
		init(prefID, titleText);
		this.columns = columns;
		this.changeAllowed = changeAllowed;
		this.translater = translater;
		this.allowSorting = allowSorting;
		if (parent != null) {
			createControl(parent);
		}
	}
	
	
	
	@Override
	protected void adjustForNumColumns(int numColumns) {
        Control control = getLabelControl();
        ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
        ((GridData) list.getLayoutData()).horizontalSpan = numColumns - 1;
	}
	
	


	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        control.setLayoutData(gd);

        list = getListControl(parent);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        list.setLayoutData(gd);

        buttonBox = getButtonBoxControl(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        buttonBox.setLayoutData(gd);
		
	}
	
	  /**
     * Returns this field editor's list control.
     *
     * @param parent the parent control
     * @return the list control
     */
    public Table getListControl(Composite parent) {
        if (list == null) {
            list = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL
                    | SWT.H_SCROLL |SWT.FULL_SELECTION);
            list.setHeaderVisible(true);
            tableViewer = new TableViewer(list);
            tva = new TableViewerAdministrator<V>(tableViewer,columns,getPreferenceName(),Integer.MIN_VALUE,allowSorting);
            tva.apply();
            
            tableViewer.setContentProvider(new TableViewerProvider());
            
            list.setFont(parent.getFont());
            list.addSelectionListener(getSelectionListener());
            list.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    list = null;
                    tableViewer = null;
                }
            });
            
            
        } else {
            checkParent(list, parent);
        }
        return list;
    }
    
    /**
     * Returns this field editor's selection listener.
     * The listener is created if nessessary.
     *
     * @return the selection listener
     */
    private SelectionListener getSelectionListener() {
        if (selectionListener == null) {
			createSelectionListener();
		}
        return selectionListener;
    }
    
    /**
     * Creates a selection listener.
     */
    public void createSelectionListener() {
        selectionListener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                Widget widget = event.widget;
                if (widget == addButton) {
                    addPressed();
                } else if (widget == removeButton) {
                    removePressed();
                } else if (widget == upButton) {
                    upPressed();
                } else if (widget == downButton) {
                    downPressed();
                } else if (widget == list) {
                    selectionChanged();
                } else if (changeAllowed && widget == changeButton) {
                	changePressed();
                }
            }
        };
    }

    
    /**
     * Returns this field editor's button box containing the Add, Remove,
     * Up, and Down button.
     *
     * @param parent the parent control
     * @return the button box
     */
    public Composite getButtonBoxControl(Composite parent) {
        if (buttonBox == null) {
            buttonBox = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            buttonBox.setLayout(layout);
            createButtons(buttonBox);
            buttonBox.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    addButton = null;
                    removeButton = null;
                    upButton = null;
                    downButton = null;
                    buttonBox = null;
                    changeButton = null;
                }
            });

        } else {
            checkParent(buttonBox, parent);
        }

        selectionChanged();
        return buttonBox;
    }
    
    /**
     * Creates the Add, Remove, Up, and Down button in the given button box.
     *
     * @param box the box for the buttons
     */
    private void createButtons(Composite box) {
        addButton = createPushButton(box, "ListEditor.add");//$NON-NLS-1$
        removeButton = createPushButton(box, "ListEditor.remove");//$NON-NLS-1$
        upButton = createPushButton(box, "ListEditor.up");//$NON-NLS-1$
        downButton = createPushButton(box, "ListEditor.down");//$NON-NLS-1$
        if (changeAllowed) {
        	changeButton = createPushButton(box,"openChange"); //$NON-NLS-1$
        }
        
        
    }
    
    /**
     * Notifies that the Add button has been pressed.
     */
    private void addPressed() {
        setPresentsDefaultValue(false);
        V input = getNewInputObject();

        if (input != null && !items.contains(input)) {
            int index = list.getSelectionIndex();
            if (index >= 0) {
            	items.add(index+1, input);
			} else {
				items.add(0, input);
			}
            tableViewer.refresh();
            selectionChanged();
        }
    }
    

    
    
	@Override
	protected void doLoad() {
	    String s = getPreferenceStore().getString(getPreferenceName());
	    items = parseString(s,translater);
        tableViewer.setInput(items);
    }
		
	

	@Override
	protected void doLoadDefault() {
		if (list != null) {

			String s = getPreferenceStore().getDefaultString(
					getPreferenceName());
			items = parseString(s,translater);
			tableViewer.setInput(items);
		}
	}
	



	@Override
	public int getNumberOfControls() {
		return 2;
	}
	
	
    /**
     * Notifies that the list selection has changed.
     */
    private void selectionChanged() {

        int index = list.getSelectionIndex();
        int size = list.getItemCount();

        removeButton.setEnabled(index >= 0);
        upButton.setEnabled(size > 1 && index > 0);
        downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
        
        if (changeAllowed) {
        	changeButton.setEnabled(index >= 0);
        }
    }

    /**
     * Splits the given string into a list of strings.
     * This method is the converse of <code>createList</code>. 
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @param stringList the string
     * @return an array of <code>String</code>
     * @see #createList
     */
    public static <V> List<V> parseString(String stringList,IPrefSerializer<V> translater) {
    	List<V> list = new ArrayList<V>();
    	for (String s :  loadList(stringList)) {
    		list.add(translater.unSerialize(PrefConverter.asArray(s)));
    	}
    	return list;
    }
    
    
   // protected abstract V createItemFromString(String item);
    
    /**
     * just loads a List of item strings from the given string..
     * allows decoding 
     * 
     * @param stringList - the preference value that was stored before via complex List editor
     * @return
     */
    private static List<String> loadList(String stringList) {
    	if (stringList == null) {
    		stringList = "";
    	}
    	if (!stringList.endsWith("\n")&& !GH.isEmpty(stringList)) { //workaround for old ComplexListEditor so data can still be loaded..
    		stringList+= "\n";
    	}
    	
    	List<String> list = new ArrayList<String>();
    	int i = 0;
    	while ((i = stringList.indexOf('\n')) != -1) {
    		list.add(GH.revReplace(stringList.substring(0, i)));
    		stringList = stringList.substring(i+1);
    	}
    	return list;
    }

    /**
     * Notifies that the Up button has been pressed.
     */
    private void upPressed() {
        swap(true);
    }
    
    /**
     * Notifies that the Down button has been pressed.
     */
    private void downPressed() {
        swap(false);
    }
    
    /**
     * Moves the currently selected item up or down.
     *
     * @param up <code>true</code> if the item should move up,
     *  and <code>false</code> if it should move down
     */
    @SuppressWarnings("unchecked")
	private void swap(boolean up) {
        setPresentsDefaultValue(false);
        int index = list.getSelectionIndex();
        int target = up ? index - 1 : index + 1;

        if (index >= 0) {
        	StructuredSelection sel = (StructuredSelection)tableViewer.getSelection();
        	V selection=(V)sel.getFirstElement();
            items.remove(index);
            items.add(target,selection);
            tableViewer.refresh();
            tableViewer.setSelection(new StructuredSelection(selection));
        }
        selectionChanged();
    }
    
    /**
     * Notifies that the Remove button has been pressed.
     */
    private void removePressed() {
        setPresentsDefaultValue(false);
        int index = list.getSelectionIndex();
        if (index >= 0) {
            items.remove(index);
            tableViewer.refresh();
            selectionChanged();
        }
    }
    
    private void changePressed() {
    	setPresentsDefaultValue(false);
    	 int index = list.getSelectionIndex();
         if (index >= 0) {
        	 V v = items.get(index);
        	 changeInputObject(v);
             tableViewer.refresh();
         }
    }
    
    
    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    protected void doStore() {
        String s = createList(items, translater);
        if (s != null) {
			getPreferenceStore().setValue(getPreferenceName(), s);
		}
    }
    
    public static <V> String createList(List<V> items,IPrefSerializer<V> translater) {
    	StringBuilder s = new StringBuilder();
    	for (V v: items) {
    		String item = PrefConverter.asString(translater.serialize(v));
    		s.append( GH.replaces(item)).append('\n');
    	}
    	return s.toString();
    }
    
    
    //protected abstract String createStringFromItem(V v);
    
    
    /**
     * Helper method to create a push button.
     * 
     * @param parent the parent control
     * @param key the resource name used to supply the button's label text
     * @return Button
     */
    private Button createPushButton(Composite parent, String key) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(JFaceResources.getString(key));
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        int widthHint = convertHorizontalDLUsToPixels(button,
                IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT,
                SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.addSelectionListener(getSelectionListener());
        return button;
    }
	
    /*
     * @see FieldEditor.setEnabled(boolean,Composite).
     */
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        getListControl(parent).setEnabled(enabled);
        addButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        upButton.setEnabled(enabled);
        downButton.setEnabled(enabled);
    }
    
    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    public void setFocus() {
        if (list != null) {
            list.setFocus();
        }
    }


    /**
     * Creates and returns a new item for the list.
     * <p>
     * Subclasses must implement this method.
     * </p>
     *
     * @return a new item - or null if user aborted input..
     */
    protected abstract V getNewInputObject();
    
    
    /**
     * requests changing of some input Item
     * @param v - the item that needs to be changed...
     */
    protected void changeInputObject(V v) {}
    
    
    class TableViewerProvider implements IStructuredContentProvider {
	
		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			return ((List<?>)inputElement).toArray();
		}
    	
    }

    
    /**
     * mediates between  preferences and Objects they presenting 
     * 
     * @author Quicksilver
     *
     * @param <T> the object needing serialisation
     */
    public static interface IPrefSerializer<T> {
    	
    	T unSerialize(String[] all);
    	
    	String[] serialize(T t);
    	
    }
    
}
