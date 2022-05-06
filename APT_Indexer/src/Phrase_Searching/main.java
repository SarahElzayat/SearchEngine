package Phrase_Searching;

//import Phrase_Searching.queryprocessing;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

public class main {
    public static void main(String[] args) throws JSONException {
        Phrase_sreach pp=new Phrase_sreach();
       Vector<Vector<JSONObject>> urlsofPhase =pp.Phraseprocess(" three container adaptor ");
        for(int m=0;m<urlsofPhase.size();m++)
            for(int k=0;k<(urlsofPhase.get(m)).size();k++) {
                System.out.println("Phase" + urlsofPhase.get(m).get(k));
                System.out.println(m);
            }
    }
}
