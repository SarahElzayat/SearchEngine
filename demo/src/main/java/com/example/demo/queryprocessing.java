package com.example.demo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import opennlp.tools.stemmer.PorterStemmer;
import org.bson.Document;

import java.util.*;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;


public class queryprocessing {
    //Data Members
    private PorterStemmer porterStemmer;//stemmer

    private static HashSet<String> stopWords;
    private static HashSet<String> ImportantWords;


    //MongoDB Class Objects
    private MongoDB Search_Engine_Database;

    //Collections
    public MongoCollection<Document> database_Indexer;
    public MongoCollection<Document> database_Crawler;

    HashMap<String, String> bodies = new HashMap<>();
    public HashSet<String> tags;


    //1.Constructor
    public queryprocessing() {
        //connect to Search Engine DB
        Search_Engine_Database=new MongoDB("SearchEngine");

        //Collections
        database_Crawler=Search_Engine_Database.GetCollection("URLSWithHTML");
        database_Indexer=Search_Engine_Database.GetCollection("Indexer");

        porterStemmer = new PorterStemmer();

        stopWords = Indexer.getStopWords();
        ImportantWords = Indexer.getImportantword();

        tags.add("h1");
        tags.add("h2");
        tags.add("h3");
        tags.add("h4");
        tags.add("h5");
        tags.add("h6");
        tags.add("p");
    }

    //=====================================================================================================//
    //Query processing
    /*input:QP_str
    output:Original_Results, Steam_Results, NonCommon_Results,Snippets
    return: QP_str after preprocessing
    * */
    public Vector<String> query_process(String QP_str, HashMap<String, Vector<JSONObject>> Original_Results, HashMap<String, Vector<JSONObject>> Steam_Results, HashMap<String, Vector<JSONObject>> NonCommon_Results, HashMap<String, Vector<String>> Snippets) throws JSONException
    {
        //Intermiediate variables
        Boolean Notfound = false;
        Vector<String> Steam_Words_Arr = new Vector<>();
        Vector<String> finalword = new Vector<String>(0);

        //split query
        String[] words = (QP_str).toLowerCase().split("\\s+");//splits the string based on whitespace


        //preprocessing the query
        //NB Exactly like in The Indexer
        for (int i = 0; i < words.length; i++) {
            //loop over the words of the query
            if (!ImportantWords.contains(words[i])) {
                words[i] = words[i].replaceAll("[^a-zA-Z0-9]", " ");
                String[] subwords = words[i].split("\\s+");//splits the string based on whitespace
                for (String sw : subwords) {
                    //further, sub words
                    if (stopWords.contains(sw)) {
                        continue;
                    }
                    String stem_Word = porterStemmer.stem(sw);
                    Steam_Words_Arr.add(stem_Word);
                    finalword.add(sw);
                }
            } else {
                String stem_Word = porterStemmer.stem(words[i]);
                Steam_Words_Arr.add(stem_Word);
                finalword.add(words[i]);
            }
        }//end of loop of words of query


        //get documents of each word in the query words
        JSONArray[] docarr_Array = new JSONArray[finalword.size()];
        FindIterable<Document> DBresult = database_Indexer.find(new Document("_id", new BasicDBObject("$in", (Steam_Words_Arr))));
        MongoCursor<Document> iterator = DBresult.iterator();
        int counter_for_Documents_from_DB = 0;

        //loop over documents of words of this query in the DB
        while (iterator.hasNext())
        {
            counter_for_Documents_from_DB++;
            String s = iterator.next().toJson();
            JSONObject Jsonobj = new JSONObject(s);
            String id = Jsonobj.getString("_id");//stem word
            int index = Steam_Words_Arr.indexOf(id);
            docarr_Array[index] = Jsonobj.getJSONArray("DOC");
            if(index+1<Steam_Words_Arr.size())
            {
                index = Steam_Words_Arr.indexOf(id, index + 1);
                while(index!=-1)
                {
                    docarr_Array[index] = Jsonobj.getJSONArray("DOC");
                    index = Steam_Words_Arr.indexOf(id, index + 1);

                }
            }
        }//end of while loop
        // ==>docarr_Array:["id":work,"DF":1,"DOC":["url1":"http..","worker":{"p":[1,6],"h1":[7,8]},"working":{..}]

        if (counter_for_Documents_from_DB != finalword.size())
            Notfound = true;//there is word that isn't found in the Indexer

        if (counter_for_Documents_from_DB ==0)
            return null;//all those words aren't found in indexer

        //call Query_process work
        //fill Original_Results ,Steam_Results,NonCommon_Results form docarr_Array
        query_process_work(finalword, new Vector<JSONArray>(List.of(docarr_Array)), Original_Results, Steam_Results, NonCommon_Results, Notfound);

        extract_Common_Array_tags(finalword, Original_Results, Snippets);
        extract_Steaming_Array_tags(Steam_Results, Snippets);
        extract_Steaming_Array_tags(NonCommon_Results, Snippets);

        return finalword;
    }



    //==========================================================Private Functions=======================//

