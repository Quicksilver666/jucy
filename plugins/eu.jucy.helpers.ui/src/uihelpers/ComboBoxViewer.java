package uihelpers;

import helpers.GH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Combo;

public  class ComboBoxViewer<T> {

	
	private final List<T> items;
	private final Combo combo;
	
	
	public ComboBoxViewer(Combo combo,T[] items) {
		this(combo,Arrays.asList(items));
	}
	
	public ComboBoxViewer(Combo combo, List<T> items) {
		this(combo,items, false);
	}
	
	/**
	 * 
	 * @param combo - the combo for which this viewer is
	 * @param items - the items that can be choosen
	 * @param addEmptyItem - if an additional empty item should be added "no item selected"
	 */
	public ComboBoxViewer(Combo combo, List<T> items,boolean addEmptyItem) {
		this.combo = combo;
		this.items =  new ArrayList<T>(items);
		if (addEmptyItem) {
			this.items.add(0, null);
		}
		
		for (T t: this.items) {
			combo.add(get(t));
		}
	}
	
	/**
	 * selects the given item in the ComboBox
	 * 
	 * @param t - one item that is presenting the list
	 * throws IllegalArgumentException if not in the list.
	 */
	public void select(T t) {
		int i = items.indexOf(t);
		if (i == -1) {
			throw new IllegalArgumentException();
		} else {
			combo.select(i);
		}
	}
	
	/**
	 * selects the first item that will return the same string 
	 * @param s
	 */
	public void selectByString(String s) {
		for (T t : items) {
			if ( GH.isNullOrEmpty(s) ? GH.isNullOrEmpty(get(t)) : s.equals(get(t))) {
				select(t);
			}
		}
	}

	private String get(T t) {
		if (t == null) {
			return "";
		}else {
			return getShown(t);
		}
	}
	
	/**
	 * 
	 * @param t - an item that should be presented
	 * implementing classes should override this method..
	 * 
	 * @return bhy default toString() is returned
	 */
	protected String getShown(T t) {
		return t.toString();
	}
	
	/**
	 * 
	 * @return the currently selected item
	 */
	public T getSelected() {
		int i  = combo.getSelectionIndex();
		if (i != -1) {
			return items.get(i);
		}
		return null;
	}
	
	public String getSelectedString() {
		return get(getSelected());
	}
	
}
