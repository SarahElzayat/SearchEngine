package com.example.demo;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.json.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class Ranker {
    //Urls


    HashMap<String, Vector<JSONObject>> Original_Results = new HashMap<>();
    HashMap<String, String> snippet_for_Phrase_Search = new HashMap<String, String>(0);
    //    Vector<String> snippet_for_Phrase_Search = new Vector<String>(0);
    Vector<Integer> DF = new Vector<Integer>(0); // EH DA?
    HashMap<String, Vector<JSONObject>> Steam_Results = new HashMap<>();
    HashMap<String, Vector<JSONObject>> NonCommon_Results = new HashMap<>();

    HashMap<String, Vector<String>> Snippets = new HashMap<>(0);

    queryprocessing queryProcessor = new queryprocessing();
    Phrase_sreach PhraseSearch = new Phrase_sreach();
    MongoCollection<Document> rankerCollection;
    MongoCollection<Document> crawlerCollection;
    MongoDatabase db;
    Vector<String> searchWords = new Vector<String>(0);
    public static HashMap<String, Document> shosho = new HashMap<String, Document>(5007);

    public Ranker() {
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        db = mongo.getDatabase("SearchEngine");
//        db.getCollection("Ranker").drop();
        rankerCollection = db.getCollection("Ranker");
        FindIterable<Document> shoshoGet = db.getCollection("URLSWithHTML").find();
        for (Document doc : shoshoGet) {
            shosho.put(doc.get("_id").toString(), doc);
        }
    }

    public void Teamp_func() throws JSONException {


        String url = new String();
        Iterator<String> it = Original_Results.keySet().iterator();
//        int i = -1;
        while (it.hasNext()) {
            url = it.next();
//            i++;
            Vector<JSONObject> weights = Original_Results.get(url);//==> the original words only
            float rank=0;
            float headers = 0;
            for (int i = 0; i < weights.size(); i++) {
                float internalRank =0;
                if (weights.get(i).has("h1"))
                    headers += weights.get(i).getJSONArray("h1").length() + 100;

                if (weights.get(i).has("h2"))
                    headers += weights.get(i).getJSONArray("h2").length() + 50;

                if (weights.get(i).has("h3"))
                    headers += weights.get(i).getJSONArray("h3").length() + 25;

                if (weights.get(i).has("h4"))
                    headers += weights.get(i).getJSONArray("h4").length() + 12;

                if (weights.get(i).has("h5"))
                    headers += weights.get(i).getJSONArray("h5").length() + 6;

                if (weights.get(i).has("h6"))
                    headers += weights.get(i).getJSONArray("h6").length() + 3;

                if (weights.get(i).has("p"))
                    headers += weights.get(i).getJSONArray("p").length();
                internalRank = headers;
                internalRank /= Integer.parseInt(shosho.get(url).get("NoOfWords").toString());
                internalRank += DF.get(i) / 5000.0;
                internalRank *= Integer.parseInt(shosho.get(url).get("popularity").toString());
                rank += internalRank;

            }
            Document temp = new Document();
            temp.append("url", url);//s=>url
            temp.append("header", shosho.get(url).get("title"));
            temp.append("paragraph", snippet_for_Phrase_Search.get(url));
            temp.append("rank", rank);
            rankerCollection.insertOne(temp);
            rank =0;
        }
    }

    public void calculateRank(HashMap<String, Vector<JSONObject>> vec) throws JSONException {


        String s = new String();
        String url = new String();

        Iterator<String> it = vec.keySet().iterator();
//        int i = -1;
        float rank=0;
        while (it.hasNext()) {
            url = it.next();
//            i++;
            float internalRank=0;
            Vector<JSONObject> weights = vec.get(url);//==> the original words only //loop
            for (int i = 0; i < weights.size(); i++) {
                float headers = 0;

                if (weights.get(i).has("h1"))
                    headers += weights.get(i).getJSONArray("h1").length() + 100;

                if (weights.get(i).has("h2"))
                    headers += weights.get(i).getJSONArray("h2").length() + 50;

                if (weights.get(i).has("h3"))
                    headers += weights.get(i).getJSONArray("h3").length() + 25;

                if (weights.get(i).has("h4"))
                    headers += weights.get(i).getJSONArray("h4").length() + 12;

                if (weights.get(i).has("h5"))
                    headers += weights.get(i).getJSONArray("h5").length() + 6;

                if (weights.get(i).has("h6"))
                    headers += weights.get(i).getJSONArray("h6").length() + 3;

                if (weights.get(i).has("p"))
                    headers += weights.get(i).getJSONArray("p").length();
                internalRank = headers;
                internalRank /= Integer.parseInt(shosho.get(url).get("NoOfWords").toString());
                internalRank += DF.get(i) / 5000.0;
                internalRank *= Integer.parseInt(shosho.get(url).get("popularity").toString());
                rank += internalRank;

            }
            Document temp = new Document();
            temp.append("url", url);//s=>url
            temp.append("header", shosho.get(url).get("title"));
            temp.append("paragraph", Snippets.get(url));
            temp.append("rank", rank);
            rankerCollection.insertOne(temp);
            rank =0;
        }

    }


    public void getResults(String query) throws JSONException {
        if (query.startsWith("\"") && query.endsWith("\"")) {
            long time1 = System.currentTimeMillis();
            PhraseSearch.Phrase_Search(query, Original_Results, snippet_for_Phrase_Search, DF);
            System.out.println("No of URLS:" + Original_Results.size());
            Teamp_func();
            long time2 = System.currentTimeMillis();
            System.out.println("\nTime " + (time2 - time1));
        } else {
            HashMap<String, Vector<JSONObject>> Steam_Results = new HashMap();
            HashMap<String, Vector<JSONObject>> NonCommon_Results = new HashMap();
            long time1 = System.currentTimeMillis();

            searchWords = queryProcessor.query_process(query, Original_Results, Steam_Results, NonCommon_Results, DF, Snippets);
            long time2 = System.currentTimeMillis();
            System.out.println("\nTime1 " + (time2 - time1));
            time1 = System.currentTimeMillis();
            calculateRank(Original_Results);
            calculateRank(Steam_Results);
            calculateRank(NonCommon_Results);
            time2 = System.currentTimeMillis();
            System.out.println("\nTime2 " + (time2 - time1));

        }
        Original_Results.clear();
        DF.clear();
        Steam_Results.clear();
        snippet_for_Phrase_Search.clear();
        Snippets.clear();
        NonCommon_Results.clear();

    }

    public static void main(String[] args) throws JSONException {
        Ranker r = new Ranker();
        r.getResults("\"first-class\"");

    }

}

