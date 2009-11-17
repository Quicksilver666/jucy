package helpers;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;



public class NLS {

	
	public static void load(String bundleName,Class<?> clazz) {
		ResourceBundle rb = ResourceBundle.getBundle(bundleName,Locale.getDefault(),clazz.getClassLoader());
		ResourceBundle rbDefault = null;
		
		final Field[] fieldArray = clazz.getDeclaredFields();
		for (Field f:fieldArray) {
			String name = f.getName();
			String translation = null;
			try{
				translation = rb.getString(name);
			} catch (MissingResourceException mre) {
				if (rbDefault == null) {
					rbDefault = ResourceBundle.getBundle(bundleName,Locale.ENGLISH,clazz.getClassLoader());
				}
				translation = rb.getString(name);
			}
			if (translation == null) {
				throw new MissingResourceException("Not found res",clazz.getCanonicalName(),name);
			}
			try {
				f.set(null, translation);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
	}
	
}
