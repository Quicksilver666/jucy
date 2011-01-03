package uc.files.filelist;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import helpers.IFilter;
import helpers.ISearchMap;
import helpers.ISubstringMapping2;


public class InvertedIndex<V> implements ISearchMap<V> {

	private static Logger logger = LoggerFactory.make();
	
	private final ISubstringMapping2<V> mapping;

	
	private final List<V> itemByIndex = new ArrayList<V>();
	
	private final SimpleAnalyzer analyzer = new SimpleAnalyzer();
	private final Directory index = new RAMDirectory();
	

	private IndexWriter w;
	
	
	public InvertedIndex(ISubstringMapping2<V> mapping) {
		this.mapping = mapping;
	}
	

	
	
	public synchronized void put(V toMap) {
		if (w == null) {
		    try {
				w = new IndexWriter(index, analyzer, true,
				        IndexWriter.MaxFieldLength.UNLIMITED);
			} catch (Exception e) {
				logger.error(e, e);
			}
		}
		int pos = itemByIndex.size();
		itemByIndex.add(toMap);
		Document doc = new Document();
		doc.add(new Field("title", mapping.getMappingString(toMap), Field.Store.NO, Field.Index.ANALYZED));
		doc.add(new Field("pos",""+pos,Field.Store.YES,Field.Index.NO ));
		
		try {
			w.addDocument(doc);
		} catch (CorruptIndexException e) {
			logger.warn(e, e);
		} catch (IOException e) {
			logger.warn(e, e);
		}
		
	}

	public Set<V> search(String s) {
		return search(Collections.singleton(s));
	}

	public Set<V> search(Set<String> searchStrings) {
		return search(searchStrings,Collections.<String>emptySet(),new IFilter<V>() { //empty filter..
			public boolean filter(V item) {
				return true;
			}
			
			public Set<V> mapItems(Set<V> nodeItems) {
				return nodeItems;
			}});
	}
	

	public synchronized Set<V> search(Set<String> searchStrings,
			Set<String> excludes, IFilter<V> filter) {
		if (itemByIndex.isEmpty()) { //if inverted Index is empty .. -> no results..
			return Collections.<V>emptySet();
		}
		if (w != null) {
			try {
				w.close();
				w = null;
			} catch (Exception e) {
				logger.error("Problem creating the FileListindex: "+e,e);
			}
		}
		
		
		Set<V> current = null;
		//do the searches..
		for (String s: searchToQueryStrings(searchStrings)) {
			Set<V> found = getMatching(s);
			found = filter.mapItems(found);
			if (current != null) {
				found.retainAll(current);
			} else {
				//remove filtered items
				for (Iterator<V> it = found.iterator(); it.hasNext();) {
					if (!filter.filter(it.next())) {
						it.remove();
					}
				}
				
			}
			current = found;
			if (current.isEmpty()) {
				break;
			}
		}

		if (current == null) {
			current = Collections.<V>emptySet();
		}
		//remove all excludes ...
		for (String exclude : searchToQueryStrings(excludes)) {
			Set<V> found = getMatching(exclude);
			found = filter.mapItems(found);
			current.removeAll(found);
		}
		
		return current;
		
	}
	
	private Set<V> getMatching(String querystr) {
		Set<V> res = new HashSet<V>();
		try {
			Query q = new QueryParser(Version.LUCENE_CURRENT,"title", analyzer).parse(querystr);
			IndexSearcher searcher = new IndexSearcher(index,true);
			TopScoreDocCollector collector = TopScoreDocCollector.create(itemByIndex.size(),false);
		//	TopDocCollector collector = new TopDocCollector(itemByIndex.size());
		    searcher.search(q, collector);
		    ScoreDoc[] hits = collector.topDocs().scoreDocs;
		    
		    for (ScoreDoc sd: hits) {
		    	int docId = sd.doc;
		    	Document d = searcher.doc(docId);
		    	int pos = Integer.parseInt(d.get("pos"));
		    	res.add(itemByIndex.get(pos));
		    }
		    
		    searcher.close();
	
		} catch(Exception e) {
			logger.warn(e+": "+querystr,e);
		}
		
		return res;
	}
	
	
	
	private static List<String> searchToQueryStrings(Collection<String> searchstrings) {
		List<String> queryStrings = new ArrayList<String>();
		for (String searchstr: searchstrings) {
			for (String s: searchstr.split("\\W")) {
				if (s.length() > 2) {
					queryStrings.add( s+"*"  );
				}
			}
			
		}
		
		return queryStrings;
	}

}
