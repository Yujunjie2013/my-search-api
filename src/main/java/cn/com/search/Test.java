package cn.com.search;

import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.StringReader;

public class Test {
    public static void main(String[] args) {
        String str = "南京市长江大桥";
        print(str);
    }

    public static void print(String str) {
        try {
            IKSegmenter ik = new IKSegmenter(new StringReader(str), false);
            Lexeme l = null;
            while ((l = ik.next()) != null) {
                System.out.println(l.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
