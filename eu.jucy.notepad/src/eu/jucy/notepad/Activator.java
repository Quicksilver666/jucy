package eu.jucy.notepad;



public class Activator implements Runnable {

	public static final byte MAX_NOTEPADS = 5;
	public static final String NOTEPADENABLED = NPI.PLUGIN_ID+".padnr";
	
	public Activator() {
	}

	
	
	public void run() {
		int nrOfNotepads = NPI.getInt(NPI.NR_OF_NOTEPADS);
		for (int i = 0; i < MAX_NOTEPADS;i++) {
			System.setProperty(NOTEPADENABLED+i, Boolean.valueOf(i < nrOfNotepads).toString());
		}
		
	}



}
