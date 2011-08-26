package helpers;

/**
 * mediates between  preferences and Objects they presenting 
 * 
 * @author Quicksilver
 *
 * @param <T> the object needing serialisation
 */
public interface IPrefSerializer<T> {
	
	T unSerialize(String[] all);
	
	String[] serialize(T t);
	
}