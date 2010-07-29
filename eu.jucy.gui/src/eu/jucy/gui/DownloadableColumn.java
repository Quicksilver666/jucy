package eu.jucy.gui;


import helpers.PreferenceChangedAdapter;
import helpers.SizeEnum;

import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import eu.jucy.gui.UserColumns.Nick;


import uc.IUser;
import uc.crypto.HashValue;
import uc.files.IDownloadable;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.IDownloadable.IDownloadableFolder;
import uc.files.downloadqueue.DownloadQueue;
import uc.files.filelist.FileListFile;
import uc.files.filelist.FileListFolder;
import uc.files.filelist.IOwnFileList;
import uc.files.search.ISearchResult;
import uihelpers.IconManager;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class DownloadableColumn extends ColumnDescriptor<IDownloadable> {

	private static Color fileInDownloadCol;
	private static Color fileInShareCol;
	private static Color fileMultiUserCol;
	private static Color fileDefaultCol;
	
	private static void loadFontsAndColours() {
		fileInDownloadCol = GUIPI.getColor(GUIPI.fileInDownloadCol);
		fileInShareCol = GUIPI.getColor(GUIPI.fileInShareCol);
		fileMultiUserCol = GUIPI.getColor(GUIPI.fileMultiUserCol);
		fileDefaultCol = GUIPI.getColor(GUIPI.fileDefaultCol);
	}
	
	static {
		loadFontsAndColours();
		new PreferenceChangedAdapter(GUIPI.get(),
				GUIPI.fileInDownloadCol,GUIPI.fileInShareCol,GUIPI.fileMultiUserCol,GUIPI.fileDefaultCol) {

			@Override
			public void preferenceChanged(String preference, String oldValue,
					String newValue) {

				new SUIJob() {
					@Override
					public void run() {
						loadFontsAndColours();
					}
				}.scheduleIfNotRunning(500,getClass());
				
			}
		};
	}
	
	
	public static Color getDownloadableColor(IDownloadable x) {

		if (x.isFile()) {
			IDownloadableFile file = (IDownloadableFile)x;
			if (ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue().containsDQE(file.getTTHRoot())) {
				return fileInDownloadCol;
			}
		
			if (ApplicationWorkbenchWindowAdvisor.get().getFilelist().getFile(file.getTTHRoot()) != null) {
				return fileInShareCol;
			}
	
			if (file.nrOfUsers() > 1) {
				return fileMultiUserCol;
			}
		} else if (x instanceof FileListFolder) {
			if (x.getUser().equals(ApplicationWorkbenchWindowAdvisor.get().getFilelistself())) {
				return fileInShareCol;
			}
			FileListFolder folder = (FileListFolder)x;
			IOwnFileList fileList = ApplicationWorkbenchWindowAdvisor.get().getFilelist();
			DownloadQueue dq = ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue();
			boolean inDownload = false;
			boolean completedDownload = false;
			for (FileListFile fileListFile : folder) {
				HashValue hash = fileListFile.getTTHRoot();
				if (fileList.getFile(hash) != null) {
					completedDownload = true;
				} else if (dq.containsDQE(hash)) {
					inDownload = true;
				} else {
					return fileDefaultCol;
				}
			}
			
			
			if (inDownload) {
				return fileInDownloadCol;
			} else if (completedDownload) {
				return fileInShareCol;
			} else {
				return fileDefaultCol;
			}
			
		}
		return fileDefaultCol;
	}
	
	
	
	
	public DownloadableColumn(int defaultColumnSize, String columnName,int style) {
		super(defaultColumnSize, columnName, style);
	}
	
	
	@Override
	public Comparator<IDownloadable> getComparator() {
		return new Comparator<IDownloadable>() {
			private Comparator<IDownloadable> base = DownloadableColumn.super.getComparator(); 
			public int compare(IDownloadable o1, IDownloadable o2) {
				int i=Boolean.valueOf(o1.isFile()).compareTo(o2.isFile());
				if (i != 0) {
					return i;
				} else {
					return base.compare(o1, o2);
				}
			}
			
		};

	}

	@Override
	public Image getImage(IDownloadable downloadable) {
		return null;
	}
	
	
	@Override
	public String getText(IDownloadable x) {
		if (x.isFile()) {
			return getFileText((IDownloadableFile)x);
		} else {
			return  getFolderText((IDownloadableFolder)x);
		}
	}


	abstract String getFolderText(IDownloadableFolder folder);
	
	abstract String getFileText(IDownloadableFile file);

	
	/**
	 * represents the name of the file 
	 *
	 */
	public static class FileColumn extends DownloadableColumn {

		public FileColumn() {
			super(200, Lang.FileCol,SWT.LEAD);
		}

		@Override
		public Image getImage(IDownloadable downloadable) {
			if (downloadable.isFile()) {
				return IconManager.get().getIcon(((IDownloadableFile)downloadable).getEnding());
			} else {
				return IconManager.get().getFolderIcon();
			}
		}

		@Override
		String getFileText(IDownloadableFile file) {
			return file.getName();
		}

		@Override
		String getFolderText(IDownloadableFolder folder) {
			return folder.getName();
		}

		@Override
		public Color getForeground(IDownloadable x) {
			return getDownloadableColor(x);
		}
	}
	
	
	
	public static class Type extends DownloadableColumn {

		
		
		public Type() {
			super(30, Lang.Type ,SWT.LEAD);
		}

		@Override
		String getFileText(IDownloadableFile file) {
			return file.getEnding();
		}

		@Override
		String getFolderText(IDownloadableFolder folder) {
			return "";
		}
	}
	
	public static class Size extends DownloadableColumn {

		
		
		protected Size(int defaultColumnSize, String columnName, int style) {
			super(defaultColumnSize, columnName, style);
		}

		public Size() {
			super(100, Lang.Size, SWT.TRAIL);
		}

		@Override
		String getFileText(IDownloadableFile file) {
			return SizeEnum.getReadableSize(file.getSize());
		}

		@Override
		String getFolderText(IDownloadableFolder folder) {
			if (folder instanceof FileListFolder) {
				return SizeEnum.getReadableSize(getSize(folder));
			} 
			return "";
		}

		protected long getSize(IDownloadable file) {
			if (file.isFile()) {
				return ((IDownloadableFile)file).getSize();
			} else if (file instanceof FileListFolder) {
				return ((FileListFolder)file).getContainedSize();
			} else {
				return 0;
			}
		}
		
		@Override
		public Comparator<IDownloadable> getComparator() {
			
			return new Comparator<IDownloadable>() {
				public int compare(IDownloadable o1, IDownloadable o2) {
					int i=Boolean.valueOf(o1.isFile()).compareTo(o2.isFile());
					if (i != 0) {
						return i;
					} else {
						return Long.valueOf(getSize(o1)).compareTo(getSize(o2));
					}
				}
				
			};
		}
	}
	
	public static class ExactSize extends Size {

		public ExactSize() {
			super(120, Lang.ExactSize, SWT.TRAIL);
		}

		@Override
		String getFileText(IDownloadableFile file) {
			return SizeEnum.getExactSharesize(file.getSize());
		}

		@Override
		String getFolderText(IDownloadableFolder folder) {
			if (folder instanceof FileListFolder) {
				return SizeEnum.getExactSharesize(getSize(folder));
			} 
			return "";
		}
	}
	
	public static class TTHRoot extends DownloadableColumn {

		public TTHRoot() {
			super(200, Lang.TTHRoot, SWT.LEAD);
		}

		@Override
		String getFileText(IDownloadableFile file) {
			return file.getTTHRoot().toString();
		}

		@Override
		String getFolderText(IDownloadableFolder folder) {
			return "";
		}
	}
	
	public static class Path extends DownloadableColumn {
		

		public Path() {
			super(300,  Lang.Path, SWT.LEAD);
		}

		@Override
		String getFileText(IDownloadableFile file) {
			return file.getOnlyPath();
		}

		@Override
		String getFolderText(IDownloadableFolder folder) {
			return folder.getPath();
		}
		
	}
	
	
	public static class UserWrapper extends DownloadableColumn {
		
		/**
		 * creates a user column
		 * @return
		 */
		public static UserWrapper createUserColumn() {
			return new UserWrapper(new Nick(),Lang.User);
		}
		
		private final ColumnDescriptor<IUser> desc;
		
		/**
		 * wraps the UserColumn and replaces the name
		 */
		public UserWrapper(ColumnDescriptor<IUser> desc,String columnName) {
			super(desc.getDefaultColumnSize(),columnName,desc.getStyle());
			this.desc = desc;
		}
		
		/**
		 * wraps a UserColumn so it can be used with IDownloadable
		 * @param desc the UserColumn to wrap
		 */
		public UserWrapper(ColumnDescriptor<IUser> desc) {
			super(desc.getDefaultColumnSize(),desc.getColumnName(),desc.getStyle());
			this.desc = desc;
		}
		
		@Override
		String getFileText(IDownloadableFile file) {
			if (file.nrOfUsers() > 1) {
				return file.nrOfUsers()+" "+Lang.Users;
			}
			
			return desc.getText(file.getUser());
		}
		@Override
		String getFolderText(IDownloadableFolder folder) {
			return desc.getText(folder.getUser());
		}

		@Override
		public Comparator<IDownloadable> getComparator() {
			
			return new Comparator<IDownloadable>() {
				Comparator<IUser> comp = desc.getComparator();
				public int compare(IDownloadable o1, IDownloadable o2) {
					if (o1.nrOfUsers() > 1  || o2.nrOfUsers() > 1) {
						return -Integer.valueOf(o1.nrOfUsers()).compareTo(o2.nrOfUsers());
					} else {
						return comp.compare(o1.getUser(), o2.getUser());
					}
				}
			};
		}

		@Override
		public Image getImage(IDownloadable downloadable) {
			return desc.getImage(downloadable.getUser());
		}
		
		
		
	}
	
	
	public static class SlotsSearchColumn extends ColumnDescriptor<IDownloadable>  {

		public SlotsSearchColumn() {
			super(30, Lang.Slots, SWT.LEAD);
		}

		public Image getImage(IDownloadable x) {
			return null;
		}

		
		@Override
		public Comparator<IDownloadable> getComparator() {
			return new Comparator<IDownloadable>() {
				public int compare(IDownloadable s1, IDownloadable s2) {
					ISearchResult o1 = (ISearchResult)s1, o2 = (ISearchResult)s2;
					return Float.valueOf((float)o1.getAvailabelSlots()/o1.getTotalSlots())
							.compareTo((float)o2.getAvailabelSlots()/o2.getTotalSlots());
				}
				
			};
		}

		@Override
		public String getText(IDownloadable s1) {
			
			ISearchResult sr = (ISearchResult)s1;
			return sr.getAvailabelSlots()+"/"+sr.getTotalSlots();
		}
		
	}
	
	
	
	
}
