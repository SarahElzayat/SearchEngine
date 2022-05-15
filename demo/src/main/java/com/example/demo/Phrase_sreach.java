package com.example.demo;
/*import IndexerPackage.Indexer;
 *//*import QueryProcessing.queryprocessing;*//*
import MongoDBPackage.MongoDB;*/
/*import QueryProcessing.queryprocessing;*/
import opennlp.tools.stemmer.PorterStemmer;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Phrase_sreach
{
    private MongoDB database;
    private MongoDB database_Indexer;
    private PorterStemmer porterStemmer ;//stemmer
    private static HashSet<String> stopWords ;
    private static HashSet<String> ImportantWords;
    public Phrase_sreach()
    {
        //connect to DB
        database=new MongoDB("SearchEngine","URLSWithHTML");
        database_Indexer=new MongoDB("SearchEngine","Indexer");
        porterStemmer = new PorterStemmer();
        stopWords=Indexer.getStopWords();
        ImportantWords=Indexer.getImportantword();
    }
    public  Vector<String> Phrase_process(String QP_str,Vector<Vector<JSONObject>>resultorginal,Vector<Integer>DF) throws JSONException {
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

                    Document DBresult = database_Indexer.collection.find(new Document("_id", stemw)).first();
                    if (DBresult != null)
                    {
                        // System.out.println(stemw);
                        JSONObject obj = new JSONObject(DBresult.toJson());
                        JSONArray arr = obj.getJSONArray("DOC");
                        finalword.add(sw);
                        DF.add(obj.getInt("DF"));
                        docarr.add(arr);
                    }
                    else{
                       // Notfound=true;
                       return  null;
                    }
                }
            }
            else{
                String stemw = porterStemmer.stem(words[i]);
                Document DBresult = database_Indexer.collection.find(new Document("_id", stemw)).first();
                if (DBresult != null) {
                    JSONObject obj = new JSONObject(DBresult.toJson());
                    JSONArray arr = obj.getJSONArray("DOC");
                    docarr.add(arr);
                    finalword.add(words[i]);
                }
                else
                    return  null;
            }

        }
