package model;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Command {
	public static String exeCmd(String commandStr) {
		BufferedReader br = null;
		try {
			Process p = Runtime.getRuntime().exec(commandStr);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
//			System.out.println(sb.toString());
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static void main(String[] args) {
		String commandStr = "perl sample_eval.pl qrels2014.txt test-1.2-0.75-result.txt";

		String res = Command.exeCmd(commandStr);
		for(String s : res.split("\n")[1].split("\t")){
			System.out.println(s);
		}
		System.out.println(res.split("\n")[1].split("\t")[0]);
		System.out.println(Float.parseFloat(res.split("\n")[1].split("\t")[4]));
	}
}
