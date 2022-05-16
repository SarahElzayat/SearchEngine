package com.example.demo;
//Database
/*import MongoDBPackage.MongoDB;
import IndexerPackage.Indexer;*/
import ch.qos.logback.core.BasicStatusManager;
import opennlp.tools.stemmer.PorterStemmer;
import org.bson.Document;

import java.util.*;

/*import org.json.JSONException;*/
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

public class queryprocessing {
    private MongoDB database;
    private PorterStemmer porterStemmer ;//stemmer
    private static HashSet<String> stopWords ;
    private static HashSet<String> ImportantWords;
    private MongoDB database_Crawler;

    public queryprocessing()
    {
        database_Crawler=new MongoDB("SearchEngine","URLSWithHTML");
        database=new MongoDB("SearchEngine","Indexer");
        porterStemmer = new PorterStemmer();
        stopWords=Indexer.getStopWords();
        ImportantWords=Indexer.getImportantword();
    }
    public  Vector<String> query_process(String QP_str,HashMap<String,Vector<JSONObject>> Original_Results, HashMap<String, Vector<JSONObject>> Steam_Results,HashMap<String, Vector<JSONObject>> NonCommon_Results,Vector<Integer>DF,Vector<Vector<String>>snippet_for_all_urls,Vector<Vector<String>>snippet_for_all_urls_forms,Vector<Vector<String>>snippet_for_all_urls_Non_Common) throws JSONException {
        Boolean Notfound=false;

        HashMap<String ,JSONObject>Snippets_and_DF=new HashMap<>();
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
//                        finalword.add(sw);
                        continue;
                    }
                    String stemw = porterStemmer.stem(sw);

                    Document DBresult = database.collection.find(new Document("_id", stemw)).first();
                    if (DBresult != null) {
                        // System.out.println(stemw);
                        JSONObject obj = new JSONObject(DBresult.toJson());
                        JSONArray arr = obj.getJSONArray("DOC");
                        finalword.add(sw);
                        DF.add(obj.getInt("DF"));
                        docarr.add(arr);
                    }
                    else{
                        Notfound=true;
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
        query_process_work(finalword,docarr,Original_Results,Steam_Results,NonCommon_Results,Notfound);
//        extract_Common_Array_tags(finalword,resultorginal,snippet_for_all_urls);
//        extract_Steaming_Array_tags(resultforms,snippet_for_all_urls_forms);
//        extract_Steaming_Array_tags(NonCommon,snippet_for_all_urls_Non_Common);
        return finalword;
    }
    private void query_process_work(  Vector<String>words, Vector<JSONArray> docarr,  HashMap<String, Vector<JSONObject>> Original_Results, HashMap<String, Vector<JSONObject>> Steam_Results,HashMap<String, Vector<JSONObject>> NonCommon_Results,Boolean not_Found) throws JSONException {

        HashMap<String, HashMap<String, JSONObject>> Inedxer_Results = new HashMap();

//        HashMap<String, Vector<JSONObject>> Original_Results = new HashMap<String, Vector<JSONObject>>();
//        HashMap<String, Vector<JSONObject>> Steam_Results = new HashMap<String, Vector<JSONObject>>();
//        HashMap<String, Vector<JSONObject>> NonCommon_Results = new HashMap<String, Vector<JSONObject>>();


        Vector<JSONObject> Temp_Original = new Vector<JSONObject>(0);
        Vector<JSONObject> Temp_Steam = new Vector<JSONObject>(0);
        Vector<JSONObject> Temp_NonCommon = new Vector<JSONObject>(0);


//        Vector<JSONObject> temporginal = new Vector<JSONObject>(1);
//        Vector<JSONObject> tempNonCommon = new Vector<JSONObject>(1);
//        Vector<JSONObject> tempform = new Vector<JSONObject>(1);

        //unique urls
        HashMap<String, Integer> Urls = new HashMap<String, Integer>();
        //loop over each word
        //O(n2)
        for (int j = 0; j < docarr.size(); j++) {
            HashMap<String, JSONObject> HashMap_Word = new HashMap<>();
            //loop over each url for this word
            for (int k = 0; k < docarr.get(j).length(); k++) {
                String url = docarr.get(j).getJSONObject(k).getString("_url");
                HashMap_Word.put(docarr.get(j).getJSONObject(k).getString("_url"), docarr.get(j).getJSONObject(k));
                //getting Frequency
                Integer freq = Urls.get(url);
                if (freq == null)
                    freq = 0;
                freq++;
                Urls.put(url, freq);
            }
            Inedxer_Results.put(words.get(j), HashMap_Word);
        }

        //loop over all urls
        //O(n2)
        for (Map.Entry<String, Integer> set : Urls.entrySet()) {
//            boolean common = true;
//            boolean original = false;
            String url = set.getKey();
            Integer freq = set.getValue();
            //loop over all words in the query
            for (int i = 0; i < docarr.size(); i++) {
                //loop over all urls of this word to compare with url
                //get this url for this word
                //Getting hashmap of this word
                String Query_Word = words.get(i);
                JSONObject word_Url = Inedxer_Results.get(Query_Word).get(url);
                if (word_Url == null) {//this word isn't in this url
//                    common = false;
                } else {//this word is in this url
                    if (freq < words.size())//non common
                    {
                        Temp_NonCommon.add(word_Url);
                    }
                    else if (word_Url.has(Query_Word))//has original form like word in the query
                    {
                        Temp_Original.add(word_Url);
                    } else {//this word's stem is in the url
                        Temp_Steam.add(word_Url);
                    }
                }
            }
            if(Temp_NonCommon.size()!=0)
            {
                NonCommon_Results.put(url,new Vector<JSONObject>(Temp_NonCommon));
            }
            else if(Temp_Original.size()==words.size())//Original URL
            {
                if(not_Found)
                    NonCommon_Results.put(url,new Vector<JSONObject>(Temp_Original));
                else
                    Original_Results.put(url,new Vector<JSONObject>(Temp_Original));
            }
            else{//Non Orignal
                Temp_Steam.addAll(Temp_Original);
                if(not_Found)
                    NonCommon_Results.put(url,new Vector<JSONObject>(Temp_Steam));
                else
                    Steam_Results.put(url,new Vector<JSONObject>(Temp_Steam));
            }
            Temp_Original.clear();
            Temp_NonCommon.clear();
            Temp_Steam.clear();
        }
    }

