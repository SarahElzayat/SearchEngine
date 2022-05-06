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
        database=new MongoDB("Search_Engine","URLSWithHTML");
    }
    public Vector<Vector<JSONObject>>  Phraseprocess(String QP_str) throws JSONException {
        //Vcetor to be returned
        Vector<Vector<JSONObject>> Phraseurls = new Vector<Vector<JSONObject>>(1);
        queryprocessing d = new queryprocessing();
        Vector<Vector<JSONObject>> resultorginal = new Vector<Vector<JSONObject>>(1);
        Vector<Vector<JSONObject>> resultforms = new Vector<Vector<JSONObject>>(1);
        Vector<String> phrase_search_words = d.query_process(QP_str, resultorginal, resultforms);
        for (int i = 0; i < resultorginal.size(); i++) {//row
            JSONObject[] docarr = new JSONObject[resultorginal.get(i).size()];
            for (int j = 0; j < resultorginal.get(i).size(); j++) {
                docarr[j] = resultorginal.get(i).get(j).getJSONObject(phrase_search_words.get(j));
            }
            for (int k = 0; k < Indexer.tags.length; k++) {
                if (docarr[0].has(Indexer.tags[k])) {
                    boolean has = true;
                    for (int j = 1; j < resultorginal.get(i).size(); j++) {
                        ////////////De bet3mel eh
                        docarr[j] = resultorginal.get(i).get(j).getJSONObject(phrase_search_words.get(j));

                        if (!(docarr[j].has(Indexer.tags[k]))) {
                            has = false;
                            break;
                        }
                    }

                    if (has) {
                        //search array of this tag
                        Vector<JSONArray> tagarr = new Vector<>(1);
                        tagarr.add(docarr[0].getJSONArray(Indexer.tags[k]));
                        for (int j = 1; j < docarr.length; j++) {
                            tagarr.add(docarr[j].getJSONArray(Indexer.tags[k]));
                        }
                        //========================================================================================
                        if (compare_Json_array(tagarr))//if true then this url is valid
                        {
                            //add  this row to the resutlst to be returned
                            Phraseurls.add(resultorginal.get(i));
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
    boolean compare_Json_array(Vector<JSONArray>tagarr) throws JSONException {int k=0;
        for (;k<tagarr.get(0).length();k++)
        {
            int j=1;
            for (;j< tagarr.size();j++)
            {
                int i=0;
                for (;i<tagarr.get(j).length();i++)
                {
                    int x= ((int) tagarr.get(0).get(k));
                    int y= ((int) tagarr.get(j).get(i));

                    if(x+j==y)
                        break;
                }
                if(i==tagarr.get(j).length())
                    break;
            }
            if(j!=tagarr.size()-1)
                return true;

        }
        return false;
    }
}