//            for(int k=0;k<docarr.size();k++)
//                System.out.println("docarr:" + docarr.get(k));
        phrase_process_work(finalword,docarr,resultorginal);
        return finalword;
    }
    private void phrase_process_work(  Vector<String>words, Vector<JSONArray> docarr, Vector<Vector<JSONObject>> resultorginal) throws JSONException {

        Vector<JSONObject> temporginal = new Vector<JSONObject>(1);
        //unique urls
        HashMap<String, Integer> Urls = new HashMap<String, Integer>();
        for (int j = 0; j < docarr.size(); j++)
        {
            //each document
            for (int k = 0; k < docarr.get(j).length(); k++)
            {
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
        for (Map.Entry<String, Integer> set : Urls.entrySet())
        {
            String url = set.getKey();
            //loop over documents of words
            boolean common = true;
            boolean original = false;
            for (int i = 0; i < docarr.size(); i++)
            {
                //loop over all urls of this word to compare with url
                JSONArray wordDocs = docarr.get(i);
                int j;
                for (j = 0; j < wordDocs.length(); j++) {
                    if (url.equals(wordDocs.getJSONObject(j).getString("_url"))) {
                        Integer freq = set.getValue();
                        if (freq < words.size())//non common
                        {
                            common = false;
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
                if (j == wordDocs.length()) //this link doesn't contain the original or even the stem of words(j)
                    break;
                if (!common)
                    break;
                if (original) {
                    temporginal.add(wordDocs.getJSONObject(j));
                }
            }
            //is orignal for all words
            if (temporginal.size() == words.size())
            {
                    resultorginal.add(new Vector<JSONObject>(temporginal));
            }
            temporginal.clear();
        }
    }


    public Vector<Vector<JSONObject>>  Phrase_Search(String QP_str,Vector<String>snippet_for_all_urls,Vector<Integer> DF) throws JSONException {
        StringBuilder To_remove_space=new StringBuilder(QP_str);
        To_remove_space.deleteCharAt(0);
        To_remove_space.deleteCharAt(QP_str.length()-2);

        QP_str=To_remove_space.toString();
        //Vcetor to be returned
        Vector<Vector<JSONObject>> Phraseurls = new Vector<Vector<JSONObject>>(1);
        queryprocessing d = new queryprocessing();
        Vector<Vector<JSONObject>> resultorginal = new Vector<Vector<JSONObject>>(1);
        Vector<Vector<JSONObject>> resultforms = new Vector<Vector<JSONObject>>(1);
        Vector<Vector<JSONObject>>NonCommon=new Vector<Vector<org.json.JSONObject>>(1);
        Vector<String> phrase_search_words =Phrase_process(QP_str, resultorginal,DF);
        //////////////////////////////
        for (int i = 0; i < resultorginal.size(); i++) {//row
            JSONObject[] docarr = new JSONObject[resultorginal.get(i).size()];
            for (int j = 0; j < resultorginal.get(i).size(); j++) {
                docarr[j] = resultorginal.get(i).get(j).getJSONObject(phrase_search_words.get(j));
            }


            Indexer Id=new Indexer();
            Iterator<String> it=Id.tagsnames.iterator();
            while (it.hasNext()){
                String x=it.next();
                if (docarr[0].has(x)) {
                    boolean has = true;
                    for (int j = 1; j < resultorginal.get(i).size(); j++) {
                        ////////////De bet3mel eh
                        docarr[j] = resultorginal.get(i).get(j).getJSONObject(phrase_search_words.get(j));


                        if (!(docarr[j].has(x))) {
                            has = false;
                            break;
                        }
                    }

                    if (has)
                    {
                        //search array of this tag
                        Vector<JSONArray> tagarr = new Vector<>(1);
//                        tagarr.add(docarr[0].getJSONArray(it.next()));
                        tagarr.add(docarr[0].getJSONArray(x));
                        for (int j = 1; j < docarr.length; j++) {
                            tagarr.add(docarr[j].getJSONArray(x));
                        }
                        //========================================================================================
                       int first_Index=compare_Json_array(tagarr);
                        if (first_Index!=-1)//if true then this url is valid
                        {
                            //add  this row to the resutlst to be returned
                            Phraseurls.add(resultorginal.get(i));
                            String url=resultorginal.get(i).get(0).getString("_url");

                            Document DBresult = database.collection.find(new Document("_url", url)).first();
                            JSONArray body=null;
                            if (DBresult != null) {
                                JSONObject obj = new JSONObject(DBresult.toJson());
                               body = obj.getJSONArray("_body");
                            }
                            snippet_for_all_urls.add(snippet_url(body,first_Index));
                            break;
                        }
                        //else go and find another tag

//                        for(int Q=0;Q<tagarr.size();Q++)
//                System.out.println("tagarr:" + tagarr.get(Q));
//
                    } else
                        continue;
                } else {
                    continue;
                }
            }
            for(int k=0;k<docarr.length;k++)
                System.out.println("docarr:" + docarr[k]);
        }

        return Phraseurls;
    }


   int compare_Json_array(Vector<JSONArray>tagarr) throws JSONException {int k=0;
//        int word_index=0;

        for (;k<tagarr.get(0).length();k++)
        {
            int x= ((int) tagarr.get(0).get(k));
            int j=1;
            for (;j< tagarr.size();j++)
            {
                int i=0;
                for (;i<tagarr.get(j).length();i++)
                {
                    int y= ((int) tagarr.get(j).get(i));

                    if(x+1==y||x==y) {
                        x=y;
//                        word_index++;
                        break;
                    }
                }
                if(i==tagarr.get(j).length())
                {
//                    word_index=0;
                    break;
                }
            }
            if(j==tagarr.size())
            {
                return  ((int) tagarr.get(0).get(k));
            }
        }
        return -1;
    }
    public String snippet_url(JSONArray body,int start_index) throws JSONException
    {
        StringBuffer snippet=new StringBuffer();
        int i=start_index;
        while (i>=0&&!body.getString(i).endsWith("."))
        {
            //snippet.insert(0,body.getString(i)+" ");
            StringBuffer temp=new StringBuffer(body.getString(i));
            temp.reverse();
            snippet.append(temp+" ");
            i--;
        }
        snippet.reverse();
        i=start_index+1;
        if(body.getString(start_index).endsWith("."))
            return snippet.toString();
        while (i<= body.length()-1&&!body.getString(i).endsWith("."))
        {
            snippet.append(body.getString(i)+" ");
            i++;
        }
        if (i<= body.length()-1)
            snippet.append(body.getString(i)+" ");

        return snippet.toString();
    }
    public static void main(String[] args) throws JSONException {

        Vector<String>snippet_for_all_urls =new Vector<String>(0);
        Vector<Integer> DF=new Vector<Integer>(0);

        long time=System.nanoTime();
        System.out.println("\ntimeeee"+time);

//        queryprocessing q=new queryprocessing();
        Phrase_sreach ps=new Phrase_sreach();
        String query="\"template classes\"";
        query=query.trim();

//        Vector<Vector<org.json.JSONObject>> resultorginal=new Vector<Vector<org.json.JSONObject>>(1);
//        Vector<Vector<org.json.JSONObject>>  resultforms=new Vector<Vector<org.json.JSONObject>>(1);
//        Vector<Vector<JSONObject>>NonCommon=new Vector<Vector<org.json.JSONObject>>(1);
//
//        Vector<Integer> NoofDocumentsforword=new Vector<Integer>(1);
        Vector<Vector<JSONObject>> result=new Vector<Vector<org.json.JSONObject>>(1);
        if(query.startsWith("\"") && query.endsWith("\""))
        {
            result= ps.Phrase_Search(query,snippet_for_all_urls,DF);
        }
//        else{
//            q.query_process(query,resultorginal, resultforms, NonCommon,NoofDocumentsforword);
//        }
        long time2 =System.nanoTime();
        System.out.println("\ntimeafter"+(time2-time));


        System.out.println(result.size());
        for(int m=0;m<result.size();m++) {
            for (int k = 0; k < (result.get(m)).size(); k++) {
                System.out.println("Phase search:" + result.get(m).get(k));
            }
        }
        System.out.println(snippet_for_all_urls);


//        for(int m=0;m<resultforms.size();m++)
//            for(int k=0;k<(resultforms.get(m)).size();k++) {
//                System.out.println("steming:" + resultforms.get(m).get(k));
//                System.out.println(m);}
//
//        for(int m=0;m<NonCommon.size();m++)
//            for(int k=0;k<(NonCommon.get(m)).size();k++) {
//                System.out.println("Noncommon:" + NonCommon.get(m).get(k));
//                System.out.println(m);}


    }
}
