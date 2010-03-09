package eu.jucy.op;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;



import uc.IHub;
import uc.files.filelist.FileListFile;
import uc.protocols.SendContext;

public class OpADLSendContext extends SendContext {
	
	
	
	private int count;
	private Collection<FileListFile> files;
	private String comment;
	private final IHub hub;
	

	
	public OpADLSendContext(IHub hub,FileListFile file,String comment) {
		super(file,Collections.<String,String>emptyMap());
		this.hub = hub;
		files = Collections.singleton(file);
		this.comment = comment;
		replacements = getReplacements();
	}

	public OpADLSendContext(IHub hub,FileListFile distinguished, Collection<FileListFile> files, int count, String comment) {
		super(distinguished,Collections.<String,String>emptyMap());
		this.hub = hub;
		this.count = count;
		this.files = files;
		this.comment = comment;
		replacements = getReplacements(); 
	}

//	@Override
//	public String format(String command) {
//		
//		for (int i = 0 ; i < 5; i++) { 
//			Matcher m = replace.matcher(command);
//			int currentpos = 0;
//			
//			while(m.find(currentpos)) {
//				String formatStringfound = m.group(2);
//				String replacement = getReplacement(formatStringfound);
//				if (replacement != null) {
//					command = command.replace(m.group(1), replacement);
//				}
//				
//				currentpos = m.start(1)+1;
//			}
//		}
//		
//		return super.format(command);
//	}
//	
//	private String getReplacement(String what) {
//		String s = replacements.get(what);
//		if (s != null) {
//			return s;
//		}
//		return null;
//	}
	
	
	private Map<String,String> getReplacements() {
		HashMap<String,String> replacements = new HashMap<String,String>();
		
		replacements.putAll( StaticReplacement.loadReplacements());
		replacements.putAll( StaticReplacement.loadReplacements(hub.getFavHub()));
		
		
		replacements.put("OpADLFileCount", ""+files.size());
		replacements.put("OpADLComment", comment);
		replacements.put("OpADLCounter", ""+count);
			
		return replacements;
	}
	
}
