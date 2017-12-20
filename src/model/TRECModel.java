package model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

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
	IndexReader reader = null; //索引
	int resNum;					//返回多少个索引结果
	String runName;				//
	String resPath;				//结果存储的路径

	public static void main(String[] args) throws DocumentException, IOException, ParseException{
//		String docsPath = "F:/�о���/��һ/��һ��ѧ��/�γ̽���/4�ִ���Ϣ����/��ҵ/pmc-text-01/00";
//		String topicsPath = "F:/�о���/��һ/��һ��ѧ��/�γ̽���/4�ִ���Ϣ����/��ҵ/topics2014.xml";
		String docsPath = "/Users/qingping/TREC/Data";
		String topicsPath = "/Users/qingping/TREC/topics2014.xml";
		String qrelsPath = "qrels2014.txt";
		String indexPath = "Lucene_TRECALL.index";
		String runName = "GridSearch";
		int number = 1000;
		
		TRECModel tm = new TRECModel();
		//重新建立索引。如果不用重新建立索引，只需要将docPath改为null即可
		tm.setResNum(number);
		tm.setRunName(runName);
		tm.MakeIndex(null, indexPath, true);
		float k1 = (float)1.5, b = (float)0.8;
//		tm.QueryTopicsBM25(topicsPath, k1, b, null);
		double[] k1s = TRECFloatRange(3, 5, 0.2);
		double[] bs = TRECFloatRange(0.6, 1, 0.05);
		//利用grid search得到最优的参数
		Entry<Float, Float> params = tm.GridSearchBM25(topicsPath, qrelsPath, k1s, bs);
		
//		//得出topics2014训练的结果
		k1 = params.getKey();
		b = params.getValue();
//		k1 = (float) 3.2;
//		b=(float) 0.8;
		String topics2015Path = "/Users/qingping/TREC/topics2015A.xml";
		String qrels2015Path = "qrels-treceval-2015.txt";
		tm.setRunName("search2015+type");
		tm.QueryTopicsBM25(topics2015Path, k1, b, null);
		float ndcg = tm.infNDCGOfQueryResult(qrels2015Path, tm.getResPath());
		System.out.println("Topics 2015(k1="+k1+",b="+b+"): NDCG="+ndcg);
	}
	
	/**
	 * 基于GridSearch方法，2014的topics数据来训练最优的出最优的参数。
	 * 	对k1和b的候选值进行两两组合。比如k1s={}
	 * 
	 * @param topicsPath			topicsPath
	 * @param qrelsPath				结果的路径
	 * @param k1s					k1的候选值
	 * @param bs					b的候选值
	 * @return						最优的参数
	 * @throws DocumentException
	 * @throws IOException
	 * @throws ParseException
	 */
	public Entry<Float, Float> GridSearchBM25(String topicsPath, String qrelsPath, double[] k1s, double[] bs) throws DocumentException, IOException, ParseException{
		System.out.println("======================Grid Search For Parameters======================");
		String gridResPath = this.runName+"-grid-tmp.txt";
		double bestK1 = 1.2, bestB = 0.75, bestNDCG = -1;
		for(int i = 0; i < k1s.length; i++){
			for(int j = 0; j < bs.length; j++){
				this.QueryTopicsBM25(topicsPath, (float)k1s[i], (float)bs[j], gridResPath);
				float infNDCG = this.infNDCGOfQueryResult(qrelsPath, gridResPath);
				if(bestNDCG == -1 || bestNDCG < infNDCG){
					bestNDCG = infNDCG;
					bestK1 = k1s[i];
					bestB = bs[j];
				}
				System.out.println("current: k1="+k1s[i]+", b="+bs[j]+", infNDCG="+infNDCG+", bestNDCG="+bestNDCG);
			}
		}
		System.out.println("===============================REPORT:===============================");
		System.out.println("best k1="+bestK1+", best b="+bestB+", bestNDCG="+bestNDCG);
		System.out.println("==========================Grid Search Over!==========================");
		return new AbstractMap.SimpleEntry((float)bestK1, (float)bestB);
	}
	/**
	 * 利用命令行命令，得出结果的评价NDCG
	 * 
	 * @param qrelsPath ground truth
	 * @param resPath	结果的路径
	 * @return
	 */
	public float infNDCGOfQueryResult(String qrelsPath, String resPath){
//		perl sample_eval.pl qrels2014.txt test-1.2-0.75-result.txt
		String cmdStr = "perl sample_eval.pl "+qrelsPath+" "+resPath;
		String res = Command.exeCmd(cmdStr);
		
		//将结果显示的infNDCG这一行得出
		return Float.parseFloat(res.split("\n")[1].split("\t")[4]);
	}
	/**
	 * 功能类似Python中的range和xrange，返回一个[s, t]以d为间隔的double数组
	 * @param s
	 * @param t
	 * @param d
	 * @return
	 */
	public static double[] TRECFloatRange(double s, double t, double d){
		double[] res = new double[((int)Math.floor((t-s)/d)+1)];
		int idx = 0;
		for(;s <= t; s+=d){
			res[idx] = s;
			idx ++;
		}
		return res;
	}
	/**
	 * @param docsPath 
	 * @param indexPath index are saved to indexPath.
	 * @param create 
	 * @throws IOException
	 */
	public void MakeIndex(String docsPath, String indexPath, boolean create) throws IOException{
		System.out.println(indexPath);
		System.out.println(docsPath);
		if(docsPath != null){
			IndexFile.IndexProcess(indexPath, docsPath, create);
		}
		reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
	}
	
	/**
	 * 基于BM25(k1, b)模型，每个topics在索引中检索出1000个文档。在非grid search的调用中，令gridResPath=null
	 * @param topicsPath			topics存储路径
	 * @param k1					参数k1
	 * @param b						参数b
	 * @param gridResPath			如果不是grid search的话，该参数为null，存储路径为runName-k1-b.txt；在grid search中结果存储在gridResPath
	 * @throws DocumentException	
	 * @throws IOException
	 * @throws ParseException
	 */
	public void QueryTopicsBM25(String topicsPath, float k1, float b, String gridResPath) 
			throws DocumentException, IOException, ParseException{
		List<Element> topics = GetTopics(topicsPath);
		if(reader == null){
			System.out.println("The variable reader is not initialized!");
			System.exit(0);
		}

		QueryParser parser = new QueryParser("contents", new StandardAnalyzer());
		IndexSearcher searcher = new IndexSearcher(reader);
		//set parameters of BM25 model
		searcher.setSimilarity(new BM25Similarity(k1, b));
		FileWriter fw;
		if(null == gridResPath){
			fw = new FileWriter(new File(this.runName+"-"+k1+"-"+b+".txt"));
			this.setResPath(this.runName+"-"+k1+"-"+b+".txt");
		}
		else{
			fw = new FileWriter(new File(gridResPath));
			this.setResPath(gridResPath);
		}
		
		for(Element topic : topics){
			String typeStr = topic.attributeValue("type");
			String IDStr = topic.attributeValue("number");
			String queryStr = topic.element("summary").getText().trim();

			//add typeStr to queryStr
			for(int i = 0; i < 1; i++)
				queryStr += " "+typeStr;
			
//			System.out.println("--------------------------------------------------------");
//			System.out.println(typeStr);
//			System.out.println("before remove stemming:"+queryStr);
			queryStr = DataPreprocess.remove_stemming(queryStr);//ȥ��ͣ�ôʺͻ�ԭ�ʸ�
//			System.out.println("after remove stemming:"+queryStr);
			
			TopDocs results = searcher.search(parser.parse(queryStr), this.resNum);
			ScoreDoc[] hits = results.scoreDocs;
//			System.out.println(hits.length);
			for(int i = 0; i < hits.length; i++){
				Document doc = searcher.doc(hits[i].doc);
				//<查询ID> Q0 <文档ID> <文档排序> <文档评分> <系统ID>
				String docPath = doc.get("path");
				
				String docID = docPath.substring(Math.max(docPath.lastIndexOf('/'), 
						docPath.lastIndexOf('\\'))+1, docPath.lastIndexOf('.'));
				String resStr = IDStr+" Q0 " + docID + " " + (i+1) + " " + hits[i].score + " " + this.runName + "\n";
//				System.out.print(resStr);
				fw.write(resStr);
			}
		}
		fw.close();
	}
	
	/**
	 * 从topics的xml文件中，获取每个topic
	 * @param topicsPath
	 * @return
	 * @throws DocumentException
	 */
	public List<Element> GetTopics(String topicsPath) throws DocumentException{
		SAXReader reader = new SAXReader();
		org.dom4j.Document document = reader.read(new File(topicsPath));
		Element root = document.getRootElement();
		List<Element> topics = root.elements();
		return topics;
	}

	public String getResPath(){
		return this.resPath;
	}
	public void setResPath(String resPath){
		this.resPath = resPath;
	}
	public void setResNum(int resNum) {
		this.resNum = resNum;
	}

	public void setRunName(String runName) {
		this.runName = runName;
	}
}
