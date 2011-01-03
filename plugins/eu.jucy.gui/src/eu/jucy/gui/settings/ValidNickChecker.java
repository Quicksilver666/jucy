package eu.jucy.gui.settings;

import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

/**
 * helps detecting bad characters..
 * so user can't enter them in textControl
 * 
 * @author Quicksilver
 *
 */
public class ValidNickChecker implements KeyListener {

	private final char[] invalid;
	

	
	/*
	 * default allows no space in Nicks
	 *
	public ValidNickChecker() {
		
	} */
	
	public ValidNickChecker(boolean spaceAllowed) {
		if (spaceAllowed) {
			invalid = new char[] {'$','|','%','<','>'};
		} else {
			invalid = new char[] {'$','|','%','<','>',' '};
		}
	}
	

	public void keyPressed(KeyEvent e) {
		e.doit = isAllowedCharacter((char)e.keyCode);
	}


	public void keyReleased(KeyEvent e) {}

	
	/**
	 * 
	 * @param a character to test
	 * @return true if the character is allowed
	 */
	public boolean isAllowedCharacter(char a) {
		for (char inva: invalid) {
			if (inva == a) {
				return false;
			}
		}
	//	if ((a >= 1 && a <=4) || a == 8) {
	//		return true;
	//	}
		//if (a < 32) {
		//	return false;
		//}
		
		return true;
	}
	
	/**
	 * true if no invalid characters are in the Nick
	 * @param s
	 * @return
	 */
	public boolean checkString(String s,int forbidSmallerCharThen) {
		if (s == null) {
			return true;
		}
		for (char inva: invalid) {
			if (s.indexOf(inva) != -1) {
				return false;
			}
		}
		
		for (int i = 0; i < s.length(); i++ ) {
			if (s.charAt(i) < forbidSmallerCharThen) {
				return false;
			}
		}
		
		return true;
	}
	

}
