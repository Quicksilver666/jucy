package eu.jucy.charts;

import java.util.List;



public interface IChartDataProvider {

	/**
	 * tells the DataProvider to register itself and collect data..
	 * (editor is opened..)
	 */
	void register();
	
	/**
	 * tells the Provider to cancel registration
	 * and dispose of gathered Data
	 * (editor closed)
	 */
	void unregister();
	
	
	/**
	 * 
	 * @return a list of currently available data elements
	 */
	List<DataElement> getData(long after);
	
	/**
	 * 
	 * @return the largest DataElement used
	 * for scaling
	 */
	DataElement getLargestDataelement();
}
