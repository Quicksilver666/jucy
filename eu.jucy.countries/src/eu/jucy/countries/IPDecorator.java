package eu.jucy.countries;

import geoip.GEOIP;
import helpers.GH;

import org.eclipse.swt.graphics.Image;


import uc.IUser;
import uc.protocols.client.ClientProtocol;
import uc.protocols.client.ClientProtocolStateMachine;
import uihelpers.TableViewerAdministrator.TableColumnDecorator;



public class IPDecorator extends TableColumnDecorator<Object> {
	
	@Override
	public Image getImage(Object o,Image parent) {
		Image img = null;
		if (o instanceof ClientProtocol) {
			ClientProtocol cp = (ClientProtocol)o;
			IUser other = cp.getUser();
			
			img = FlagStorage.get().getFlag(other,true,false);
		} else if (o instanceof ClientProtocolStateMachine ) {
			ClientProtocolStateMachine ccspm =  (ClientProtocolStateMachine)o;
			img = FlagStorage.get().getFlag(ccspm.getUser(),true,false);
		}
		
		return img;
	}

	
	@Override
	public String getText(Object o, String parent) {
		if (GH.isNullOrEmpty(parent)) {
			return parent;
		}
		
		IUser other = null;
		if (o instanceof ClientProtocol) {
			ClientProtocol cp = (ClientProtocol)o;
			other = cp.getUser();
		} else if (o instanceof ClientProtocolStateMachine ) {
			ClientProtocolStateMachine ccspm =  (ClientProtocolStateMachine)o;
			other = ccspm.getUser();
		}
		
		if (other != null && other.getIp() != null) {
		//	Country c = GEOIP.get().getCountry(other.getIp());
			
			String cc = GEOIP.get().getCountryCode(other.getIp());
			
		//	Location loc = GEOIP.get().getLocation(other.getIp());
			if (cc != null) {
				return  cc+"("+parent+")";
			}
		}
		
		return parent;
		
	}

}
