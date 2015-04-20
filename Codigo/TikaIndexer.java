package datos.gob.mx;

/*
 #1 Change to true to see all text
 #2 Which metadata fields are textual
 #3 List all mime types handled by Tika
 #4 Create Metadata for the file
 #5 Open the file
 #6 Automatically determines file type
 #7 Extracts metadata and body text
 #8 Setup ParseContext
 #9 Does all the work!
 #10 Index body content
 #11 Index metadata fields
 #12 Append to contents field
 #13 Separately store metadata fields
 #14 Index file path
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.config.TikaConfig;
import org.xml.sax.ContentHandler;

public class TikaIndexer extends Indexer {

	static Set<String> textualMetadataFields = new HashSet<String>();
	static {
		textualMetadataFields.add(TikaCoreProperties.TITLE.getName());
		textualMetadataFields.add(TikaCoreProperties.COMMENTS.getName());
		textualMetadataFields.add(TikaCoreProperties.KEYWORDS.getName());
		textualMetadataFields.add(TikaCoreProperties.DESCRIPTION.getName());
		textualMetadataFields.add(TikaCoreProperties.KEYWORDS.getName());
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException("Usage: java "
					+ TikaIndexer.class.getName() + " <index dir> <data dir>");
		}

		TikaConfig config = TikaConfig.getDefaultConfig();
		System.out.println("Mime type parser:" + config.getParser());

		String indexDir = args[0];
		String dataDir = args[1];

		long start = new Date().getTime();
		TikaIndexer indexer = new TikaIndexer(indexDir);
		int numIndexed = indexer.index(dataDir, null);
		indexer.close();
		long end = new Date().getTime();

		System.out.println("Indexing " + numIndexed + " files took "
				+ (end - start) + " milliseconds");
	}

	public TikaIndexer(String indexDir) throws IOException {
		super(indexDir);
	}

	protected Document getDocument(File f) throws Exception {

		Metadata metadata = new Metadata();
		metadata.set(Metadata.RESOURCE_NAME_KEY, f.getName());

		InputStream is = new FileInputStream(f);
		Parser parser = new AutoDetectParser();
		ContentHandler handler = new BodyContentHandler();
		ParseContext context = new ParseContext();
		context.set(Parser.class, parser);

		try {
			parser.parse(is, handler, metadata, new ParseContext());
		} finally {
			is.close();
		}

		Document doc = new Document();

		doc.add(new StringField("fullpath", f.getPath(), Field.Store.YES));
		doc.add(new StringField("filename", f.getCanonicalPath(),
				Field.Store.YES));
		doc.add(new LongField("modified", f.lastModified(), Field.Store.NO));

		doc.add(new TextField("contents", new BufferedReader(
				new InputStreamReader(new FileInputStream(f),
						StandardCharsets.UTF_8))));

		doc.add(new TextField("contents", handler.toString(), Field.Store.NO));
		for (String name : metadata.names()) {
			String value = metadata.get(name);
			if (textualMetadataFields.contains(name)) {
				doc.add(new TextField("contents", value, Field.Store.NO));
			}
			doc.add(new TextField(name, value, Field.Store.YES));
		}

		return doc;
	}
}
