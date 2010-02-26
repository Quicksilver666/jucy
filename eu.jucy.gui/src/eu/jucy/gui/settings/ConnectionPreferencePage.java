package eu.jucy.gui.settings;






import helpers.GH;

import org.eclipse.jface.preference.BooleanFieldEditor;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;



import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;


import uc.PI;



/**
 * 
 * 
 * @author Quicksilver
 */
public class ConnectionPreferencePage extends UCPrefpage {

	private static final int MAX_PORT_NUMBER = (int)Math.pow(2, 16)-1; 
	private Group activeGroup; 
	private Group socksGroup; 
	private IntegerFieldEditor tcpTLSPort;
	
	private BooleanFieldEditor passive;
	
	public ConnectionPreferencePage() {
		super(PI.PLUGIN_ID);
	}
	
	private void updatePassive(boolean newValue) {
		for (Control c: activeGroup.getChildren()) {
			c.setEnabled(!newValue);
		}
		activeGroup.setEnabled(!newValue);
		
		for (Control c: socksGroup.getChildren()) {
			c.setEnabled(newValue);
		}
		socksGroup.setEnabled(newValue); 
	}
	
	private void updateTLS(boolean newValue) {
		tcpTLSPort.setEnabled(newValue,activeGroup);
	
	}

	@Override
	protected void createFieldEditors() {
		
		passive = new BooleanFieldEditor(PI.passive,
				Lang.UsePassiveMode,
				getFieldEditorParent()) {
					@Override
					protected void valueChanged(boolean oldValue,
							boolean newValue) {
						updatePassive(newValue);
					}
			
		};
		addField(passive);
		
		int cols = ((GridLayout)getFieldEditorParent().getLayout()).numColumns;
		
		Composite comp = new Composite(getFieldEditorParent(),SWT.NONE);
		GridData gd3 =new GridData(SWT.FILL,SWT.FILL,false,false);
		gd3.horizontalSpan = cols;
		comp.setLayoutData(gd3);
		comp.setLayout(new GridLayout(cols,false));
		
		activeGroup = new Group(comp,SWT.NONE);
		activeGroup.setText(Lang.ActiveModeSettings);
		GridData gd =new GridData(SWT.FILL,SWT.FILL,false,false);
		gd.horizontalSpan = cols;
		activeGroup.setLayoutData(gd);
		activeGroup.setLayout(new GridLayout(cols,false));
		
		BooleanFieldEditor upnp = new BooleanFieldEditor(PI.allowUPnP,
				Lang.AllowUsingUPnP,
				activeGroup);
		addField(upnp);

		
		String host =	ApplicationWorkbenchWindowAdvisor.get()
							.getConnectionDeterminator().getDetectedIP().getHostAddress();
		
		StringFieldEditor wanip = new StringFieldEditor(PI.externalIp,
				String.format(Lang.ExternalWANIP, host),
				activeGroup);
		addField(wanip);
		
		
		
		final IntegerFieldEditor tcpport= new IntegerFieldEditor(PI.inPort,
				Lang.TCPPort,
				activeGroup);
		
		tcpport.setValidRange(1, (int)Math.pow(2, 16)-1);
		addField(tcpport);
		
		IntegerFieldEditor udpport = new IntegerFieldEditor(PI.udpPort,
				Lang.UDPPort,
				activeGroup);
		udpport.setValidRange(1, MAX_PORT_NUMBER);
		addField(udpport);

		
		BooleanFieldEditor allowTLS = new BooleanFieldEditor(PI.allowTLS,
				Lang.UseTLSIfPossible,
				activeGroup) {
			protected void valueChanged(boolean oldValue,
					boolean newValue) {
				updateTLS(newValue); 
			}
		};
		addField(allowTLS);
		
		
		tcpTLSPort = new IntegerFieldEditor(PI.tlsPort,
				Lang.TLSTCPPort,  
				activeGroup) {

			@Override
			protected boolean checkState() {
				try {
					return super.checkState() && tcpport.getIntValue() != getIntValue();
				} catch (NumberFormatException nfe) {
					return false;
				}
			}
		};
		tcpTLSPort.setValidRange(1, MAX_PORT_NUMBER);
		addField(tcpTLSPort);
		
		
		socksGroup = new Group(comp,SWT.NONE);
		socksGroup.setText("SOCKS Proxy Settings");
		GridData gd2 = new GridData(SWT.FILL,SWT.FILL,false,false);
		gd2.horizontalSpan = cols;
		socksGroup.setLayoutData(gd2);
		socksGroup.setLayout(new GridLayout(cols,false));
		
		
		final BooleanFieldEditor useSocks = new BooleanFieldEditor(PI.socksProxyEnabled,
				"Use Socks Proxy",
				socksGroup);
		addField(useSocks);
		
		StringFieldEditor socksHost = new StringFieldEditor(PI.socksProxyHost,
				"Socks Host",
				socksGroup) {
					@Override
					protected boolean checkState() {
						return super.checkState() &&  (!useSocks.getBooleanValue()|| !GH.isEmpty(getStringValue()) );
					}
			
		};
		addField(socksHost);
		
		IntegerFieldEditor socksPort = new IntegerFieldEditor(PI.socksProxyPort,
				"Socks Port",
				socksGroup);
		socksPort.setValidRange(1, MAX_PORT_NUMBER);
		addField(socksPort);
		
		StringFieldEditor socksUsername = new StringFieldEditor(PI.socksProxyUsername,
				"Socks Username",
				socksGroup);
		addField(socksUsername);
		
		final StringFieldEditor socksPassword = new StringFieldEditor(PI.socksProxyPassword,
				"Socks Password",
				socksGroup);
		socksPassword.getTextControl(socksGroup).setEchoChar('*');
		addField(socksPassword);
		
		StringFieldEditor socksPassword2 = new StringFieldEditor(PI.socksProxyPassword,
				"Repeat Socks Password",
				socksGroup) {
			
				protected boolean checkState() {
					return super.checkState() && 
						socksPassword.getStringValue().equals(getStringValue());
				}
		};
		socksPassword2.getTextControl(socksGroup).setEchoChar('*');
		
		addField(socksPassword2); 
		

		
		updatePassive(PI.getBoolean(PI.passive));
		updateTLS(PI.getBoolean(PI.allowTLS));
		
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		updatePassive(passive.getBooleanValue());
		
	}

	
	
}
