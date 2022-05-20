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
    HashMap<String, JSONObject> phraseSearchResults = new HashMap<>();
    HashMap<String, String> snippet_for_Phrase_Search = new HashMap<String, String>(0);
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
        System.out.println(phraseSearchResults);
        String url = new String();
        Iterator<String> it = phraseSearchResults.keySet().iterator();
        int NoofURLS = phraseSearchResults.size();
//        int i = -1;
        while (it.hasNext()) {
            url = it.next();
//            i++;
            JSONObject weights = phraseSearchResults.get(url);//==> the original words only
            System.out.println(weights);
            float rank = 0;
            float headers = 0;
//            for (int i = 0; i < weights.size(); i++) {
            float internalRank = 0;
            System.out.println("URL " + url);
            if (weights.has("h1"))
                headers += weights.getJSONArray("h1").length() * 10;

            if (weights.has("h2"))
                headers += weights.getJSONArray("h2").length() * 8;

            if (weights.has("h3"))
                headers += weights.getJSONArray("h3").length() * 6;

            if (weights.has("h4"))
                headers += weights.getJSONArray("h4").length() * 4;

            if (weights.has("h5"))
                headers += weights.getJSONArray("h5").length() * 2;

            if (weights.has("h6"))
                headers += weights.getJSONArray("h6").length() * 2;

            if (weights.has("p"))
                headers += weights.getJSONArray("p").length();

            internalRank = headers;
            System.out.println("HEADERS " + internalRank);
            internalRank /= Integer.parseInt(shosho.get(url).get("NoOfWords").toString());
            System.out.println("AFTER NORMALIZATION " + internalRank);
            //internalRank += DF.get(i) / 5000.0;
            internalRank *= Math.log(5000.0 / NoofURLS);
            System.out.println("AFTER DF " + internalRank);
            internalRank *= Integer.parseInt(shosho.get(url).get("popularity").toString()) * .001;
            System.out.println("AFTER POPULARITY " + internalRank);
            rank += internalRank;


            Document temp = new Document();
            temp.append("url", url);//s=>url
            temp.append("header", shosho.get(url).get("title"));
            temp.append("paragraph", snippet_for_Phrase_Search.get(url));
            temp.append("rank", rank);
            rankerCollection.insertOne(temp);
            rank = 0;

        }

    }

    public void calculateRank(HashMap<String, Vector<JSONObject>> vec) throws JSONException {


        String s = new String();
        String url = new String();

        Iterator<String> it = vec.keySet().iterator();
//        int i = -1;
        float rank = 0;
        while (it.hasNext()) {
            url = it.next(); //urls
//            i++;
            float internalRank = 0;
            Vector<JSONObject> weights = vec.get(url);//==> the original words only //loop
            System.out.println("Url " + url);
            for (int i = 0; i < weights.size(); i++) {
                float headers = 0;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h1"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h1").length() * 12;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h2"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h2").length() * 10;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h3"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h3").length() * 8;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h4"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h4").length() * 6;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h5"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h5").length() * 4;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h6"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h6").length() * 2;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("p"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("p").length();
                internalRank = headers;
                System.out.println("Headers " + internalRank);
                internalRank /= Integer.parseInt(shosho.get(url).get("NoOfWords").toString());
                System.out.println("Normalization " + internalRank);

                internalRank *= Math.log(5000.0 / vec.size());
                System.out.println("DF" + vec.size());
                System.out.println("DF  " + internalRank);

                internalRank *= Integer.parseInt(shosho.get(url).get("popularity").toString());
                System.out.println("Popularity " + internalRank);

                rank += internalRank;

            }
            Document temp = new Document();
            temp.append("url", url);//s=>url
            temp.append("header", shosho.get(url).get("title"));
            temp.append("paragraph", Snippets.get(url));
            temp.append("rank", rank);
            temp.append("type", 1);
            rankerCollection.insertOne(temp);
            rank = 0;
        }

    }

    public void calculateStemmedRank(HashMap<String, Vector<JSONObject>> vec, int x) throws JSONException {


        String s = new String();
        String url = new String();

        Iterator<String> it = vec.keySet().iterator();
//        int i = -1;
        float rank = 0;
        while (it.hasNext()) {
            url = it.next(); //urls
//            i++;
            float internalRank = 0;
            Vector<JSONObject> weights = vec.get(url);//==> the original words only //loop
            System.out.println("Url " + url);
            for (int i = 0; i < weights.size(); i++) {
                float headers = 0;
                Iterator<String> it2 = weights.get(i).keys();
                while (it2.hasNext()) {
                    String form = it2.next();
                    if (form.equals("_url"))
                        continue;
                    if (weights.get(i).getJSONObject(form).has("h1"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h1").length() * 12;

                    if (weights.get(i).getJSONObject(form).has("h2"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h2").length() * 10;

                    if (weights.get(i).getJSONObject(form).has("h3"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h3").length() * 8;

                    if (weights.get(i).getJSONObject(form).has("h4"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h4").length() * 6;

                    if (weights.get(i).getJSONObject(form).has("h5"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h5").length() * 4;

                    if (weights.get(i).getJSONObject(form).has("h6"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h6").length() * 2;

                    if (weights.get(i).getJSONObject(form).has("p"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("p").length();
                }
                internalRank = headers;
                System.out.println("Headers " + internalRank);
                internalRank /= Integer.parseInt(shosho.get(url).get("NoOfWords").toString());
                System.out.println("Normalization " + internalRank);
                if (x == 0)
                    internalRank *= Math.log(5000.0 / vec.size());
                else
                    internalRank *= Math.log(5000.0 / vec.size()) / searchWords.size();

                System.out.println("DF" + vec.size());
                System.out.println("DF  " + internalRank);

                internalRank *= Integer.parseInt(shosho.get(url).get("popularity").toString());
                System.out.println("Popularity " + internalRank);

                rank += internalRank;

            }
            if (x == 0)
                rank /= 5.0;
            else
                rank /= 10.0;
            System.out.println("Rank " + rank);
            Document temp = new Document();
            temp.append("url", url);//s=>url
            temp.append("header", shosho.get(url).get("title"));
            temp.append("paragraph", Snippets.get(url));
            temp.append("rank", rank);
            if (x == 0)
                temp.append("type", 2);
            else
                temp.append("type", 3);
            rankerCollection.insertOne(temp);
            rank = 0;
        }

    }


    public void getResults(String query) throws JSONException {
        long timeq1 = 0, timeq2 = 0, timeR1 = 0, timeR2 = 0;
        long Time1 = System.currentTimeMillis();
        if (query.startsWith("\"") && query.endsWith("\"")) {

            PhraseSearch.Phrase_Search(query, phraseSearchResults, snippet_for_Phrase_Search, DF);
            System.out.println("No of URLS:" + phraseSearchResults.size());
            Teamp_func();
        } else {

            timeq1 = System.currentTimeMillis();
            searchWords = queryProcessor.query_process(query, Original_Results, Steam_Results, NonCommon_Results, DF, Snippets);
            if (searchWords==null)
                return;
            timeq2 = System.currentTimeMillis();
            timeR1 = System.currentTimeMillis();
            calculateRank(Original_Results);
            calculateStemmedRank(Steam_Results, 0);
            calculateStemmedRank(NonCommon_Results, 1);
//            calculateRank(NonCommon_Results);
            timeR2 = System.currentTimeMillis();


        }
        Original_Results.clear();
        DF.clear();
        Steam_Results.clear();
        snippet_for_Phrase_Search.clear();
        Snippets.clear();
        NonCommon_Results.clear();

        long Time2 = System.currentTimeMillis();
        System.out.println("\n\nTotal Time to get result in fun get results:" + (Time2 - Time1));
        System.out.println("\nQuery Processing " + (timeq2 - timeq1));
        System.out.println("\nRanker " + (timeR2 - timeR1));

    }

    public static void main(String[] args) throws JSONException {
        Ranker r = new Ranker();
        r.getResults("\"first-class\"");

    }

}