    public Vector<String> snippet_url(JSONArray body,Vector<Integer>Start_index) throws JSONException
    {
        Vector<String>all_Sinppet=new Vector<>(1);
        for (int j=0;j<Start_index.size();j++) {
            if(j>0)
            {
                int k=0;
                for (;k<all_Sinppet.size();k++)
                {

                    if(all_Sinppet.get(k).toLowerCase().contains((body.getString(Start_index.get(j))).toLowerCase()))
                        break;
                }
                if(k!=all_Sinppet.size())
                    continue;
            }
            StringBuffer snippet = new StringBuffer();
            int i = Start_index.get(j);
            while (i >= 0 && !body.getString(i).endsWith(".")) {
                //snippet.insert(0,body.getString(i)+" ");
                StringBuffer temp = new StringBuffer(body.getString(i));
                temp.reverse();
                snippet.append(temp + " ");
                i--;
            }
            snippet.reverse();
            snippet.append(" ");
            i = Start_index.get(j) + 1;
            if (body.getString(Start_index.get(j)).endsWith("."))
                all_Sinppet.add(snippet.toString());
            else {


                while (i <= body.length() - 1 && !body.getString(i).endsWith(".")) {
                    snippet.append(body.getString(i) + " ");
                    i++;
                }
                if (i <= body.length() - 1)
                    snippet.append(body.getString(i) + " ");

                all_Sinppet.add(snippet.toString());
            }
        }
        return all_Sinppet;
    }
    private void extract_Common_Array_tags(  Vector<String>finalword,Vector<Vector<org.json.JSONObject>> resultorginal,Vector<Vector<String>>snippet_for_all_urls) throws JSONException
    {
        for (int i = 0; i < resultorginal.size(); i++)
        {//row
            String url=resultorginal.get(i).get(0).getString("_url");
            Document DBresult = database_Crawler.collection.find(new Document("_url", url)).first();
            JSONArray body=null;
            if (DBresult != null) {
                JSONObject obj = new JSONObject(DBresult.toJson());
                body = obj.getJSONArray("_body");
            }
            Vector<Integer>Start_index=new Vector<Integer>(1);
            JSONObject[] docarr = new JSONObject[resultorginal.get(i).size()];
            for (int j = 0; j < resultorginal.get(i).size(); j++)
            {
                docarr[j] = resultorginal.get(i).get(j).getJSONObject(finalword.get(j));
                Indexer Id=new Indexer();
                Iterator<String> it=Id.tagsnames.iterator();
                while (it.hasNext()) {
                    String x = it.next();
                    if (docarr[j].has(x))
                    {
                        JSONArray indexes=docarr[j].getJSONArray(x);
                        Start_index.add((Integer) indexes.get(0));
                        break;
                    }
                }
            }
            snippet_for_all_urls.add(snippet_url(body,Start_index));
        }
    }
    private void extract_Steaming_Array_tags(  Vector<Vector<org.json.JSONObject>> resultforms,Vector<Vector<String>>snippet_for_all_urls_forms) throws JSONException
    {
        for (int i = 0; i < resultforms.size(); i++)
        {//row
            String url=resultforms.get(i).get(0).getString("_url");
            Document DBresult = database_Crawler.collection.find(new Document("_url", url)).first();
            JSONArray body=null;
            if (DBresult != null) {
                JSONObject obj = new JSONObject(DBresult.toJson());
                body = obj.getJSONArray("_body");
            }
            Vector<Integer>Start_index=new Vector<Integer>(1);
            JSONObject[] docarr = new JSONObject[resultforms.get(i).size()];
            for (int j = 0; j < resultforms.get(i).size(); j++)
            {
                Iterator<String> iter=resultforms.get(i).get(j).keys();
                String key = iter.next().toString();
                docarr[j] = resultforms.get(i).get(j).getJSONObject(key);
                Indexer Id=new Indexer();
                Iterator<String> it=Id.tagsnames.iterator();
                while (it.hasNext()) {
                    String x = it.next();
                    if (docarr[j].has(x))
                    {
                        JSONArray indexes=docarr[j].getJSONArray(x);
                        Start_index.add((Integer) indexes.get(0));
                        break;
                    }
                }
            }
            snippet_for_all_urls_forms.add(snippet_url(body,Start_index));
        }
    }

