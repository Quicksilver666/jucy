package helpers;

import java.io.IOException;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class InvertedIndex<V> {

	private final ISubstringMapping2<V> mapping;
	
	private final SimpleAnalyzer analyzer = new SimpleAnalyzer();
	private Directory index = new RAMDirectory();

	private IndexWriter w;
	
	public InvertedIndex(ISubstringMapping2<V> mapping) {
		this.mapping = mapping;
		try {
			w = new IndexWriter(index, analyzer, true,
		        IndexWriter.MaxFieldLength.UNLIMITED);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public synchronized void put(V toMap) {
		Document doc = new Document();
		doc.add(new Field("file", mapping.getMappingString(toMap), Field.Store.YES, Field.Index.ANALYZED));
		try {
			w.addDocument(doc);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) throws IOException, ParseException {
	    // 0. Specify the analyzer for tokenizing text.
	    //    The same analyzer should be used for indexing and searching
		SimpleAnalyzer analyzer = new SimpleAnalyzer();

	    // 1. create the index
	    Directory index = new RAMDirectory();

	    // the boolean arg in the IndexWriter ctor means to
	    // create a new index, overwriting any existing index
	    IndexWriter w= new IndexWriter(index, analyzer, true,
		        IndexWriter.MaxFieldLength.UNLIMITED);
	    addDoc(w, "Lucene-in.Action");
	    addDoc(w, "Lucene for Dummies");
	    addDoc(w, "Managing Gigabytes");
	    addDoc(w, "The Art of Computer Science");
	    w.close();

	    // 2. query
	    String querystr = args.length > 0 ? args[0] : "lucene-in";

	    // the "title" arg specifies the default field to use
	    // when no field is explicitly specified in the query.
	    Query q = new QueryParser("title", analyzer).parse(querystr);

	    // 3. search
	    int hitsPerPage = 10;
	    IndexSearcher searcher = new IndexSearcher(index);
	    TopDocCollector collector = new TopDocCollector(hitsPerPage);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
	    // 4. display results
	    System.out.println("Found " + hits.length + " hits.");
	    for(int i=0;i<hits.length;++i) {
	      int docId = hits[i].doc;
	      Document d = searcher.doc(docId);
	      System.out.println((i + 1) + ". " + d.get("title"));
	    }

	    // searcher can only be closed when there
	    // is no need to access the documents any more. 
	    searcher.close();
	  }

	  private static void addDoc(IndexWriter w, String value) throws IOException {
	    Document doc = new Document();
	    doc.add(new Field("title", value, Field.Store.YES, Field.Index.ANALYZED));
	    w.addDocument(doc);
	  }

	
}
