package eu.jucy.notepad.editors;


import eu.jucy.gui.UCEditorInput;

public class NotepadInput extends UCEditorInput {

	private final String notepadID;
	
	
	
	public NotepadInput() {
		this(Notepad.notepad0);
	}


	public NotepadInput(String notepadID) {
		super();
		this.notepadID = notepadID;
	}



	public String getNotepadID() {
		return notepadID;
	}

	private int getNotepadNumber() {
		return Integer.parseInt(notepadID.substring(notepadID.length()-1)); 
	}

	public String getName() {
		String s = Messages.getString("NotepadInput"); //$NON-NLS-1$
		int i = getNotepadNumber()+1;
		if (i != 1) {
			s += " - " + i;
		}
		return s;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((notepadID == null) ? 0 : notepadID.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		NotepadInput other = (NotepadInput) obj;
		if (notepadID == null) {
			if (other.notepadID != null)
				return false;
		} else if (!notepadID.equals(other.notepadID))
			return false;
		return true;
	}
	
	

}