    public static void main(String[] args) throws JSONException {

        long time = System.currentTimeMillis();
        System.out.println("\ntimeeee" + time);

        queryprocessing q = new queryprocessing();
        String query = "virus";
        query = query.trim();

        Vector<Vector<org.json.JSONObject>> resultorginal = new Vector<Vector<org.json.JSONObject>>(1);
        Vector<Vector<org.json.JSONObject>> resultforms = new Vector<Vector<org.json.JSONObject>>(1);
        Vector<Vector<JSONObject>> NonCommon = new Vector<Vector<org.json.JSONObject>>(1);
        Vector<Vector<String>> snippet_for_all_urls = new Vector<Vector<String>>(1);
        Vector<Integer> DF = new Vector<Integer>(1);
        Vector<Vector<String>> snippet_for_all_urls_forms = new Vector<Vector<String>>(1);
        Vector<Vector<String>> snippet_for_all_urls_Non_common = new Vector<Vector<String>>(1);
        if (query.startsWith("\"") && query.endsWith("\"")) {
            //  phraseSearch.Phraseprocess(query);
        } else {
            //q.query_process(query, resultorginal, resultforms, NonCommon, DF, snippet_for_all_urls, snippet_for_all_urls_forms, snippet_for_all_urls_Non_common);
        }
        long time2 = System.currentTimeMillis();
        System.out.println("\ntimeafter" + (time2 - time));


        System.out.println(resultorginal.size());
        for (int m = 0; m < resultorginal.size(); m++) {
            for (int k = 0; k < (resultorginal.get(m)).size(); k++) {
                System.out.println("Orignal:" + resultorginal.get(m).get(k));
//                System.out.println(m);
            }
            System.out.println(snippet_for_all_urls.get(m));
        }
//        for(int i=0;i<DF.size();i++)
//            System.out.println(DF.get(i));


        for (int m = 0; m < resultforms.size(); m++) {
            for (int k = 0; k < (resultforms.get(m)).size(); k++) {
                System.out.println("steming:" + resultforms.get(m).get(k));
                System.out.println(m);
            }
            System.out.println(snippet_for_all_urls_forms.get(m));
        }

        for (int m = 0; m < NonCommon.size(); m++)
        {
            for (int k = 0; k < (NonCommon.get(m)).size(); k++) {
                System.out.println("Noncommon:" + NonCommon.get(m).get(k));
                System.out.println(m);
            }
            System.out.println(snippet_for_all_urls_Non_common.get(m));
        }

    }

}
