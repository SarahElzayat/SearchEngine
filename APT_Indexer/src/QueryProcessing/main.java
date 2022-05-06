package QueryProcessing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

public class main {
    public static void main(String[] args) throws JSONException {

       queryprocessing d=new queryprocessing();
        Vector<Vector<JSONObject>> resultorginal=new Vector<Vector<JSONObject>>(1);
        Vector<Vector<JSONObject>>  resultforms=new Vector<Vector<JSONObject>>(1);
       d.query_process("first class" , resultorginal, resultforms); //////////////should remove any space before query use the trim() method.
            System.out.println(resultorginal.size());
                for(int m=0;m<resultorginal.size();m++)
            for(int k=0;k<(resultorginal.get(m)).size();k++) {
                System.out.println("Orignal:" + resultorginal.get(m).get(k));
                System.out.println(m);
            }
        for(int m=0;m<resultforms.size();m++)
            for(int k=0;k<(resultforms.get(m)).size();k++) {
                System.out.println("steming:" + resultforms.get(m).get(k));
                System.out.println(m);}
    }
}