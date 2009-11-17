package uc.protocols.hub;

public interface IHubnameChangedListener {
	
	/**
	 * tells that either the hubname or the topic have changed
	 * @param hubname
	 * @param topic
	 */
	public void hubnameChanged(String hubname,String topic); 
	
}
