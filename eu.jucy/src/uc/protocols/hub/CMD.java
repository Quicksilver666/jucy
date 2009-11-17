package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;


import uc.Command;

public class CMD extends AbstractADCHubCommand {

	
	
	public CMD(Hub hub) {  // ICMD Status\\Do\snot\sdisturb CT1 TTBINF\s%[mySID]\sAW2\n
		super(hub);									//command name   //flags  
		setPattern(getHeader()+" ("+ADCTEXT+") (.*)",true);
	}

	public void handle(String command) throws ProtocolException, IOException {
		
		String name = revReplaces(matcher.group(HeaderCapt+1));
		
		Map<Flag,String> flags = getFlagMap(matcher.group(HeaderCapt+2));
		int context  =  0;
		if (flags.containsKey(Flag.CT)) {
			context = Integer.parseInt( flags.get(Flag.CT) );
		}
		boolean remove = "1".equals( flags.get(Flag.RM) );
		boolean seperator = "1".equals(flags.get(Flag.SP));
		
		Command c;
		if (seperator) {
			Command com = hub.getLastUserCommand();
			if (GH.isEmpty(name) && com != null) {
				name = com.getParentPath()+"\\";
			}
			c = new Command(context,name,hub.getFavHub().getHubaddy());
		} else {
			String exec = flags.get(Flag.TT);
			boolean allowMulti = !"1".equals(flags.get(Flag.CO));
			c =  new Command(name,allowMulti,context,exec,hub.getFavHub().getHubaddy());
		}
		
		logger.debug("Received cmd: "+command+" remove:"+remove+"  name:"+name+":");
		 
		
		if (remove) {
			hub.removeUserCommand(c);
		} else {
			hub.addUserCommand(c);
		}
	}

}
