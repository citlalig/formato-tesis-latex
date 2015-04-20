package datos.gob.mx;

/*
 #1 Parse provided index directory
 #2 Parse provided query string
 #3 Open index
 #4 Parse query
 #5 Search index
 #6 Write search stats
 #7 Retrieve matching document
 #8 Display filename
 #9 Close IndexSearcher
 */

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

/**
 * This code was originally written for Erik's Lucene intro java.net article
 * and modified by Citlali Garcia to meet the application needs.
 */
public class Searcher {

	public static void main(String[] args) throws IllegalArgumentException,
			IOException, ParseException {
		if (args.length != 2) {
			throw new IllegalArgumentException("Usage: java "
					+ Searcher.class.getName() + " <index dir> <query>");
		}

		String indexDir = args[0];
		String query = args[1];
		search(indexDir, query);
	}

	public static void search(String indexDir, String q) throws IOException,
			ParseException {

		Directory dir = FSDirectory.open(new File(indexDir));
		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher is = new IndexSearcher(reader);

		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		QueryParser parser = new QueryParser(Version.LUCENE_4_9, "contents",
				analyzer);

		Query query = parser.parse(q);
		long start = System.currentTimeMillis();
		TopDocs hits = is.search(query, 10);
		long end = System.currentTimeMillis();

		System.err.println("Found " + hits.totalHits + " document(s) (in "
				+ (end - start) + " milliseconds) that matched query '" + q
				+ "':");

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = is.doc(scoreDoc.doc);
			System.out.println(doc.get("fullpath"));
		}
		reader.close();
	}
}