    private void query_process_work(Vector<String> words, Vector<JSONArray> docarr, HashMap<String, Vector<JSONObject>> Original_Results, HashMap<String, Vector<JSONObject>> Steam_Results, HashMap<String, Vector<JSONObject>> NonCommon_Results, boolean not_Found) throws JSONException
    {
        //Intermediate variables
        HashMap<String, Integer> Urls = new HashMap<String, Integer>();
        HashMap<String, HashMap<String, JSONObject>> Inedxer_Results = new HashMap();

        Vector<JSONObject> Temp_Original = new Vector<JSONObject>(0);
        Vector<JSONObject> Temp_Steam = new Vector<JSONObject>(0);
        Vector<JSONObject> Temp_NonCommon = new Vector<JSONObject>(0);

        //Getting URLS of each word together in one hashMap
        //loop over each word
        for (int j = 0; j < docarr.size(); j++) {
            if (docarr.get(j) == null)
                continue;
            HashMap<String, JSONObject> HashMap_Word = new HashMap<>();
            //loop over each url for this word
            for (int k = 0; k < docarr.get(j).length(); k++) {
                String url = docarr.get(j).getJSONObject(k).getString("_url");
                HashMap_Word.put(url, docarr.get(j).getJSONObject(k));
                //getting Frequency
                Integer freq = Urls.get(url);
                if (freq == null)
                    freq = 0;
                freq++;
                Urls.put(url, freq);
            }
            Inedxer_Results.put(words.get(j), HashMap_Word);
        } //end of loop of URLS
        /*Urls:
         * "http" :2
         * Inedexer_Results:
         * "word1":    "http"
         * "word2":    "http"
         * */

        //===================================filing bodies of these URLS===================================//
        FindIterable<Document> DBresult = database_Crawler.find(new Document("_id", new BasicDBObject("$in", Urls.keySet())));
        MongoCursor<Document> iterator = DBresult.iterator();
        JSONArray body = null;
        Document doc = null;
        while (iterator.hasNext()) {
            doc = iterator.next();
            bodies.put(doc.getString("_id"), doc.getString("_body"));
        }//end of loop of bodies


        //loop over all urls
        for (Map.Entry<String, Integer> set : Urls.entrySet()) {
            String url = set.getKey();
            Integer freq = set.getValue();

            //loop over all words in the query
            for (int i = 0; i < docarr.size(); i++) {
                String Query_Word = words.get(i);
                //Does this word have this URL??!
                JSONObject word_Url = Inedxer_Results.get(Query_Word).get(url);
                if (word_Url == null) {
                    //this word isn't in this url
                    //No action
                } else {
                    //this word is in this url
                    if (freq < words.size())//non common
                    {
                        Temp_NonCommon.add(word_Url);
                    } else if (word_Url.has(Query_Word))//has original form like word in the query
                    {
                        Temp_Original.add(word_Url);
                    } else {
                        //this word's stem is in the url
                        Temp_Steam.add(word_Url);
                    }
                }
            }//end of loop of words of query

            if (Temp_NonCommon.size() != 0) {//Non Common URL==>Add to NonCommon_Results
                NonCommon_Results.put(url, new Vector<JSONObject>(Temp_NonCommon));
            }
            else if (Temp_Original.size() == words.size())//Original URL
            {
                if (not_Found)//there is word in the query NOT found in the DB==>NonCommon URL
                    NonCommon_Results.put(url, new Vector<JSONObject>(Temp_Original));
                else//Common URL with the Original form
                    Original_Results.put(url, new Vector<JSONObject>(Temp_Original));
            } else {
                //Steamed
                Temp_Steam.addAll(Temp_Original);
                if (not_Found)//there is word in the query NOT found in the DB==>NonCommon URL
                    NonCommon_Results.put(url, new Vector<JSONObject>(Temp_Steam));
                else//Common URL with the Steamed form
                    Steam_Results.put(url, new Vector<JSONObject>(Temp_Steam));
            }
            Temp_Original.clear();
            Temp_NonCommon.clear();
            Temp_Steam.clear();
        }//end of loop of URLS
    }

    //=====================================================================================================//

    private void extract_Common_Array_tags(Vector<String> finalword, HashMap<String, Vector<JSONObject>> resultorginal, HashMap<String, Vector<String>> Snipptes) throws JSONException
    {
        //loop over Original results
        for (Map.Entry<String, Vector<JSONObject>> url_entry : resultorginal.entrySet()) {
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();

            Vector<Integer> Start_index = new Vector<Integer>(1);
            JSONObject[] docarr = new JSONObject[Words_Documents.size()];

            for (int j = 0; j < Words_Documents.size(); j++) {
                docarr[j] = Words_Documents.get(j).getJSONObject(finalword.get(j));
                Iterator<String> it = tags.iterator();
                while (it.hasNext()) {
                    String x = it.next();
                    if (docarr[j].has(x)) {
                        JSONArray indexes = docarr[j].getJSONArray(x);
                        Start_index.add((Integer) indexes.get(0));
                        break;
                    }
                }
            }//end of loop
            Snipptes.put(url, snippet_url(bodies.get(url), Start_index));
        }//end of main loop
    }

