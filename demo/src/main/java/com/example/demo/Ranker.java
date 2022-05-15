package com.example.demo;

import com.mongodb.client.*;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import javax.print.Doc;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class Ranker {
    //Urls
    Vector<Vector<JSONObject>> originalResults = new Vector<Vector<org.json.JSONObject>>(0);
    Vector<Vector<org.json.JSONObject>> stemmedResults = new Vector<Vector<org.json.JSONObject>>(0);
    Vector<Vector<JSONObject>> nonCommon = new Vector<Vector<org.json.JSONObject>>(0);
    //DF
    Vector<Integer> DF = new Vector<Integer>(0); // EH DA?
    //Snipptes
    Vector<Vector<String>> snippet_for_all_urls = new Vector<Vector<String>>(0);
    Vector<Vector<String>> snippet_for_all_urls_forms = new Vector<Vector<String>>(0);
    Vector<Vector<String>> snippet_for_all_urls_Non_common = new Vector<Vector<String>>(0);
    //phrase Search Snippets
    Vector<String> snippet_for_Phrase_Search = new Vector<String>(0);


    queryprocessing queryProcessor = new queryprocessing();
    Phrase_sreach PhraseSearch = new Phrase_sreach();
    MongoCollection<Document> rankerCollection;
    MongoCollection<Document> crawlerCollection;
    MongoDatabase db;
    Vector<String> searchWords = new Vector<String>(0);
    HashMap<String, Document> shosho = new HashMap<String, Document>(5007);

    //  Vector<JSONObject> phraseResults = new Vector<>(0);
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

    public void Teamp_func() throws JSONException {

        String s = new String();
        for (int i = 0; i < originalResults.size(); i++) {
            JSONObject obj = originalResults.get(i).get(0);
            s = obj.getString("_url");
            Iterator<String> iterator = obj.keys();
            while (iterator.hasNext()) {
                JSONObject weights = obj.getJSONObject(searchWords.get(i));
                float headers = 0;

                if (weights.has("h1"))
                    headers += weights.getJSONArray("h1").length() + 100;

                if (weights.has("h2"))
                    headers += weights.getJSONArray("h2").length() + 50;

                if (weights.has("h3"))
                    headers += weights.getJSONArray("h3").length() + 25;

                if (weights.has("h4"))
                    headers += weights.getJSONArray("h4").length() + 12;

                if (weights.has("h5"))
                    headers += weights.getJSONArray("h5").length() + 6;

                if (weights.has("h6"))
                    headers += weights.getJSONArray("h6").length() + 3;

                if (weights.has("p"))
                    headers += weights.getJSONArray("p").length();

                System.out.println(s);
//                arr[i] = headers;// the query processor ->
//                System.out.println("Headers " + headers);
//                arr[i] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
//                System.out.println("Headers / by no of words " + arr[i]);
//                arr[i] += DF.get(i) / 5000.0;
//                System.out.println("Multiplied by df " + arr[i]);
//                arr[i] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
////                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1; title
//                System.out.println("after pop " + arr[i]);

            }


//        float rank = 0;
//        for (int k = 0; k < arr.length; k++) {
////                System.out.println("K "+ arr[k] );
//            rank += arr[k];
//        }
        Document temp = new Document();
        temp.append("url", s);//s=>url
        temp.append("header", shosho.get(s).get("title").toString());
        temp.append("paragraph", snippet_for_Phrase_Search.get(i));
        temp.append("rank", 1);
        rankerCollection.insertOne(temp);
    }

//    originalResults .);
}
public void calculateStemmed() throws JSONException {
    float[] arr = new float[searchWords.size()];
    String s = new String();
    for (int i = 0; i < stemmedResults.size(); i++) {
        for (int j = 0; j < stemmedResults.get(i).size(); j++) {
            JSONObject obj = stemmedResults.get(i).get(j);
            s = obj.getString("_url");
            Iterator<String> iterator= obj.keys();
            while(iterator.hasNext()){
                String key = iterator.next();
                if(key.equals("_url"))
                    continue;
            JSONObject weights = obj.getJSONObject(key);
            float headers = 0;

            if (weights.has("h1"))
                headers += weights.getJSONArray("h1").length() + 100;

            if (weights.has("h2"))
                headers += weights.getJSONArray("h2").length() + 50;

            if (weights.has("h3"))
                headers += weights.getJSONArray("h3").length() + 25;

            if (weights.has("h4"))
                headers += weights.getJSONArray("h4").length() + 12;

            if (weights.has("h5"))
                headers += weights.getJSONArray("h5").length() + 6;

            if (weights.has("h6"))
                headers += weights.getJSONArray("h6").length() + 3;

            if (weights.has("p"))
                headers += weights.getJSONArray("p").length();
            System.out.println(s);
            arr[j] = headers;// the query processor ->
            System.out.println("Headers " + headers);
            arr[j] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
            System.out.println("Headers / by no of words " + arr[j]);
            arr[j] += DF.get(j) / 5000.0;
            System.out.println("Multiplied by df " + arr[j]);
            arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
//                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1; title
            System.out.println("after pop " + arr[j]);
                float rank = 0;
                for (int k = 0; k < arr.length; k++) {
//                System.out.println("K "+ arr[k] );
                    rank += arr[k];
                }
                Document temp = new Document();
                temp.append("url", s);
                temp.append("header", shosho.get(s).get("title").toString());
                temp.append("paragraph", snippet_for_all_urls_forms.get(j));///////*******///////
                temp.append("rank", rank);
                rankerCollection.insertOne(temp);
        }
        }

    }
    stemmedResults.clear();


}
    public void calculateNonCommon() throws JSONException {
        float[] arr = new float[searchWords.size()];
        String s = new String();
        for (int i = 0; i < nonCommon.size(); i++) {
            for (int j = 0; j < nonCommon.get(i).size(); j++) {
                JSONObject obj = nonCommon.get(i).get(j);
                s = obj.getString("_url");
                Iterator<String> iterator= obj.keys();
                while(iterator.hasNext()){
                    String key = iterator.next();
                    if(key.equals("_url"))
                        continue;
                    JSONObject weights = obj.getJSONObject(key);
                    float headers = 0;

                    if (weights.has("h1"))
                        headers += weights.getJSONArray("h1").length() + 100;

                    if (weights.has("h2"))
                        headers += weights.getJSONArray("h2").length() + 50;

                    if (weights.has("h3"))
                        headers += weights.getJSONArray("h3").length() + 25;

                    if (weights.has("h4"))
                        headers += weights.getJSONArray("h4").length() + 12;

                    if (weights.has("h5"))
                        headers += weights.getJSONArray("h5").length() + 6;

                    if (weights.has("h6"))
                        headers += weights.getJSONArray("h6").length() + 3;

                    if (weights.has("p"))
                        headers += weights.getJSONArray("p").length();
                    System.out.println(s);
                    arr[j] = headers;// the query processor ->
                    System.out.println("Headers " + headers);
                    arr[j] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
                    System.out.println("Headers / by no of words " + arr[j]);
                    arr[j] += DF.get(j) / 5000.0;
                    System.out.println("Multiplied by df " + arr[j]);
                    arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
                    System.out.println("after pop " + arr[j]);
                    float rank = 0;
                    for (int k = 0; k < arr.length; k++) {
//                System.out.println("K "+ arr[k] );
                        rank += arr[k];
                    }
                    Document temp = new Document();
                    temp.append("url", s);
                    temp.append("header", shosho.get(s).get("title").toString());
                    temp.append("paragraph", snippet_for_all_urls_Non_common.get(j));//////******//////
                    temp.append("rank", rank);
                    rankerCollection.insertOne(temp);
                }
            }

        }
        nonCommon.clear();


    }
    public void calculateRank(Vector<Vector<JSONObject>> vec) throws JSONException {

        float[] arr = new float[searchWords.size()];
        String s = new String();
        for (int i = 0; i < vec.size(); i++) {
            for (int j = 0; j < vec.get(i).size(); j++) {
                JSONObject obj = vec.get(i).get(j);
                s = obj.getString("_url");
                JSONObject weights = obj.getJSONObject(searchWords.get(j));
                float headers = 0;

                if (weights.has("h1"))
                    headers += weights.getJSONArray("h1").length() + 100;

                if (weights.has("h2"))
                    headers += weights.getJSONArray("h2").length() + 50;

                if (weights.has("h3"))
                    headers += weights.getJSONArray("h3").length() + 25;

                if (weights.has("h4"))
                    headers += weights.getJSONArray("h4").length() + 12;

                if (weights.has("h5"))
                    headers += weights.getJSONArray("h5").length() + 6;

                if (weights.has("h6"))
                    headers += weights.getJSONArray("h6").length() + 3;

                if (weights.has("p"))
                    headers += weights.getJSONArray("p").length();
                System.out.println(s);
                arr[j] = headers;// the query processor ->
                System.out.println("Headers " + headers);
                arr[j] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
                System.out.println("Headers / by no of words " + arr[j]);
                arr[j] += DF.get(j) / 5000.0;
                System.out.println("Multiplied by df " + arr[j]);
                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
//                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1; title
                System.out.println("after pop " + arr[j]);
            }
            float rank = 0;
            for (int k = 0; k < arr.length; k++) {
//                System.out.println("K "+ arr[k] );
                rank += arr[k];
            }
            Document temp = new Document();
            temp.append("url", s);
            temp.append("header", shosho.get(s).get("title").toString());
            temp.append("paragraph", snippet_for_all_urls.get(i));/////********/////
            temp.append("rank", rank);
            rankerCollection.insertOne(temp);
        }
        vec.clear();

    }

    public void calculatePhrase() throws JSONException {
        // float[] arr = new float[searchWords.size()];
//        String s = new String();
//        for (int i = 0; i < vec.size(); i++) {
//            for (int j = 0; j < vec.get(i).size(); j++) {
//                JSONObject obj = vec.get(i).get(j);
//                s = obj.getString("_url");
//                JSONObject weights = obj.getJSONObject(searchWords.get(j));
//                float headers = 0;
//
//                if (weights.has("h1"))
//                    headers += weights.getJSONArray("h1").length()+100 ;
//
//                if (weights.has("h2"))
//                    headers += weights.getJSONArray("h2").length()+50 ;
//
//                if (weights.has("h3"))
//                    headers += weights.getJSONArray("h3").length()+25 ;
//
//                if (weights.has("h4"))
//                    headers += weights.getJSONArray("h4").length()+12 ;
//
//                if (weights.has("h5"))
//                    headers += weights.getJSONArray("h5").length()+6 ;
//
//                if (weights.has("h6"))
//                    headers += weights.getJSONArray("h6").length()+3;
//
//                if (weights.has("p"))
//                    headers += weights.getJSONArray("p").length();
//                System.out.println(s);
//                arr[j] = headers;// the query processor ->
//                System.out.println("Headers "+headers);
//                arr[j] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
//                System.out.println("Headers / by no of words "+ arr[j]);
//                arr[j] += noOfDocumentsForWord.get(j) / 5000.0;
//                System.out.println("Multiplied by df "+arr[j]);
//                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
////                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1; title
//                System.out.println("after pop " +arr[j]);
//            }
//            float rank = 0;
//            for (int k = 0; k < arr.length; k++) {
////                System.out.println("K "+ arr[k] );
//                rank += arr[k];
//            }
//            Document temp = new Document();
//            temp.append("url", s);
//            temp.append("header",Jsoup.parse(shosho.get(s).get("html").toString()).title()
//            );
//            temp.append("paragraph", s);
//            temp.append("rank", rank);
//            rankerCollection.insertOne(temp);
//        }
//        vec.clear();

    }

    public void getResults(String query) throws JSONException {

        if (query.startsWith("\"") && query.endsWith("\"")) {
            originalResults = PhraseSearch.Phrase_Search(query, snippet_for_Phrase_Search, DF);
//            calculatePhrase();
            Teamp_func();
//            System.out.println(originalResults);
        } else {
            searchWords = queryProcessor.query_process(query, originalResults, stemmedResults, nonCommon, DF, snippet_for_all_urls, snippet_for_all_urls_forms, snippet_for_all_urls_Non_common);//DF
            calculateRank(originalResults);
        }


    }

    public static void main(String[] args) throws JSONException {
        Ranker r = new Ranker();
        r.getResults("\"war\"");

    }

}
