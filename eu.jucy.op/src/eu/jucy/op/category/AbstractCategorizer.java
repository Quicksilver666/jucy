package eu.jucy.op.category;

import uc.files.filelist.FileListFile;

public abstract class AbstractCategorizer {

	
	
	/**
	 * categorizes a filelist file for this category
	 * @param f - file to be categorized
	 * @param parentPath  path of the parent folder
	 * @return percentage given for matching the category
	 *  1.0f -> definite match 
	 *  0.0  -> no match
	 */
	public abstract double matches(FileListFile f,String parentPath);
	
	
	
	
	public static abstract class BooleanCategorizer extends AbstractCategorizer {
		
		
		@Override
		public double matches(FileListFile f, String parentPath) {
			return match(f,parentPath)?1.0d:0.0d;
		}

		/**
		 * same as normal categorizer just gives definite answers
		 * 
		 * @param f - file to be ckeced
		 * @param parentPath  path of the parent folder
		 * @return true for matches... false for none matches..
		 */
		protected abstract boolean match(FileListFile f,String parentPath);
		
	}
	
	
}
