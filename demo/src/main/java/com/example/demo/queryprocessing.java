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
    public  Vector<String> query_process(String QP_str,Vector<Vector<JSONObject>>resultorginal,Vector<Vector<JSONObject>> resultforms,Vector<Vector<JSONObject>>NonCommon,Vector<Integer>DF,Vector<Vector<String>>snippet_for_all_urls,Vector<Vector<String>>snippet_for_all_urls_forms) throws JSONException {

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
        query_process_work(finalword,docarr,resultorginal,resultforms,NonCommon);
        extract_Common_Array_tags(finalword,resultorginal,snippet_for_all_urls);
        extract_Steaming_Array_tags(resultforms,snippet_for_all_urls_forms);
        return finalword;
    }
    private void query_process_work(  Vector<String>words, Vector<JSONArray> docarr, Vector<Vector<JSONObject>> resultorginal,Vector<Vector<JSONObject>>  resultforms,Vector<Vector<JSONObject>>NonCommon) throws JSONException {

        Vector<JSONObject> tempNonCommon = new Vector<JSONObject>(1);
        Vector<JSONObject> temporginal = new Vector<JSONObject>(1);
        Vector<JSONObject> tempform = new Vector<JSONObject>(1);

        //unique urls
        HashMap<String, Integer> Urls = new HashMap<String, Integer>();
        for (int j = 0; j < docarr.size(); j++) {
            //each document
            for (int k = 0; k < docarr.get(j).length(); k++) {
                //each url of it\
                String url = docarr.get(j).getJSONObject(k).getString("_url");
                //getting Frequency
                Integer freq = Urls.get(url);
                if (freq == null)
                    freq = 0;
                freq++;
                Urls.put(url, freq);
            }
        }

        //loop over all urls
        for (Map.Entry<String, Integer> set : Urls.entrySet()) {
            String url = set.getKey();
            //loop over documents of words
            boolean common = true;
            boolean original = false;
            for (int i = 0; i < docarr.size(); i++) {
                //loop over all urls of this word to compare with url
                JSONArray wordDocs = docarr.get(i);
                int j;
                for (j = 0; j < wordDocs.length(); j++) {
                    if (url.equals(wordDocs.getJSONObject(j).getString("_url"))) {

                        Integer freq = set.getValue();
                        if (freq < words.size())//non common
                        {
                            common=false;
                            tempNonCommon.add(wordDocs.getJSONObject(j));
                            //break;
                        }
                        //common
                        //original or steam
                        else if (wordDocs.getJSONObject(j).has(words.get(i))) {
                            original = true;
                        }

                        break;
                    }
                }

                if(j==wordDocs.length()) //this link doesn't contain the original or even the stem of words(j)
                    continue;
                if(!common)
                    continue;
                if (original) {
                    temporginal.add(wordDocs.getJSONObject(j));
                } else {
                    tempform.add(wordDocs.getJSONObject(j));
                }
            }
            if(tempNonCommon.size()!=0)
            {
                NonCommon.add(new Vector<JSONObject>(tempNonCommon));
            }
            //is orignal for all words
            else if (temporginal.size() == words.size()) {
                resultorginal.add(new Vector<JSONObject>(temporginal));
            } else {
                tempform.addAll(temporginal);
                resultforms.add(new Vector<JSONObject>(tempform));
            }
            tempform.clear();
            temporginal.clear();
            tempNonCommon.clear();

//            System.out.println(Urls);
//            System.out.println(Urls.size());
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

        long time=System.nanoTime();
        System.out.println("\ntimeeee"+time);

        queryprocessing q=new queryprocessing();
        String query="general library";
        query=query.trim();

        Vector<Vector<org.json.JSONObject>> resultorginal=new Vector<Vector<org.json.JSONObject>>(1);
        Vector<Vector<org.json.JSONObject>>  resultforms=new Vector<Vector<org.json.JSONObject>>(1);
        Vector<Vector<JSONObject>>NonCommon=new Vector<Vector<org.json.JSONObject>>(1);
        Vector<Vector<String>>snippet_for_all_urls=new Vector<Vector<String>>(1);
        Vector<Integer> DF=new Vector<Integer>(1);
         Vector<Vector<String>>snippet_for_all_urls_forms=new Vector<Vector<String>>(1);
        if(query.startsWith("\"") && query.endsWith("\"")){
            //  phraseSearch.Phraseprocess(query);
        }
        else{
            q.query_process(query,resultorginal, resultforms, NonCommon,DF,snippet_for_all_urls,snippet_for_all_urls_forms);
        }
         long time2 =System.nanoTime();
        System.out.println("\ntimeafter"+(time2-time));


        System.out.println(resultorginal.size());
        for(int m=0;m<resultorginal.size();m++) {
            for (int k = 0; k < (resultorginal.get(m)).size(); k++) {
                System.out.println("Orignal:" + resultorginal.get(m).get(k));
//                System.out.println(m);
            }
            System.out.println(snippet_for_all_urls.get(m));
        }
//        for(int i=0;i<DF.size();i++)
//            System.out.println(DF.get(i));


        for(int m=0;m<resultforms.size();m++)
        {
            for(int k=0;k<(resultforms.get(m)).size();k++) {
                System.out.println("steming:" + resultforms.get(m).get(k));
                System.out.println(m);
            }
            System.out.println(snippet_for_all_urls_forms.get(m));
        }

        for(int m=0;m<NonCommon.size();m++)
            for(int k=0;k<(NonCommon.get(m)).size();k++) {
                System.out.println("Noncommon:" + NonCommon.get(m).get(k));
                System.out.println(m);}


    }

}
