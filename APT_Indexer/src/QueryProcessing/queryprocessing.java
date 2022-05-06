package QueryProcessing;
//Database
import MongoDBPackage.MongoDB;
import IndexerPackage.Indexer;
import opennlp.tools.stemmer.PorterStemmer;
import org.bson.Document;

import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class queryprocessing {
    private MongoDB database;
    private PorterStemmer porterStemmer ;//stemmer
    private static HashSet<String> stopWords ;
    private static HashSet<String> ImportantWords;
    public queryprocessing()
    {

        database=new MongoDB("Search_Engine","Indexer");
        porterStemmer = new PorterStemmer();
        stopWords=Indexer.getStopWords();
        ImportantWords=Indexer.getImportantword();
    }
    public  Vector<String> query_process(String QP_str,Vector<Vector<JSONObject>>resultorginal,Vector<Vector<JSONObject>> resultforms) throws JSONException {

        String[] words = (QP_str).toLowerCase().split("\\s");//splits the string based on whitespace
        Vector<String>finalword=new Vector<String>(1);
        Vector<JSONArray>docarr=new Vector<JSONArray>(1);
        for (int i = 0; i < words.length; i++)
        {
            if (!ImportantWords.contains(words[i]))
            {
                words[i] = words[i].replaceAll("[^a-zA-Z0-9]", " ");
                //System.out.println(words[i]);
                String[] subwords = words[i].split("\\s");//splits the string based on whitespace
                for (String sw : subwords)
                {
                    if (stopWords.contains(sw))
                    {
                        continue;
                    }
                    String stemw = porterStemmer.stem(sw);

                    Document DBresult = database.collection.find(new Document("_id", stemw)).first();
                    if (DBresult != null) {
                       // System.out.println(stemw);
                        JSONObject obj = new JSONObject(DBresult.toJson());
                        JSONArray arr = obj.getJSONArray("DOC");
                        finalword.add(sw);
                        docarr.add(arr);
                    }
                }
            }
            else{
                String stemw = porterStemmer.stem(words[i]);
                Document DBresult = database.collection.find(new Document("_id", stemw)).first();
                if (DBresult != null) {
                    JSONObject obj = new JSONObject(DBresult.toJson());
                    JSONArray arr = obj.getJSONArray("DOC");
                    docarr.add(arr);
                    finalword.add(words[i]);
                }
            }

        }
//            for(int k=0;k<docarr.size();k++)
//                System.out.println("docarr:" + docarr.get(k));
        query_process_work(finalword,docarr,resultorginal,resultforms);
        return finalword;
    }
    private void query_process_work(  Vector<String>words, Vector<JSONArray> docarr, Vector<Vector<JSONObject>> resultorginal,Vector<Vector<JSONObject>>  resultforms) throws JSONException {
//===================================================================== all documents of all word in Query
        Vector<JSONObject> temporginal = new Vector<JSONObject>(docarr.get(0).length());/////////////

        Vector<JSONObject> tempform = new Vector<JSONObject>(docarr.get(0).length());
        for (int i = 0; i < docarr.get(0).length(); i++)
        {
//            temp.add(docarr[0].getJSONObject(i));
            boolean orginal_doc0 = docarr.get(0).getJSONObject(i).has(words.get(0)); ///in case if not
            if (orginal_doc0 )
                temporginal.add(docarr.get(0).getJSONObject(i));
            else
                tempform.add(docarr.get(0).getJSONObject(i));

            String url = docarr.get(0).getJSONObject(i).getString("url");
            boolean comm =true;
            boolean orginal =true;
            for (int j = 1; j < docarr.size(); j++)
            {
                comm = false;
                orginal=false;
                int k;
                for ( k = 0; k < docarr.get(j).length(); k++)
                {
                    comm = false;
                    orginal=false;
                    if ((url.equals(docarr.get(j).getJSONObject(k).getString("url"))))
                    {
                        //vaild url

                        comm = true;
                        if (docarr.get(j).getJSONObject(k).has(words.get(j)))
                        {
                            orginal = true;
                        }

                        break;
                    }
                }///has document
                if (!comm)
                    break;
                else {
                    if (orginal &&orginal_doc0) {
                        temporginal.add(docarr.get(j).getJSONObject(k));
                    }
                    else {

                        tempform.add(docarr.get(j).getJSONObject(k));
                    }
                }
            }
            /////common for all words
            if (comm)
            {
                if (temporginal.size()==words.size())
                {
                    resultorginal.add(new Vector<JSONObject>( temporginal));
                }
                else
                {
                    tempform.addAll(temporginal);
                    resultforms.add(new Vector<JSONObject>(tempform));
                }
            }
            tempform.clear();
            temporginal.clear();
        }
//        for(int m=0;m<resultorginal.size();m++)
//            for(int k=0;k<(resultorginal.get(m)).size();k++) {
//                System.out.println("Orignal:" + resultorginal.get(m).get(k));
//                System.out.println(m);
//            }
//        for(int m=0;m<resultforms.size();m++)
//            for(int k=0;k<(resultforms.get(m)).size();k++) {
//                System.out.println("steming:" + resultforms.get(m).get(k));
//                System.out.println(m);}


    }
}
