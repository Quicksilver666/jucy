package uc;

/**
 * interface for all classes that can be started..
 * 
 * @author Quicksilver
 *
 */
public interface IStoppable {

	void stop();
	
	public static interface IStartable extends IStoppable{
		void start();
	}
}
