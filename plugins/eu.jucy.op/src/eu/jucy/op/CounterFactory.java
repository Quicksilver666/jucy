package eu.jucy.op;

import helpers.GH;
import helpers.PrefConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;




import uc.IUser;
import uc.files.filelist.FileListFile;

public class CounterFactory {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	

	
	/**
	 * name and id for counterFactory
	 */
	private String name = "";
	
	/**
	 * comment.. can be used in raw..
	 * for reference
	 */
	private String comment = "";

	/**
	 * if the counter should be evaluated
	 * and cleared after each file.
	 * 
	 */
	private boolean perFile = false;

	/**
	 * the priority that the counter has for execution
	 */
	private int priority = 100;
	
	
	private final List<CounterAction> actions = new ArrayList<CounterAction>();
	


	public static List<WorkingCounter> getWorkingCounter(List<CounterFactory> factories,IUser usr) {
		List<WorkingCounter> c = new ArrayList<WorkingCounter>();
		for (CounterFactory f: factories) {
			c.add(f.getWorkingCounter(usr));
		}
		for (WorkingCounter first: c) {
			for (WorkingCounter second: c) {
				if (first != second) {
					first.addWorkingCounter(second);
				}
			}	
		}
		Collections.sort(c);
		
		return c;
	}
	
	public String[] toStringAR() {
		return new String[] {
			name,
			""+perFile,
			""+priority,
			comment,
			getActions()
		};
	}
	
	public static CounterFactory fromStrinngAR(String[] ar) {
		CounterFactory cf = new CounterFactory();
		cf.name = ar[0];
		cf.perFile = Boolean.parseBoolean(ar[1]);
		cf.priority = Integer.parseInt(ar[2]);
		cf.comment = ar[3];
		cf.loadActions(ar[4]);
		return cf;
	}
	
	private String getActions() {
		List<String> ac = new ArrayList<String>();
		for (CounterAction ca: actions) {
			ac.add(PrefConverter.asString(ca.toStringAR(0)));
		}
		return PrefConverter.asString(ac.toArray(new String[]{}));
	}
	
	private void loadActions(String s) {
		for (String sOfAction: PrefConverter.asArray(s)) {
			actions.add(new CounterAction(PrefConverter.asArray(sOfAction),0));
		}
	}
	
	public boolean add(CounterAction o) {
		return actions.add(o);
	}

	public boolean remove(Object o) {
		return actions.remove(o);
	}
	
	public List<CounterAction> getAllActions() {
		return Collections.unmodifiableList(actions);
	}
	
	/**
	 * the name of the CounterFactory
	 * 
	 */
	public String toString() {
		return name;
	}

	
	/**
	 * makes this counter ready for the next user
	 * @param next
	 */
	private WorkingCounter getWorkingCounter(IUser next) {
		return new WorkingCounter(next);
	}
	

	
	class WorkingCounter implements Comparable<WorkingCounter> {
		
		private Set<FileListFile> files = new HashSet<FileListFile>();
		
		private FileListFile last = null;
		
		private final Map<String,WorkingCounter> otherCounters = new HashMap<String,WorkingCounter>();
		
		private int count;
		
		private IUser usr;
		
		private WorkingCounter(IUser current) {
			this.usr = current; 
		}
		
		
		private void addWorkingCounter(WorkingCounter counter) {
			otherCounters.put(counter.getName(), counter);
		}
		
		public String getName() {
			return CounterFactory.this.name;
		}
		public int getPriority() {
			return CounterFactory.this.priority;
		}
		
		public String getComment() {
			return CounterFactory.this.comment;
		}
		
		public boolean isPerFile() {
			return CounterFactory.this.perFile;
		}
		
		/**
		 * adds multiple files to a counter
		 * 
		 * @param s + the files to be added
		 * @param count - how much the counter should be incremented..
		 * does not work perFile !
		 */
		public void addFiles(Collection<FileListFile> s, int count) {
			if (!s.isEmpty() && !perFile) {
				files.addAll(s);
				this.count += count;
			} else  if (perFile && files.size() == 1) {
				addFile((FileListFile)s.toArray()[0],count);
			}
		}
		
		/**
		 * adds a file found to the counter
		 * 
		 * @param f - the file found
		 * @param count how the counter should be changed
		 * @return true if checking should be broken..
		 */
		public void addFile(FileListFile f,int count) {
			logger.debug("Add File: "+f.getPath()+"  of User "+f.getParent().getUser().getNick()+  " "+count);
			
			files.add(f);
			last = f;
			this.count += count;
		}
		
		/**
		 * if the current file has been finished..
		 * this will be called to clear  per File Counters..
		 * 
		 * @return true if checking should be stopped
		 */
		public boolean fileFinished() {
			boolean breakAfter = false;
			if (perFile && last != null) {	
				breakAfter = evaluate();
				files.clear();
				count = 0;
				last = null;	
			}
			return breakAfter;
		}
		
		public boolean evaluate() {
			if (!isPerFile() || last != null) {
				for (CounterAction action : actions) {
					boolean breakAfter =  action.evaluate(this);
					if (breakAfter)
						return true;
				}
			}
			return false;
		}

		/**
		 * order by highest priority first..
		 */
		public int compareTo(WorkingCounter o) {
			return -Integer.valueOf(this.getPriority()).compareTo(o.getPriority());
		}
	
	}
	
	
	public static class CounterAction extends OPAction { //TODO a schedule re check in x minutes ..
		
