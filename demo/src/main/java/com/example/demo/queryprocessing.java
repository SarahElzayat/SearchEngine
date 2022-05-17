package com.example.demo;
//Database
/*import MongoDBPackage.MongoDB;
import IndexerPackage.Indexer;*/
import ch.qos.logback.core.BasicStatusManager;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import opennlp.tools.stemmer.PorterStemmer;
import org.bson.Document;

import java.util.*;

/*import org.json.JSONException;*/
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

public class queryprocessing {
    private MongoDB database_Indexer;
    private PorterStemmer porterStemmer ;//stemmer
    private static HashSet<String> stopWords ;
    private static HashSet<String> ImportantWords;
    private MongoDB database_Crawler;

    HashMap<String,String>bodies=new HashMap<>();

    public queryprocessing()
    {
        database_Crawler=new MongoDB("SearchEngine","URLSWithHTML");
        database_Indexer=new MongoDB("SearchEngine","Indexer");
        porterStemmer = new PorterStemmer();
        stopWords=Indexer.getStopWords();
        ImportantWords=Indexer.getImportantword();
    }
    public  Vector<String> query_process(String QP_str,HashMap<String,Vector<JSONObject>> Original_Results, HashMap<String, Vector<JSONObject>> Steam_Results,HashMap<String, Vector<JSONObject>> NonCommon_Results,Vector<Integer>DF,Vector<Vector<String>>snippet_for_all_urls,Vector<Vector<String>>snippet_for_all_urls_forms,Vector<Vector<String>>snippet_for_all_urls_Non_Common) throws JSONException {

        Boolean Notfound=false;
        Vector<String>Steam_Words_Arr=new Vector<>();

        HashMap<String ,JSONObject>Snippets_and_DF=new HashMap<>();
        String[] words = (QP_str).toLowerCase().split("\\s+");//splits the string based on whitespace
        Vector<String>finalword=new Vector<String>(1);
        Vector<JSONArray>docarr=new Vector<JSONArray>(1);
        for (int i = 0; i < words.length; i++)
        {
            if (!ImportantWords.contains(words[i]))
            {
                words[i] = words[i].replaceAll("[^a-zA-Z0-9]", " ");
                //System.out.println(words[i]);
                String[] subwords = words[i].split("\\s+");//splits the string based on whitespace
                for (String sw : subwords)
                {
                    if (stopWords.contains(sw))
                    {
//                        finalword.add(sw);
                        continue;
                    }
                    String stemw = porterStemmer.stem(sw);
                    Steam_Words_Arr.add(stemw);
                    finalword.add(sw);

//                    Document DBresult = database.collection.find(new Document("_id", stemw)).first();
//                    if (DBresult != null) {
//                        // System.out.println(stemw);
//                        JSONObject obj = new JSONObject(DBresult.toJson());
//                        JSONArray arr = obj.getJSONArray("DOC");
//                        finalword.add(sw);
//                        DF.add(obj.getInt("DF"));
//                        docarr.add(arr);
//                    }
//                    else{
//                        Notfound=true;
//                    }
                }
            }
            else{
                String stemw = porterStemmer.stem(words[i]);
                Steam_Words_Arr.add(stemw);
                finalword.add(words[i]);
//                Document DBresult = database.collection.find(new Document("_id", stemw)).first();
//                if (DBresult != null) {
//                    JSONObject obj = new JSONObject(DBresult.toJson());
//                    JSONArray arr = obj.getJSONArray("DOC");
//                    docarr.add(arr);
//                    finalword.add(words[i]);
//                }
            }

        }//end of loop


        JSONArray[]docarr_Array=new JSONArray[finalword.size()];
        FindIterable<Document> DBresult = database_Indexer.collection.find(new Document("_id", new BasicDBObject("$in",(Steam_Words_Arr))));
        MongoCursor<Document> iterator = DBresult.iterator();
        int counter_for_Documents_from_DB=0;
        while (iterator.hasNext())//loop over words of this query in the DB
             {
            counter_for_Documents_from_DB++;
            String s = iterator.next().toJson();
            JSONObject Jsonobj = new JSONObject(s);
            String id=Jsonobj.getString("_id");//stem word
            int index= Steam_Words_Arr.indexOf(id);
            docarr_Array[index]=Jsonobj.getJSONArray("DOC");
            DF.add(Jsonobj.getInt("DF"));
        }
        if(counter_for_Documents_from_DB!=finalword.size())
            Notfound=true;



//            for(int k=0;k<docarr.size();k++)
//                System.out.println("docarr:" + docarr.get(k));
        query_process_work(finalword,docarr,Original_Results,Steam_Results,NonCommon_Results,Notfound);
        extract_Common_Array_tags(finalword,Original_Results,snippet_for_all_urls);
        extract_Steaming_Array_tags(Steam_Results,snippet_for_all_urls_forms);
        extract_Steaming_Array_tags(NonCommon_Results,snippet_for_all_urls_Non_Common);
        return finalword;
    }
    private void query_process_work( Vector<String>words, Vector<JSONArray> docarr,  HashMap<String, Vector<JSONObject>> Original_Results, HashMap<String, Vector<JSONObject>> Steam_Results,HashMap<String, Vector<JSONObject>> NonCommon_Results, boolean not_Found) throws JSONException {

        HashMap<String, HashMap<String, JSONObject>> Inedxer_Results = new HashMap();

//        HashMap<String, Vector<JSONObject>> Original_Results = new HashMap<String, Vector<JSONObject>>();
//        HashMap<String, Vector<JSONObject>> Steam_Results = new HashMap<String, Vector<JSONObject>>();
//        HashMap<String, Vector<JSONObject>> NonCommon_Results = new HashMap<String, Vector<JSONObject>>();


        Vector<JSONObject> Temp_Original = new Vector<JSONObject>(0);
        Vector<JSONObject> Temp_Steam = new Vector<JSONObject>(0);
        Vector<JSONObject> Temp_NonCommon = new Vector<JSONObject>(0);

        //unique urls
        HashMap<String, Integer> Urls = new HashMap<String, Integer>();
        //loop over each word
        //O(n2)
        for (int j = 0; j < docarr.size(); j++) {
            if(docarr.get(j)==null)
                continue;
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

        //filing bodies and getting snippets
        long time3 =System.currentTimeMillis();
        FindIterable<Document>  DBresult = database_Crawler.collection.find(new Document("_id", new BasicDBObject("$in",Urls.keySet())));
        MongoCursor<Document> iterator = DBresult.iterator();
        JSONArray body = null;
        Document doc=null;
        while (iterator.hasNext()) {
            doc=iterator.next();
            bodies.put(doc.getString("_id"),doc.getString("_body"));
        }
        long time4 =System.currentTimeMillis();
        long loop2=time4-time3;


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

//    private void extract_Common_Array_tags(  Vector<String>finalword,Vector<Vector<org.json.JSONObject>> resultorginal,Vector<Vector<String>>snippet_for_all_urls) throws JSONException
    private void extract_Common_Array_tags(  Vector<String>finalword,HashMap<String,Vector<JSONObject>>resultorginal ,Vector<Vector<String>>snippet_for_all_urls) throws JSONException
    {
        for (Map.Entry<String, Vector<JSONObject>> url_entry : resultorginal.entrySet())
        {
            //some url
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents= url_entry.getValue();

            Vector<Integer>Start_index=new Vector<Integer>(1);
            JSONObject[] docarr = new JSONObject[Words_Documents.size()];
            for (int j = 0; j < Words_Documents.size(); j++)
            {
                docarr[j] =Words_Documents.get(j).getJSONObject(finalword.get(j));
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
            snippet_for_all_urls.add(snippet_url(bodies.get(url),Start_index));

        }
    }
    private void extract_Steaming_Array_tags(  HashMap<String,Vector<JSONObject>> resultforms,Vector<Vector<String>>snippet_for_all_urls_forms) throws JSONException
    {


        for (Map.Entry<String, Vector<JSONObject>> url_entry : resultforms.entrySet()) {
            //some url
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();


            Vector<Integer> Start_index = new Vector<Integer>(1);
            JSONObject[] docarr = new JSONObject[Words_Documents.size()];
            for (int j = 0; j < Words_Documents.size(); j++) {
                Iterator<String> iter = Words_Documents.get(j).keys();
                String key = iter.next().toString();
                docarr[j] = Words_Documents.get(j).getJSONObject(key);
                Indexer Id = new Indexer();
                Iterator<String> it = Id.tagsnames.iterator();
                while (it.hasNext()) {
                    String x = it.next();
                    if (docarr[j].has(x)) {
                        JSONArray indexes = docarr[j].getJSONArray(x);
                        Start_index.add((Integer) indexes.get(0));
                        break;
                    }
                }
            }
            snippet_for_all_urls_forms.add(snippet_url(bodies.get(url), Start_index));
        }
    }

    public Vector<String> snippet_url(String body_String,Vector<Integer>Start_index) throws JSONException
    {
        String[]body=body_String.split("\\s+");
        Vector<String>all_Sinppet=new Vector<>(1);
        for (int j=0;j<Start_index.size();j++) {
            if(j>0)
            {
                int k=0;
                for (;k<all_Sinppet.size();k++)
                {

                    if(all_Sinppet.get(k).toLowerCase().contains((body[Start_index.get(j)]).toLowerCase()))
                        break;
                }
                if(k!=all_Sinppet.size())
                    continue;
            }
            StringBuffer snippet = new StringBuffer();
            int i = Start_index.get(j);
            while (i >= 0 && !body[i].endsWith(".")) {
                //snippet.insert(0,body.getString(i)+" ");
                StringBuffer temp = new StringBuffer(body[i]);
                temp.reverse();
                snippet.append(temp + " ");
                i--;
            }
            snippet.reverse();
            snippet.append(" ");
            i = Start_index.get(j) + 1;
            if (body[Start_index.get(j)].endsWith("."))
                all_Sinppet.add(snippet.toString());
            else {


                while (i <= body.length - 1 && !body[i].endsWith(".")) {
                    snippet.append(body[i] + " ");
                    i++;
                }
                if (i <= body.length - 1)
                    snippet.append(body[i] + " ");

                all_Sinppet.add(snippet.toString());
            }
        }
        return all_Sinppet;
    }
    public static void main(String[] args) throws JSONException {

        queryprocessing q = new queryprocessing();
        String query = "virus";
        query = query.trim();

//        Vector<Vector<org.json.JSONObject>> resultorginal = new Vector<Vector<org.json.JSONObject>>(1);
//        Vector<Vector<org.json.JSONObject>> resultforms = new Vector<Vector<org.json.JSONObject>>(1);
//        Vector<Vector<JSONObject>> NonCommon = new Vector<Vector<org.json.JSONObject>>(1);

        HashMap<String, Vector<JSONObject>> Original_Results=new HashMap<>();
        HashMap<String, Vector<JSONObject>> Steam_Results=new HashMap<>();
        HashMap<String, Vector<JSONObject>> NonCommon_Results=new HashMap<>();

        Vector<Vector<String>> snippet_for_all_urls = new Vector<Vector<String>>(1);
        Vector<Integer> DF = new Vector<Integer>(1);
        Vector<Vector<String>> snippet_for_all_urls_forms = new Vector<Vector<String>>(1);
        Vector<Vector<String>> snippet_for_all_urls_Non_common = new Vector<Vector<String>>(1);
        if (query.startsWith("\"") && query.endsWith("\"")) {
            //  phraseSearch.Phraseprocess(query);
        } else {
            long time = System.currentTimeMillis();
            q.query_process(query, Original_Results, Steam_Results, NonCommon_Results, DF, snippet_for_all_urls, snippet_for_all_urls_forms, snippet_for_all_urls_Non_common);
            long time2 = System.currentTimeMillis();
            System.out.println("\ntime:" + (time2 - time));
        }


        int m=-1;

        System.out.println(Original_Results.size());
        for (Map.Entry<String, Vector<JSONObject>> url_entry : Original_Results.entrySet()) {
            m++;
            //some url
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();
            for (int k = 0; k < Words_Documents.size(); k++) {
                System.out.println("Orignal:" + Words_Documents.get(k));
            }
            System.out.println(snippet_for_all_urls_forms.get(m));
        }


        System.out.println(Steam_Results.size());
        m=-1;
        for (Map.Entry<String, Vector<JSONObject>> url_entry : Steam_Results.entrySet()) {
            m++;
            //some url
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();
            for (int k = 0; k < Words_Documents.size(); k++) {
                System.out.println("Steamed:" + Words_Documents.get(k));
            }
            System.out.println(snippet_for_all_urls_forms.get(m));
        }




        System.out.println(NonCommon_Results.size());
        m=-1;
        for (Map.Entry<String, Vector<JSONObject>> url_entry : NonCommon_Results.entrySet()) {
            m++;
            //some url
            String url = url_entry.getKey();
            Vector<JSONObject> Words_Documents = url_entry.getValue();
            for (int k = 0; k < Words_Documents.size(); k++) {
                System.out.println("NonCommon:" + Words_Documents.get(k));
            }
            System.out.println(snippet_for_all_urls_Non_common.get(m));
        }
    }

}