//package com.example.demo;
//
//import com.mongodb.client.*;
//import org.bson.Document;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.jsoup.Jsoup;
//
//import javax.print.Doc;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Vector;
//
//public class Ranker {
//    //Urls
//
//
//    HashMap<String, Vector<JSONObject>> originalResults=new HashMap<>();
//    HashMap<String,String> snippet_for_Phrase_Search = new HashMap<String,String>(0);
////    Vector<String> snippet_for_Phrase_Search = new Vector<String>(0);
//    Vector<Integer> DF = new Vector<Integer>(0); // EH DA?
//
//
////    Vector<Vector<JSONObject>> originalResults = new Vector<Vector<org.json.JSONObject>>(0);;
//    Vector<Vector<org.json.JSONObject>> stemmedResults = new Vector<Vector<org.json.JSONObject>>(0);
//    Vector<Vector<JSONObject>> nonCommon = new Vector<Vector<org.json.JSONObject>>(0);
//    //DF
////    Vector<Integer> DF = new Vector<Integer>(0); // EH DA?
//    //Snipptes
//    Vector<Vector<String>> snippet_for_all_urls = new Vector<Vector<String>>(0);
//    Vector<Vector<String>> snippet_for_all_urls_forms = new Vector<Vector<String>>(0);
//    Vector<Vector<String>> snippet_for_all_urls_Non_common = new Vector<Vector<String>>(0);
//    HashMap<String,Vector<String>>Snippets=new HashMap<>(0);
//    //phrase Search Snippets
//    Vector<String> getSnippet_for_Phrase_Search = new Vector<String>(0);
//
//
//    queryprocessing queryProcessor = new queryprocessing();
//    Phrase_sreach PhraseSearch = new Phrase_sreach();
//    MongoCollection<Document> rankerCollection;
//    MongoCollection<Document> crawlerCollection;
//    MongoDatabase db;
//    Vector<String> searchWords = new Vector<String>(0);
//    public static HashMap<String, Document> shosho = new HashMap<String, Document>(5007);
//    public Ranker(){
//        String uri = "mongodb://localhost:27017";
//        MongoClient mongo = MongoClients.create(uri);
//        db = mongo.getDatabase("SearchEngine");
////        db.getCollection("Ranker").drop();
//        rankerCollection = db.getCollection("Ranker");
//        FindIterable<Document> shoshoGet = db.getCollection("URLSWithHTML").find();
//        for (Document doc : shoshoGet) {
//            shosho.put(doc.get("_id").toString(), doc);
//        }
//    }
//
//    public void Teamp_func() throws JSONException {
//
//
//
//        String url = new String();
//        Iterator<String> it=originalResults.keySet().iterator();
//        int i=-1;
//        while(it.hasNext())
//        {
//            url =it.next();
//            i++;
////          Vector<JsonObject> = Original_Results.get(url);==> the orignal words only
//            Document temp = new Document();
//            temp.append("url", url);//s=>url
//            temp.append("header", Jsoup.parse(shosho.get(url).get("html").toString()).title());
//            temp.append("paragraph", snippet_for_Phrase_Search.get(url));
//            temp.append("rank", 1);
//            rankerCollection.insertOne(temp);
//        }
//        originalResults.clear();
//        snippet_for_Phrase_Search.clear();
//    }
//public void calculateStemmed() throws JSONException {
//    float[] arr = new float[searchWords.size()];
//    String s = new String();
//    for (int i = 0; i < stemmedResults.size(); i++) {
//        for (int j = 0; j < stemmedResults.get(i).size(); j++) {
//            JSONObject obj = stemmedResults.get(i).get(j);
//            s = obj.getString("_url");
//            Iterator<String> iterator= obj.keys();
//            while(iterator.hasNext()){
//                String key = iterator.next();
//                if(key.equals("_url"))
//                    continue;
//            JSONObject weights = obj.getJSONObject(key);
//            float headers = 0;
//
//            if (weights.has("h1"))
//                headers += weights.getJSONArray("h1").length() + 100;
//
//            if (weights.has("h2"))
//                headers += weights.getJSONArray("h2").length() + 50;
//
//            if (weights.has("h3"))
//                headers += weights.getJSONArray("h3").length() + 25;
//
//            if (weights.has("h4"))
//                headers += weights.getJSONArray("h4").length() + 12;
//
//            if (weights.has("h5"))
//                headers += weights.getJSONArray("h5").length() + 6;
//
//            if (weights.has("h6"))
//                headers += weights.getJSONArray("h6").length() + 3;
//
//            if (weights.has("p"))
//                headers += weights.getJSONArray("p").length();
//            System.out.println(s);
//            arr[j] = headers;// the query processor ->
//            System.out.println("Headers " + headers);
//            arr[j] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
//            System.out.println("Headers / by no of words " + arr[j]);
//            arr[j] += DF.get(j) / 5000.0;
//            System.out.println("Multiplied by df " + arr[j]);
//            arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
////                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1; title
//            System.out.println("after pop " + arr[j]);
//                float rank = 0;
//                for (int k = 0; k < arr.length; k++) {
////                System.out.println("K "+ arr[k] );
//                    rank += arr[k];
//                }
//                Document temp = new Document();
//                temp.append("url", s);
//                temp.append("header", shosho.get(s).get("title").toString());
//                temp.append("paragraph", snippet_for_all_urls_forms.get(j));///////*******///////
//                temp.append("rank", rank);
//                rankerCollection.insertOne(temp);
//        }
//        }
//
//    }
//    stemmedResults.clear();
//
//
//}
//    public void calculateNonCommon() throws JSONException {
//        float[] arr = new float[searchWords.size()];
//        String s = new String();
//        for (int i = 0; i < nonCommon.size(); i++) {
//            for (int j = 0; j < nonCommon.get(i).size(); j++) {
//                JSONObject obj = nonCommon.get(i).get(j);
//                s = obj.getString("_url");
//                Iterator<String> iterator= obj.keys();
//                while(iterator.hasNext()){
//                    String key = iterator.next();
//                    if(key.equals("_url"))
//                        continue;
//                    JSONObject weights = obj.getJSONObject(key);
//                    float headers = 0;
//
//                    if (weights.has("h1"))
//                        headers += weights.getJSONArray("h1").length() + 100;
//
//                    if (weights.has("h2"))
//                        headers += weights.getJSONArray("h2").length() + 50;
//
//                    if (weights.has("h3"))
//                        headers += weights.getJSONArray("h3").length() + 25;
//
//                    if (weights.has("h4"))
//                        headers += weights.getJSONArray("h4").length() + 12;
//
//                    if (weights.has("h5"))
//                        headers += weights.getJSONArray("h5").length() + 6;
//
//                    if (weights.has("h6"))
//                        headers += weights.getJSONArray("h6").length() + 3;
//
//                    if (weights.has("p"))
//                        headers += weights.getJSONArray("p").length();
//                    System.out.println(s);
//                    arr[j] = headers;// the query processor ->
//                    System.out.println("Headers " + headers);
//                    arr[j] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
//                    System.out.println("Headers / by no of words " + arr[j]);
//                    arr[j] += DF.get(j) / 5000.0;
//                    System.out.println("Multiplied by df " + arr[j]);
//                    arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
//                    System.out.println("after pop " + arr[j]);
//                    float rank = 0;
//                    for (int k = 0; k < arr.length; k++) {
////                System.out.println("K "+ arr[k] );
//                        rank += arr[k];
//                    }
//                    Document temp = new Document();
//                    temp.append("url", s);
//                    temp.append("header", shosho.get(s).get("title").toString());
//                    temp.append("paragraph", snippet_for_all_urls_Non_common.get(j));//////******//////
//                    temp.append("rank", rank);
//                    rankerCollection.insertOne(temp);
//                }
//            }
//
//        }
//        nonCommon.clear();
//
//
//    }
////    public void calculateRank(Vector<Vector<JSONObject>> vec) throws JSONException {
////
////
////
////        String url = new String();
////        Iterator<String> it=originalResults.keySet().iterator();
////        int i=-1;
////        while(it.hasNext())
////        {
////            url =it.next();
////            i++;
//////          Vector<JsonObject> = Original_Results.get(url);==> the orignal words only
////            Document temp = new Document();
////            temp.append("url", url);//s=>url
////            temp.append("header", Jsoup.parse(shosho.get(url).get("html").toString()).title());
////            temp.append("paragraph", snippet_for_Phrase_Search.get(url));
////            temp.append("rank", 1);
////            rankerCollection.insertOne(temp);
////        }
////        originalResults.clear();
////        snippet_for_Phrase_Search.clear();
////    }
//    public void calculateRank(Vector<Vector<JSONObject>> vec) throws JSONException {
//
////
////        float[] arr = new float[searchWords.size()];
////        String s = new String();
////        for (int i = 0; i < vec.size(); i++) {
////            for (int j = 0; j < vec.get(i).size(); j++) {
////                JSONObject obj = vec.get(i).get(j);
////                s = obj.getString("_url");
////
////
////                Iterator<String> iterator=obj.keys();
////                while (iterator.hasNext()) {
////                   String key = iterator.next().toString();//here the key is "working"  "worker"  "_url" "workers"
////                   if(key.equals("_url"))//ignore url key
////                       continue;
////                    JSONObject weights = obj.getJSONObject(key);
////                    }
////                }
////            }
//
//
//        float[] arr = new float[searchWords.size()];
//        String s = new String();
//        for (int i = 0; i < vec.size(); i++) {
//            for (int j = 0; j < vec.get(i).size(); j++) {
//                JSONObject obj = vec.get(i).get(j);
//                s = obj.getString("_url");
//                JSONObject weights = obj.getJSONObject(searchWords.get(j));
//                float headers = 0;
//
//                if (weights.has("h1"))
//                    headers += weights.getJSONArray("h1").length() + 100;
//
//                if (weights.has("h2"))
//                    headers += weights.getJSONArray("h2").length() + 50;
//
//                if (weights.has("h3"))
//                    headers += weights.getJSONArray("h3").length() + 25;
//
//                if (weights.has("h4"))
//                    headers += weights.getJSONArray("h4").length() + 12;
//
//                if (weights.has("h5"))
//                    headers += weights.getJSONArray("h5").length() + 6;
//
//                if (weights.has("h6"))
//                    headers += weights.getJSONArray("h6").length() + 3;
//
//                if (weights.has("p"))
//                    headers += weights.getJSONArray("p").length();
//                System.out.println(s);
//                arr[j] = headers;// the query processor ->
//                System.out.println("Headers " + headers);
//                arr[j] /= Integer.parseInt(shosho.get(s).get("NoOfWords").toString());
//                System.out.println("Headers / by no of words " + arr[j]);
//                arr[j] += DF.get(j) / 5000.0;
//                System.out.println("Multiplied by df " + arr[j]);
//                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1;
////                arr[j] *= Integer.parseInt(shosho.get(s).get("popularity").toString());//*.1; title
//                System.out.println("after pop " + arr[j]);
//            }
//            float rank = 0;
//            for (int k = 0; k < arr.length; k++) {
////                System.out.println("K "+ arr[k] );
//                rank += arr[k];
//            }
//            Document temp = new Document();
//            temp.append("url", s);
//            temp.append("header", shosho.get(s).get("title").toString());
//            temp.append("paragraph", snippet_for_all_urls.get(i));/////********/////
//            temp.append("rank", rank);
//            rankerCollection.insertOne(temp);
//        }
//        vec.clear();
//    }
//
//    public void getResults(String query) throws JSONException {
//        if(query.startsWith("\"") && query.endsWith("\"")){
//            long time1 =System.currentTimeMillis();
//           PhraseSearch.Phrase_Search(query,originalResults,snippet_for_Phrase_Search,DF);
//           System.out.println("No of URLS:"+originalResults.size());
//           Teamp_func();
//            long time2 =System.currentTimeMillis();
//            System.out.println("\nTime"+(time2-time1));
//        }
//        else
//        {
//            HashMap<String,Vector<JSONObject>>Steam_Results=new HashMap();
//            HashMap<String,Vector<JSONObject>>NonCommon_Results=new HashMap();
//            searchWords = queryProcessor.query_process(query, originalResults, Steam_Results, NonCommon_Results, DF, Snippets);
//           // calculateRank(originalResults);
////        calculateRank(stemmedResults);
////        calculateRank(nonCommon);
//        }
//
//
//    }
//
//    public static void main(String[] args) throws JSONException {
//        Ranker r = new Ranker();
//        r.getResults("\"war\"");
//
//    }
//
//}