    //=====================================================================================================//
    private void extract_Steaming_Array_tags(HashMap<String, Vector<JSONObject>> resultforms, HashMap<String, Vector<String>> Snipptes) throws JSONException
    {
        //loop over Original results
        for (Map.Entry<String, Vector<JSONObject>> url_entry : resultforms.entrySet()) {
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();
            Vector<Integer> Start_index = new Vector<Integer>(1);
            JSONObject[] docarr = new JSONObject[Words_Documents.size()];

            for (int j = 0; j < Words_Documents.size(); j++) {
                Iterator<String> iter = Words_Documents.get(j).keys();
                String key = iter.next().toString();
                while (key.equals("_url")) {
                    key = iter.next().toString();
                }
                docarr[j] = Words_Documents.get(j).getJSONObject(key);
                Iterator<String> it = tags.iterator();
                while (it.hasNext()) {
                    String x = it.next();
                    if (docarr[j].has(x)) {
                        JSONArray indexes = docarr[j].getJSONArray(x);
                        Start_index.add((Integer) indexes.get(0));
                        break;
                    }
                }
            }
            Snipptes.put(url, snippet_url(bodies.get(url), Start_index));
        }
    }

    //=====================================================================================================//
    //Extract Snippet from a body starting from start index
    private Vector<String> snippet_url(String body_String, Vector<Integer> Start_index) throws JSONException
    {
        String[] body = body_String.split("\\s+");//splitting body
        Vector<String> all_Sinppet = new Vector<>(1);

        //loop over Start Indexes to get snippets @ them
        for (int j = 0; j < Start_index.size(); j++) {
            //getting one snippet if the previous snippets contains this word
            if (j > 0) {
                int k = 0;
                for (; k < all_Sinppet.size(); k++) {

                    if (all_Sinppet.get(k).toLowerCase().contains((body[Start_index.get(j)]).toLowerCase()))
                        break;
                }//end of loop
                //word is previously i another snippet
                if (k != all_Sinppet.size())
                    continue;
            }//end of if

            //Building a snippet
            StringBuffer snippet = new StringBuffer();
            int i = Start_index.get(j);
            StringBuffer temp2 = new StringBuffer(body[i]);
            temp2.reverse();
            snippet.append("\""+temp2 + "\" ");

            //10 words before this word
            i--;
            int counter=0;
            while (i >= 0 && counter!=10) {
                //snippet.insert(0,body.getString(i)+" ");
                StringBuffer temp = new StringBuffer(body[i]);
                temp.reverse();
                snippet.append(temp + " ");
                counter++;
                i--;
            }//end of while loop
            snippet.reverse();
            snippet.append(" ");

            //10 words after this word
            i = Start_index.get(j) + 1;
            counter=0;
            while (i <= body.length - 1 &&counter!=10) {
                snippet.append(body[i] + " ");
                i++;
                counter++;
            }//end of while loop

            if (i <= body.length - 1)
                snippet.append(body[i] + " ");

            all_Sinppet.add(snippet.toString());
        } //end of loop over Start Indexes to get snippets @ them
        return all_Sinppet;
    }


    //==============================================Main====================================================//
    public static void main(String[] args) throws JSONException {

        queryprocessing q = new queryprocessing();
        String query = "virus";
        query = query.trim();


        HashMap<String, Vector<JSONObject>> Original_Results = new HashMap<>();
        HashMap<String, Vector<JSONObject>> Steam_Results = new HashMap<>();
        HashMap<String, Vector<JSONObject>> NonCommon_Results = new HashMap<>();

        HashMap<String, Vector<String>> Snipptes = new HashMap<String, Vector<String>>();
        if (query.startsWith("\"") && query.endsWith("\"")) {
            //  phraseSearch.Phraseprocess(query);
        } else {
            long time = System.currentTimeMillis();
            q.query_process(query, Original_Results, Steam_Results, NonCommon_Results, Snipptes);
            long time2 = System.currentTimeMillis();
            System.out.println("\ntime:" + (time2 - time));
        }


        int m = -1;

        System.out.println(Original_Results.size());
        for (Map.Entry<String, Vector<JSONObject>> url_entry : Original_Results.entrySet()) {
            m++;
            //some url
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();
            for (int k = 0; k < Words_Documents.size(); k++) {
                System.out.println("Orignal:" + Words_Documents.get(k));
            }
            System.out.println(Snipptes.get(url));
        }


        System.out.println(Steam_Results.size());
        m = -1;
        for (Map.Entry<String, Vector<JSONObject>> url_entry : Steam_Results.entrySet()) {
            m++;
            //some url
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();
            for (int k = 0; k < Words_Documents.size(); k++) {
                System.out.println("Steamed:" + Words_Documents.get(k));
            }
            System.out.println(Snipptes.get(url));
        }


        System.out.println(NonCommon_Results.size());
        m = -1;
        for (Map.Entry<String, Vector<JSONObject>> url_entry : NonCommon_Results.entrySet()) {
            m++;
            //some url
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();
            for (int k = 0; k < Words_Documents.size(); k++) {
                System.out.println("NonCommon:" + Words_Documents.get(k));
            }
            System.out.println(Snipptes.get(url));
        }
    }
}