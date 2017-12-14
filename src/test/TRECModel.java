package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class TRECModel {
	IndexReader reader = null;
	public static void main(String[] args) throws DocumentException, IOException, ParseException{
		String docsPath = "/Users/qingping/TREC/Data/pmc-text-00";
		String topicsPath = "/Users/qingping/TREC/Data/topics2014.xml";
		String resultPath = "result.txt";
		int number = 1000;
		float k1 = (float)1.2, b = (float)0.9;
		TRECModel tm = new TRECModel();
		tm.MakeIndex(null, "Lucene_00.index", true);
		tm.QueryTopicsBM25(topicsPath, resultPath, number, k1, b, "test");
	}
	
	/**
	 * @param docsPath 
	 * @param indexPath index are saved to indexPath.
	 * @param create 
	 * @throws IOException
	 */
	public void MakeIndex(String docsPath, String indexPath, boolean create) throws IOException{
		if(docsPath != null){
			IndexFile.IndexProcess(indexPath, docsPath, create);
		}
		reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
	}
	
	public void QueryTopicsBM25(String topicsPath, String resultPath, int nubmer, 
			float k1, float b, String runName) throws DocumentException, IOException, ParseException{
		List<Element> topics = GetTopics(topicsPath);
		if(reader == null){
			System.out.println("The variable reader is not initialized!");
			System.exit(0);
		}

		QueryParser parser = new QueryParser("contents", new StandardAnalyzer());
		IndexSearcher searcher = new IndexSearcher(reader);
		//set parameters of BM25 model
		searcher.setSimilarity(new BM25Similarity(k1, b));
		
		FileWriter fw = new FileWriter(new File(resultPath));
		
		for(Element topic : topics){
			String typeStr = topic.attributeValue("type");
			String IDStr = topic.attributeValue("number");
			String queryStr = topic.element("summary").getText().trim();
			System.out.println(typeStr);
			System.out.println(queryStr);
			
			TopDocs results = searcher.search(parser.parse(queryStr), nubmer);
			ScoreDoc[] hits = results.scoreDocs;
			System.out.println(hits.length);
			for(int i = 0; i < hits.length; i++){
				Document doc = searcher.doc(hits[i].doc);
				//<查询ID> Q0 <文档ID> <文档排序> <文档评分> <系统ID>
				String docPath = doc.get("path");
				String docID = docPath.substring(docPath.lastIndexOf('/')+1, docPath.lastIndexOf('.'));
				String resStr = IDStr+" Q0 " + docID + " " + (i+1) + " " + hits[i].score + " " + runName + "\n";
//				System.out.print(resStr);
				fw.write(resStr);
			}
		}
		fw.close();
	}
	
	public List<Element> GetTopics(String topicsPath) throws DocumentException{
		SAXReader reader = new SAXReader();
		org.dom4j.Document document = reader.read(new File(topicsPath));
		Element root = document.getRootElement();
		List<Element> topics = root.elements();
		return topics;
	}
}
