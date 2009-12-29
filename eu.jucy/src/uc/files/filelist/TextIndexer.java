package uc.files.filelist;

import helpers.GH;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;


import org.apache.pdfbox.pdmodel.PDDocument;

import org.apache.pdfbox.util.PDFTextStripper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import uc.PI;
import uc.crypto.HashValue;
import uc.files.filelist.OwnFileList.FilelistNotReadyException;

public class TextIndexer {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	
	private static final int MAX_TOTALSIZE = 100*1024*1024; //max size to be indexed..
	private static final int MAX_RAMSIZE_FOR_PDF = 15 * 1024 * 1024; // max size to be held in ram..
	private static final String FIELD_HASH = "hash",FIELD_CONTENT = "contents",FIELD_ENDING = "ending"; //contens field -> from PDFReader
	
	private static final Set<String> SUPPORTED_ENDINGS = 
		new HashSet<String>(Arrays.asList("txt","nfo","pdf")); 
	
	
	public static boolean matchesSomeEnding(Collection<String> endings) {
		for (String s:endings) {
			if (SUPPORTED_ENDINGS.contains(s)) {
				return true;
			}
		}
		return false;
	}
	
	private final SimpleAnalyzer analyzer = new SimpleAnalyzer();
	private final Directory index;
	private IndexWriter w;
	
	private final Set<HashValue> presentHashes = new HashSet<HashValue>();
	private volatile boolean created = false;
	


	private final File dir;
	private IndexTextFiles job;
	
	public TextIndexer() throws IOException {
		System.setProperty( "org.apache.lucene.FSDirectory.class", "org.apache.lucene.store.FSDirectory");
		//have to do that -> sadly because of indexer uses it and in 0.79 was set to nioFS which does not exist in old lucene..
		
		dir = new File(new File(PI.getStoragePath(),"db"),"textindex");
		File lockfile = new File(dir,"write.lock");
		if (lockfile.exists()) {
			lockfile.delete();
		}
	
		index = new NIOFSDirectory(dir); 
		
	}
	
	public void init(OwnFileList list) {
		try {

			boolean createNew = !dir.isDirectory()
					|| dir.listFiles().length == 0;
			
			w = new IndexWriter(index, analyzer, createNew,
					IndexWriter.MaxFieldLength.UNLIMITED);
			w.setRAMBufferSizeMB(10);
			w.commit();

			if (!createNew) {
				IndexSearcher searcher = new IndexSearcher(index,true);
				IndexReader ir = searcher.getIndexReader();
				int size = ir.numDocs();
				long timestart = System.currentTimeMillis();
				logger.debug("Read "+size+" filehashes");
				FieldSelector fs = new FieldSelector() {
					private static final long serialVersionUID = 1L;

					public FieldSelectorResult accept(String fieldName) {
						if (fieldName.equals(FIELD_HASH)) {
							return FieldSelectorResult.LOAD;
						} else {
							return FieldSelectorResult.NO_LOAD;
						}
					}
				};
				for (int i = 0; i < size; i++) {
					Document doc = ir.document(i, fs);
					byte[] hashB = doc.getBinaryValue(FIELD_HASH);
					HashValue hash = HashValue.createHash(hashB);
					presentHashes.add(hash);
				}
				searcher.close();
				logger.debug("info time needed: "+(System.currentTimeMillis()-timestart));
			}
			index(list);
		} catch (FileNotFoundException fnfe) {
			logger.debug("file not found..", fnfe);
		} catch (Exception e) {
			logger.warn(e, e);
		}
	}

	
	
	
	public static boolean matches(String filename,long filesize) {
		String ending = GH.getFileEnding(filename).toLowerCase();
		return SUPPORTED_ENDINGS.contains(ending) && filesize <= MAX_TOTALSIZE ;

	}
	
	public synchronized void addPDFIfAbsent(File f,HashValue hashOfFile) throws IOException {
		if (exists(hashOfFile)) {
		//	logger.debug("File already present: "+f.getPath());
			return;
		}
		if (!matches(f.getName(),f.length())) {
			throw new IllegalArgumentException("Only text files allowed");
		}
		
		
		try {
			storeDocument(f, hashOfFile);
			presentHashes.add(hashOfFile);
		} catch (CorruptIndexException e) {
			logger.warn(e, e);
		} catch (IOException e) {
			logger.warn(e, e);
		}

	
	}
	
	private boolean exists(HashValue hash) {
		return presentHashes.contains(hash);
	}
	
