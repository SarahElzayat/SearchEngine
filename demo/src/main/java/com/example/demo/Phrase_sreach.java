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

import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Phrase_sreach
{
    private MongoDB database;
    private PorterStemmer porterStemmer ;//stemmer
    public Phrase_sreach()
    {
        //connect to DB
        database=new MongoDB("SearchEngine","URLSWithHTML");
    }
    public Vector<Vector<JSONObject>>  Phraseprocess(String QP_str,Vector<String>snippet_for_all_urls) throws JSONException {
        StringBuilder To_remove_space=new StringBuilder(QP_str);
        To_remove_space.deleteCharAt(0);
        To_remove_space.deleteCharAt(QP_str.length()-2);

        QP_str=To_remove_space.toString();
        //Vcetor to be returned
        Vector<Vector<JSONObject>> Phraseurls = new Vector<Vector<JSONObject>>(1);
        queryprocessing d = new queryprocessing();
        Vector<Vector<JSONObject>> resultorginal = new Vector<Vector<JSONObject>>(1);
        Vector<Vector<JSONObject>> resultforms = new Vector<Vector<JSONObject>>(1);
        Vector<Integer> NoofDocumentsforword=new Vector<Integer>(1);
        Vector<Vector<JSONObject>>NonCommon=new Vector<Vector<org.json.JSONObject>>(1);
        Vector<String> phrase_search_words = d.query_process(QP_str, resultorginal, resultforms,NonCommon,NoofDocumentsforword);
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
    for (int i=start_index-10;i<=start_index+10;i++)
    {
        snippet.append(body.getString(i)+" ");
    }
    return snippet.toString();
}
    public static void main(String[] args) throws JSONException {

        long time=System.nanoTime();
        System.out.println("\ntimeeee"+time);

//        queryprocessing q=new queryprocessing();
        Phrase_sreach ps=new Phrase_sreach();
        String query="\"first-class\"";
        query=query.trim();

//        Vector<Vector<org.json.JSONObject>> resultorginal=new Vector<Vector<org.json.JSONObject>>(1);
//        Vector<Vector<org.json.JSONObject>>  resultforms=new Vector<Vector<org.json.JSONObject>>(1);
//        Vector<Vector<JSONObject>>NonCommon=new Vector<Vector<org.json.JSONObject>>(1);
//
//        Vector<Integer> NoofDocumentsforword=new Vector<Integer>(1);
        Vector<Vector<JSONObject>> result=new Vector<Vector<org.json.JSONObject>>(1);;
        if(query.startsWith("\"") && query.endsWith("\""))
        {
            Vector<String>snippet_for_all_urls =new Vector<String>(1);
            result= ps.Phraseprocess(query,snippet_for_all_urls);
            System.out.println(snippet_for_all_urls);
        }
//        else{
//            q.query_process(query,resultorginal, resultforms, NonCommon,NoofDocumentsforword);
//        }
        long time2 =System.nanoTime();
        System.out.println("\ntimeafter"+(time2-time));


        System.out.println(result.size());
        for(int m=0;m<result.size();m++)
            for(int k=0;k<(result.get(m)).size();k++) {
                System.out.println("Phase search:" + result.get(m).get(k));
            }


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
