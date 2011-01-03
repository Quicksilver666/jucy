package uc.protocols;




public abstract class AbstractNMDCProtocolCommand extends AbstractDCProtocolCommand  {


	/**
	 * pattern matching the prefix.. (no space)
	 */
	protected final String prefix = "^\\Q"+getPrefix()+"\\E"; 
	



	/**
	 * 
	 * @param command the protocol command..
	 * @return true if this is the correct command
	 * base implementation only checks against a prefix
	 */
	public boolean matches(String command) {
		if (getPattern() != null) {
			matcher = getPattern().matcher(command);
			return matcher.matches();
		} else {
			return command.startsWith(getPrefix()+" ");
		}
	}
	
	
	/**
	 * @return the chars before the first space 
	 * character inclusive the $ protocol character
	 * exclusive the space
	 * 
	 */
	public String getPrefix() {
		return "$"+getClass().getSimpleName();
	}
	

}
