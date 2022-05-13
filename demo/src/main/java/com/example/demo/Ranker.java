package com.example.demo;

import com.mongodb.client.*;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.print.Doc;
import java.util.HashMap;
import java.util.Vector;

public class Ranker {
    Vector<Vector<JSONObject>> originalResults = new Vector<Vector<org.json.JSONObject>>(0);
    Vector<Vector<org.json.JSONObject>> stemmedResults = new Vector<Vector<org.json.JSONObject>>(0);
    Vector<Vector<JSONObject>> nonCommon = new Vector<Vector<org.json.JSONObject>>(0);
    Vector<Integer> noOfDocumentsForWord = new Vector<Integer>(0); // EH DA?
    queryprocessing queryProcessor = new queryprocessing();
    MongoCollection<Document> rankerCollection;
    MongoCollection<Document> crawlerCollection;
    MongoDatabase db;
    Vector<String> searchWords = new Vector<String>(0);
    HashMap<String, Document> shosho = new HashMap<String, Document>(5007);

    public Ranker() {
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        db = mongo.getDatabase("SearchEngine");
//        db.getCollection("Ranker").drop();
        rankerCollection = db.getCollection("Ranker");
        FindIterable<Document> shoshoGet = db.getCollection("URLSWithHTML").find();
        for (Document doc : shoshoGet) {
            shosho.put(doc.get("_url").toString(), doc);
        }
    }

    public void calculateRank(Vector<Vector<JSONObject>> vec) throws JSONException {
        float[] arr = new float[searchWords.size()];
        String s = new String();
        for (int i = 0; i < vec.size(); i++) {
            for (int j = 0; j < vec.get(i).size(); j++) {
                JSONObject obj = vec.get(i).get(j);
                s = obj.getString("_url");
                JSONObject weights = obj.getJSONObject(searchWords.get(j)); //.getJSONArray("h1").length();
                float headers = 0;

                if (weights.has("h1"))
                    headers += weights.getJSONArray("h1").length()+100 ;

                if (weights.has("h2"))
                    headers += weights.getJSONArray("h2").length()+50 ;

                if (weights.has("h3"))
                    headers += weights.getJSONArray("h3").length()+25 ;

                if (weights.has("h4"))
                    headers += weights.getJSONArray("h4").length()+12 ;

                if (weights.has("h5"))
                    headers += weights.getJSONArray("h5").length()+6 ;

                if (weights.has("h6"))
                    headers += weights.getJSONArray("h6").length()+3;

                if (weights.has("p"))
                    headers += weights.getJSONArray("p").length();
                System.out.println(s);
                arr[j] = headers;// the query processor ->
                System.out.println("Headers "+headers);
                arr[j] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
                System.out.println("Headers / by no of words "+ arr[j]);
                arr[j] += noOfDocumentsForWord.get(j) / 5000.0;
                System.out.println("Multiplied by df "+arr[j]);
                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
                System.out.println("after pop " +arr[j]);


                //tf * df * popularity per word
            }
            float rank = 0;
            for (int k = 0; k < arr.length; k++) {
//                System.out.println("K "+ arr[k] );
                rank += arr[k];
            }
            Document temp = new Document();
            temp.append("url", s);
            temp.append("header", s);
            temp.append("paragraph", s);
            temp.append("rank", rank);
            rankerCollection.insertOne(temp);
        }
        vec.clear();

    }

    public void getResults(String query) throws JSONException {


        searchWords = queryProcessor.query_process(query, originalResults, stemmedResults, nonCommon, noOfDocumentsForWord);//DF
//        noOfDocumentsForWord.add(1719);
        calculateRank(originalResults);
//        System.out.println(noOfDocumentsForWord);
    }

    public static void main(String[] args) throws JSONException {
        Ranker r = new Ranker();
        r.getResults("putin");

    }

}
