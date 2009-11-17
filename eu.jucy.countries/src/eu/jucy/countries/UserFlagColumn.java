package eu.jucy.countries;

import geoip.GEOIP;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.Location;

import uc.IUser;

import eu.jucy.gui.UserColumns;



public class UserFlagColumn extends UserColumns {




	public UserFlagColumn() {
		super(80, "Location", SWT.LEAD); //TODO location nationalization...
	}

	@Override
	public String getText(IUser x) {
		if (x.getIp() != null) {
			if (GEOIP.get().isCountryOnly()) {
				Country c = GEOIP.get().getCountry(x.getIp());
				if (c != null) {
					return c.getName();
				}
			} else {
				Location loc = GEOIP.get().getLocation(x.getIp());
				if (loc != null) {
					return loc.countryName +  ( loc.city != null? " - "+loc.city: "");
				}
			}
		}
		return "";
	}
	
	@Override
	public Image getImage(IUser cur) {
		Image img = FlagStorage.get().getFlag(cur,true,false);
		
		return img;
	}


	
}
