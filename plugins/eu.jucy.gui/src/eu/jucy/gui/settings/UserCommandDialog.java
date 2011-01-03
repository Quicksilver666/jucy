package eu.jucy.gui.settings;

import helpers.GH;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import eu.jucy.gui.Lang;
import uc.Command;

public class UserCommandDialog extends TrayDialog {

	private Button separatorButton;
	private Text hubText;
	private Button commandButton;
	private Button sendOnceForButton;
	private Button filelistMenuButton;
	private Button userMenuButton;
	private Button searchMenuButton;
	private Button hubMenuButton;
	private Text commandText;
	private Text nameText;
	
	
	
	private final Command command;
	
	/**
	 * create shell with new UserCommand..
	 * 
	 * @param parentShell
	 */
	public UserCommandDialog(Shell parentShell) {
		this(parentShell,new Command());
	}
	
	public UserCommandDialog(Shell parentShell,Command com) {
		super(parentShell);
		this.command = com;
		setHelpAvailable(true);
	}

	public UserCommandDialog(IShellProvider parentShell, Command com) {
		this(parentShell.getShell(),com);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent.getShell().setText(Lang.UserCommand);
		
        // create composite
        Composite composite = (Composite) super.createDialogArea(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,"eu.jucy.gui.help.UserCommands");
        
        final GridLayout gridLayout = new GridLayout();
        composite.setLayout(gridLayout);

        final Group commandTypeGroup = new Group(composite, SWT.NONE);
        commandTypeGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        commandTypeGroup.setText(Lang.Type);
        final GridLayout gridLayout_1 = new GridLayout();
        gridLayout_1.numColumns = 2;
        commandTypeGroup.setLayout(gridLayout_1);

        separatorButton = new Button(commandTypeGroup, SWT.RADIO);
        separatorButton.setText(Lang.Separator);

        commandButton = new Button(commandTypeGroup, SWT.RADIO);
        commandButton.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(final SelectionEvent e) {
        		boolean enabled = commandButton.getSelection();
        		commandText.setEnabled(enabled);
        		sendOnceForButton.setEnabled(enabled);
        	}
        });
        commandButton.setText(Lang.Command);

        final Group contextGroup = new Group(composite, SWT.NONE);
        contextGroup.setText(Lang.Context);
        contextGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        final GridLayout gridLayout_2 = new GridLayout();
        gridLayout_2.numColumns = 2;
        contextGroup.setLayout(gridLayout_2);

        hubMenuButton = new Button(contextGroup, SWT.CHECK);
        hubMenuButton.setText(Lang.HubMenu);

        searchMenuButton = new Button(contextGroup, SWT.CHECK);
        searchMenuButton.setText(Lang.SearchMenu);

        userMenuButton = new Button(contextGroup, SWT.CHECK);
        userMenuButton.setText(Lang.UserMenu);

        filelistMenuButton = new Button(contextGroup, SWT.CHECK);
        filelistMenuButton.setText(Lang.FileListMenu);

        final Group parameterGroup = new Group(composite, SWT.NONE);
        parameterGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        parameterGroup.setText(Lang.Parameter);
        parameterGroup.setLayout(new GridLayout());

        final Label nameLabel = new Label(parameterGroup, SWT.NONE);
        nameLabel.setText(Lang.Name);

        nameText = new Text(parameterGroup, SWT.BORDER);
        nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Label commandLabel = new Label(parameterGroup, SWT.NONE);
        commandLabel.setText(Lang.Command);

        commandText = new Text(parameterGroup, SWT.BORDER |SWT.MULTI);
        commandText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Label hubLabel = new Label(parameterGroup, SWT.NONE);
        hubLabel.setText(Lang.HubExplaCommands);//"Hub (\"\"=for all, \"op\"=all hubs where op, url to specify hub)");

        hubText = new Text(parameterGroup, SWT.BORDER);
        hubText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        sendOnceForButton = new Button(parameterGroup, SWT.CHECK);
        sendOnceForButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        sendOnceForButton.setText(Lang.SendOnceForEachUser);
        
        setValues();
        
        return composite;
    		
	}
	
	
	
	private void setValues() {
		boolean isCommand = !command.isSeparator();
		
		separatorButton.setSelection(!isCommand);
		commandButton.setSelection(isCommand);
		
		
		hubMenuButton.setSelection(command.matches(Command.HUB));
		searchMenuButton.setSelection(command.matches(Command.SEARCH));
		userMenuButton.setSelection(command.matches(Command.USER));
		filelistMenuButton.setSelection(command.matches(Command.FILELIST));
		
		nameText.setText(command.getPath());
		hubText.setText(command.getHub());
		
		commandText.setText(command.getCommand());
		commandText.setEnabled(isCommand);
		
		sendOnceForButton.setSelection(command.isAllowMulti());
		sendOnceForButton.setEnabled(isCommand);
	}
	
	
	
	@Override
	protected void okPressed() {
		boolean isCommand = commandButton.getSelection();
		if (isCommand && GH.isEmpty(nameText.getText())) {
			return; //not accept unnamed commands..
		}
		
		command.setSeparator(!isCommand);
		
		int where = 0;
		where += hubMenuButton.getSelection()? Command.HUB : 0 ;
		where += searchMenuButton.getSelection()? Command.SEARCH : 0 ;
		where += userMenuButton.getSelection()? Command.USER : 0 ;
		where += filelistMenuButton.getSelection()? Command.FILELIST : 0 ;
		command.setWhere(where);
		
		command.setHub(hubText.getText());
		command.setCommand(commandText.getText());
		command.setPath(nameText.getText());
		
		command.setAllowMulti(sendOnceForButton.getSelection());
		
		super.okPressed();
	}

	public Command getCommand() {
		return command;
	}


	

}