	public synchronized Set<HashValue> search(Set<String> keys,Set<String> excludes,Collection<String> endings) {
		if (presentHashes.isEmpty()) { //if inverted Index is empty .. -> no results..
			return Collections.<HashValue>emptySet();
		}
	
		
		BooleanQuery bq = new BooleanQuery();
		for (String s : keys) {
			if (s.contains(" ")) {
				PhraseQuery pq = new PhraseQuery();
				for (String subterm: s.split(" ")) {
					pq.add(new Term(FIELD_CONTENT,subterm));	
				}
				bq.add(pq, BooleanClause.Occur.MUST);
			} else {
				bq.add(new TermQuery(new Term(FIELD_CONTENT, s)), BooleanClause.Occur.MUST);
			}
		}
		for (String s : excludes) {
			if (s.contains(" ")) {
				PhraseQuery pq = new PhraseQuery();
				for (String subterm: s.split(" ")) {
					pq.add(new Term(FIELD_CONTENT,subterm));	
				}
				bq.add(pq, BooleanClause.Occur.MUST_NOT);
			} else {
				bq.add(new TermQuery(new Term(FIELD_CONTENT, s)), BooleanClause.Occur.MUST_NOT);
			}
		}
		
		if (!endings.isEmpty()) {
			BooleanQuery equery = new BooleanQuery();
			for (String s: endings) {
				equery.add(new TermQuery(new Term(FIELD_ENDING, s)), BooleanClause.Occur.SHOULD);
			}
			bq.add(equery, BooleanClause.Occur.MUST);
		}
		
		Set<HashValue> found = new HashSet<HashValue>();
		try {
			IndexSearcher searcher = new IndexSearcher(index,true);
			TopScoreDocCollector collector = TopScoreDocCollector.create(25, false); // new TopDocCollector(10);
		    searcher.search(bq, collector);
		    ScoreDoc[] hits = collector.topDocs().scoreDocs;
		    
		    for (ScoreDoc sd: hits) {
		    	int docId = sd.doc;
		    	Document d = searcher.doc(docId);
		    	found.add(HashValue.createHash(d.getBinaryValue(FIELD_HASH)));
		    }
		    
		    searcher.close();
		    
		} catch(Exception e) {
			logger.warn(e,e);
		}
		
		return found;
	}
	
	public synchronized void stop() {
		if (w != null) {
			try {	
				if (job != null) {
					job.cancel();
				}
				w.commit();
				w.close();
				w = null;
			} catch (Exception e) {
				logger.error("Problem creating the FileListindex: "+e,e);
			}
		}
	}
	
	private void index(OwnFileList list) {
		if (job != null) {
			job.cancel();
			try {
				job.join();
			} catch (InterruptedException e) {
				logger.warn(e,e);
			}
		}
		job = new IndexTextFiles(list);
		job.schedule();
	}
	
	class IndexTextFiles extends Job {
		private final IOwnFileList list;
		
		public IndexTextFiles(IOwnFileList list) {
			super("Indexing Textfiles");
			this.list = list;
		}
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String debugcurrent = ""; 
			int priority = Thread.currentThread().getPriority();
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			try {
				List<FileListFile> pdfFiles = new ArrayList<FileListFile>();
				//FileList fl = list.getFileList();
				for (FileListFile file: list.getFileList().getRoot()) {
					if (matches(file.getName(),file.getSize())) {
						pdfFiles.add(file);
					}
				}
				monitor.beginTask("Indexing Textfiles", pdfFiles.size());
				logger.debug("Files total: "+pdfFiles.size());
				for (FileListFile file: pdfFiles) {
					File f = null;
					try {
						f = list.getFile(file.getTTHRoot());
					} catch (FilelistNotReadyException e) {
						throw new IllegalStateException(e);
					}

					if (f != null) {
						debugcurrent = file.getName() +"  "+file.getSize();
					//	logger.debug(debugcurrent);
						synchronized(TextIndexer.this) {
							if (monitor.isCanceled()) {
								break;
							}
							addPDFIfAbsent(f, file.getTTHRoot());
						}
					}
					monitor.worked(1);

				}

			} catch (Throwable e) {
				logger.warn(e + debugcurrent, e);
			} finally {
				Thread.currentThread().setPriority(priority);
				monitor.done();
				setCreated(true);
			}
			
			return Status.OK_STATUS;
		}
		
	}
	

	private void storeDocument(File f,HashValue hash) throws IOException {
		Reader r = null;
		try { 
			r = getReader(f);
		} catch (Exception ioe) {
			logger.debug("ioe -> file ignored: "+f.getName(),ioe);
			r = null;
		}
		try {
			Document doc = new Document();
			doc.add(new Field(FIELD_HASH, hash.getRaw(), Field.Store.YES));
			doc.add(new Field(FIELD_ENDING,GH.getFileEnding(f.getName()),Field.Store.YES,Index.ANALYZED));
			if (r != null) {
				doc.add( new Field( FIELD_CONTENT, r ));
			}
			w.addDocument(doc);
		} finally {
			GH.close(r);
		}
	}
	
	

	
	private Reader getReader(File file) throws IOException {
		FileInputStream input = new FileInputStream(file);
		String fileending = GH.getFileEnding(file.getName());
		if (fileending.equalsIgnoreCase("pdf")) {
			PDDocument pdfDocument = null;
			try {
				if (file.length() > MAX_TOTALSIZE/2) {
					System.gc();
				}
				pdfDocument = PDDocument.load(input,true);

				if (pdfDocument.isEncrypted()) {
					return null;
				}
				PDFTextStripper stripper = new PDFTextStripper();
				// create a writer where to append the text content.
				Reader reader;
				if (file.length() < MAX_RAMSIZE_FOR_PDF) {
					StringWriter writer = new StringWriter();
					stripper.writeText(pdfDocument, writer);
					String contents = writer.getBuffer().toString();
					reader =  new StringReader(contents);
				} else {
					final File f = new File(PI.getTempPath(),"index.tmp");
					FileWriter fw = new FileWriter(f);
					try {
						stripper.writeText(pdfDocument, fw);
					} finally {
						GH.close(fw);
					}
					FileReader fr = new FileReader(f) {
						@Override
						public void close() throws IOException {
							super.close();
							if (!f.delete()) {
								f.deleteOnExit();
							}
						}
					};
					reader = fr;
				}
				
				return reader;

			} finally {
				if (pdfDocument != null) {
					pdfDocument.close();
				}
			}
			
		} else {
			return new FileReader(file);
		}
	}

	
	public boolean isCreated() {
		return created;
	}

	private void setCreated(boolean created) {
		this.created = created;
	}
}
