package eu.jucy.gui.texteditor.pmeditor;



import org.eclipse.core.runtime.Assert;


import eu.jucy.gui.UCEditorInput;


import uc.IUser;


public class PMEditorInput extends UCEditorInput {

	private final IUser other;	

	private final long time;
	
	//private List<ILogEntry> logs;

	
	public PMEditorInput(IUser other){
		this(other,Long.MAX_VALUE);
	}
	
	public PMEditorInput(IUser other,long time) {
		super();
		Assert.isNotNull(other);
		this.other = other;
		this.time = time;
	}
	
//	public void loadLogs() {
//		synchronized(this) {
//			if (logs == null) {
//				DBLogger entity = new DBLogger(other,ApplicationWorkbenchWindowAdvisor.get());
//				logs = entity.loadLogEntrys(StyledTextViewer.HISTORY, 0);
//			}
//		}
//	}
//	
//	public List<ILogEntry> getLogs() {
//		synchronized(this) {
//			if (logs == null) {
//				loadLogs();
//			}
//			return logs;
//		}
//	}
//	



	public String getName() {
		return other.getNick();
	}
	
	public long getTime() {
		return time;
	}


	public String getToolTipText() {
		return other.getNick()+(other.getHub() != null?" - "+ other.getHub().getName():"");
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return other.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PMEditorInput other = (PMEditorInput) obj;
		
		if (!this.other.equals(other.other))
			return false;
		return true;
	}

	/**
	 * @return the other
	 */
	public IUser getOther() {
		return other;
	}
	
	

}
