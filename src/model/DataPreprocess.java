package model;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.*;

import javax.print.DocFlavor;
import javax.xml.transform.Templates;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
public class DataPreprocess {
	
	public static String batch_process(String path) throws InterruptedException {
		
		File file = new File(path);
		File[] files = file.listFiles();
		String doc_path = null;
		for(File temp: files){
			System.out.println(temp);
			File[] doc = temp.listFiles();
			for(File doc_temp : doc){
				System.out.println(doc_temp);
				doc_path = doc_temp.toString();
				String content = pre_process(doc_path);
				System.out.println(content.substring(0, 100));
			}
		}
		return null;
		
		
	}
	
	public static String remove_stemming(String content) {
		StringBuilder sb_reform = new StringBuilder();
		Analyzer analyzer = new StandardAnalyzer();
		TokenStream ts = analyzer.tokenStream("", new StringReader(content));//ȥ��ͣ�ô�
		ts = new PorterStemFilter(ts);//��ԭ�ʸ�
		CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
		try {
			ts.reset();
			while (ts.incrementToken()){
				sb_reform.append(charTermAttribute.toString() + " ");
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb_reform.toString();
	}
	
	
	public static String pre_process (String doc_path) {
		//ȥ��<>��<>�ڵ�����
		String res = null;
		File doc_file = new File(doc_path);
		try {
			StringBuilder sb = new StringBuilder();
			Reader reader = new InputStreamReader(new FileInputStream(doc_file));
			char[] c = new char[30];
			int length = 0;
			while ((length = reader.read(c)) != -1){
				if (length == c.length) {
					sb.append(String.valueOf(c));
				}
				else{
					int i = 0;
					while(i < length){
						sb.append(c[i]);
						i++;
					}
					
				}	
			}
			res = sb.toString().replaceAll("\\<.*?\\>", " ");
			res = res.replaceAll("\\s+", " ");
			res = res.trim();
//			res = sb.toString().replaceAll("<.*?>", " ");
//			System.out.println(res);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//ȥ��ͣ�ôʡ���ԭ�ʸ�
		StringBuilder sb_reform = new StringBuilder();
		Analyzer analyzer = new StandardAnalyzer();
		TokenStream ts = analyzer.tokenStream("", new StringReader(res));//ȥ��ͣ�ô�
		ts = new PorterStemFilter(ts);//��ԭ�ʸ�
		CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
		try {
			ts.reset();
			while (ts.incrementToken()){
				sb_reform.append(charTermAttribute.toString() + " ");
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb_reform.toString();
	
	}
}
