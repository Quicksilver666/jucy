package helpers;

public interface ISubstringMapping<Item> {

	/**
	 * 
	 * 
	 * @param pos - position in the object
	 * @param v - the object 
	 * @return the char at the position for the object..
	 * though a space if this char would equal SubstringMap2.TERMINATOR
	 * if getCharAt equals 
	 */
	char getCharAt(int pos, Item v);
	
	/**
	 * 
	 * @param v object from with information is took
	 * @return the length of the string this 
	 */
	int getLength(Item v);
}