		private static final int fields = 5;
		
		public CounterAction(){}
		
		public CounterAction(String[] arr,int extrafields) {
			super(arr,extrafields+fields);
			minCount = Integer.parseInt(arr[extrafields]);
			noLowerBound = Boolean.parseBoolean(arr[extrafields+1]);
			maxCount = Integer.parseInt(arr[extrafields+2]);
			noUpperBound = Boolean.parseBoolean(arr[extrafields+3]);
			breakAfterExecution = Boolean.parseBoolean(arr[extrafields+4]);
		}
		
		/**
		 * interval of counts so the action gets executed
		 * both inclusive
		 */
		private int minCount = 1, maxCount = 0;
		
		private boolean noLowerBound = false, noUpperBound = true;
		
		/**
		 * raw to send
		 */
	//	private String raw = "";
		
		/**
		 * open the FileList into the view of the user
		 */
//		private boolean openFileList;

		/**
		 * increment some other counter by name
		 */
	//	private String incrementCounter = "";
		
		/**
		 * how much the counter should be incremented..
		 */
	//	private int incrementByWhat;
		
		/**
		 * if true stops more actions from being executed...
		 * after being run
		 */
		private boolean breakAfterExecution = true;
		


		public String[] toStringAR(int extrafields) {
			String[] arr = super.toStringAR(fields+extrafields);
			arr[extrafields] = ""+minCount;
			arr[extrafields+1 ] = ""+noLowerBound;
			arr[extrafields+2] = ""+maxCount;
			arr[extrafields+3 ] = ""+noUpperBound;
			arr[extrafields+4] = ""+breakAfterExecution;
			
			return arr;
			
			/*return new String[] {
					""+minCount,
					""+maxCount,
					raw,
					""+openFileList,
					incrementCounter,
					""+ incrementByWhat,
					""+ breakAfterExecution
			}; */
		}
		
		/*public static CounterAction fromStringAR(String[] ar) {
			CounterAction ca = new CounterAction();
			ca.minCount = Integer.parseInt(ar[0]);
			ca.maxCount = Integer.parseInt(ar[1]);
			ca.raw = ar[2];
			ca.openFileList = Boolean.parseBoolean(ar[3]);
			ca.incrementCounter = ar[4];
			ca.incrementByWhat = Integer.parseInt(ar[5]);
			ca.breakAfterExecution = Boolean.parseBoolean(ar[6]);
			
			return ca;
		} */
		
		private boolean evaluate(WorkingCounter w) {
			if ((minCount <= w.count || noLowerBound ) && (w.count <= maxCount || noUpperBound) && !w.files.isEmpty()) {
			
				FileListFile random =  (FileListFile)w.files.toArray()[GH.nextInt(w.files.size())];
				
				execute(w.usr,random, w.files, w.count, w.getComment(), w.otherCounters);
			/*	
				logger.info("Counter "+w.getName()+" Action evaluation: "+random.getName()+ " "+w.count+"  number of files: "+w.files.size());
				//OK .. we do sth..
				if (openFileList) {
					OpenFilelistAction.openFileList(w.usr);
				}
				
				if (!GH.isNullOrEmpty(raw)) {
					IHub hub = w.usr.getHub();
					if (hub != null) {
						hub.sendRaw(raw, new OpADLSendContext(hub,random,w.files,w.count,w.getComment()));
					}
				}
				if (incrementByWhat != 0 && !GH.isNullOrEmpty(incrementCounter)) {
					WorkingCounter other = w.otherCounters.get(incrementCounter);
					if (other != null) {
						other.addFiles(w.files, incrementByWhat);
					}
				} */
				return breakAfterExecution;
			}
			return false;
		}

		public int getMinCount() {
			return minCount;
		}

		public void setMinCount(int minCount) {
			this.minCount = minCount;
		}

		public int getMaxCount() {
			return maxCount;
		}

		public void setMaxCount(int maxCount) {
			this.maxCount = maxCount;
		}

		public String getRaw() {
			return raw;
		}

		public void setRaw(String raw) {
			this.raw = raw;
		}

		public boolean isOpenFileList() {
			return openFileList;
		}

		public void setOpenFileList(boolean openFileList) {
			this.openFileList = openFileList;
		}

		public String getIncrementCounter() {
			return incrementCounter;
		}

		public void setIncrementCounter(String incrementCounter) {
			this.incrementCounter = incrementCounter;
		}

		public int getIncrementByWhat() {
			return incrementByWhat;
		}

		public void setIncrementByWhat(int incrementByWhat) {
			this.incrementByWhat = incrementByWhat;
		}
		
		public boolean isBreakAfterExecution() {
			return breakAfterExecution;
		}

		public void setBreakAfterExecution(boolean breakAfterExecution) {
			this.breakAfterExecution = breakAfterExecution;
		}

		public boolean isNoLowerBound() {
			return noLowerBound;
		}

		public void setNoLowerBound(boolean noLowerBound) {
			this.noLowerBound = noLowerBound;
		}

		public boolean isNoUpperBound() {
			return noUpperBound;
		}

		public void setNoUpperBound(boolean noUpperBound) {
			this.noUpperBound = noUpperBound;
		}
		
		
		
	}
	
	
	public int getPriority() {
		return priority;
	}


	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public boolean isPerFile() {
		return perFile;
	}

	public void setPerFile(boolean perFile) {
		this.perFile = perFile;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	

}
