package com.example.demo;
/*import IndexerPackage.Indexer;
 *//*import QueryProcessing.queryprocessing;*//*
import MongoDBPackage.MongoDB;*/
/*import QueryProcessing.queryprocessing;*/
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
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





    //Constructor
    public Phrase_sreach() throws JSONException {
        //connect to DB
        database=new MongoDB("SearchEngine","URLSWithHTML");
        database_Indexer=new MongoDB("SearchEngine","Indexer");
        porterStemmer = new PorterStemmer();
        stopWords=Indexer.getStopWords();
        ImportantWords=Indexer.getImportantword();

    }
    //Gets Urls that contain the Phrase, The snippets ,The DF for each Word
    //return -1 if one No website conatin one of the words of the Phrase and 0 sucess
    //But Note still the Original Results may be empty if the Phrase isn't found in The URLS
    public int Phrase_Search(String QP_str, HashMap<String,Vector<JSONObject>> Original_Results,Vector<String>snippet_for_all_urls,Vector<Integer>DF,HashMap<String, Document> shosho ) throws JSONException {
        long time1 =System.currentTimeMillis();

        Vector<String>Steam_Words_Arr=new Vector<>();
        //remove quotations
        StringBuilder To_remove_space=new StringBuilder(QP_str);
        To_remove_space.deleteCharAt(0);
        To_remove_space.deleteCharAt(QP_str.length()-2);
        QP_str=To_remove_space.toString();

        //Preprocessing the Query
        String[] words = (QP_str).toLowerCase().split("\\s");//splits the string based on whitespace
        Vector<String>finalword=new Vector<String>(0);
        Vector<JSONArray>docarr=new Vector<JSONArray>(0);
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
                    Steam_Words_Arr.add(stemw);
                    finalword.add(sw);
//                    Document DBresult = database_Indexer.collection.find(new Document("_id", stemw)).first();
//                    if (DBresult != null)
//                    {
//                        // System.out.println(stemw);
//                        JSONObject obj = new JSONObject(DBresult.toJson());
//                        JSONArray arr = obj.getJSONArray("DOC");
//
//                        DF.add(obj.getInt("DF"));
//                        docarr.add(arr);
//                    }
//                    else{
//                       // Notfound=true;
//                       return  -1;
//
                }
            }
            else{
                String stemw = porterStemmer.stem(words[i]);
                Steam_Words_Arr.add(stemw);
                finalword.add(words[i]);
            }
        }//end of loop

        //get Documents for those final word
        FindIterable<Document>  DBresult = database_Indexer.collection.find(new Document("_id", new BasicDBObject("$in",(Steam_Words_Arr))));
        MongoCursor<Document> iterator = DBresult.iterator();
        int counter_for_Documents_from_DB=0;
        while (iterator.hasNext()) {
            counter_for_Documents_from_DB++;
            String s = iterator.next().toJson();
            JSONObject Jsonobj = new JSONObject(s);
            docarr.add(Jsonobj.getJSONArray("DOC"));
            DF.add(Jsonobj.getInt("DF"));
        }
        if(counter_for_Documents_from_DB!=finalword.size())
            return -1;

//            for(int k=0;k<docarr.size();k++)
//                System.out.println("docarr:" + docarr.get(k));


        long time2 =System.currentTimeMillis();
        System.out.println("\nTime after our work:"+(time2-time1));
        phrase_Search_work(finalword,docarr,Original_Results,snippet_for_all_urls,shosho);
        long time3 =System.currentTimeMillis();
        System.out.println("\nTime here:"+(time3-time2));
        return 1;
    }


    //*********************************************Private Functions
    //Phrase Search Work ==>Original Results(URLS that Contain Exact Phrase) , Snippet fo each URL
    private void phrase_Search_work(  Vector<String>words, Vector<JSONArray> docarr,  HashMap<String,Vector<JSONObject>> Original_Results,Vector<String>snippet_for_all_urls,HashMap<String, Document> shosho) throws JSONException
    {
        HashMap<String, HashMap<String, JSONObject>> Inedxer_Results = new HashMap();
        Vector<JSONObject> Temp_Original = new Vector<JSONObject>(0);


        //unique urls
        HashMap<String, Integer> Urls = new HashMap<String, Integer>();
        //loop over each word
        //O(n2)
        long time1 =System.currentTimeMillis();
        for (int j = 0; j < docarr.size(); j++){
            HashMap<String, JSONObject> HashMap_Word = new HashMap<>();
            //loop over each url for this word
            for (int k = 0; k < docarr.get(j).length(); k++)
            {
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
        long time2 =System.currentTimeMillis();
        System.out.println("\nloop1:"+(time2-time1));




//        long time3 =System.currentTimeMillis();
//        long time4 =System.currentTimeMillis();
//        System.out.println("\nloop2:"+(time4-time3));
        //filing bodies and getting snippets
        //get Documents for those final word
//        FindIterable<Document>  DBresult = database.collection.find(new Document("_url", new BasicDBObject("$in",Urls.keySet())));
//        MongoCursor<Document> iterator = DBresult.iterator();
//        JSONArray body = null;
//        while (iterator.hasNext()) {
//            String s = iterator.next().toJson();
//            JSONObject Jsonobj = new JSONObject(s);
//            String url=Jsonobj.getString("_url");
//            bodies.put(url,Jsonobj.getJSONArray("_body"));
//        }
//        long time4 =System.currentTimeMillis();
//        System.out.println("\nloop2:"+(time4-time3));


        //loop over all urls
        //O(n2)

        long totaltime=0;
        long time5 =System.currentTimeMillis();
        for (Map.Entry<String, Integer> set : Urls.entrySet()) {
            Boolean common = true;
            String url = set.getKey();
            Integer freq = set.getValue();
            //loop over all words in the query
            for (int i = 0; i < docarr.size(); i++) {
                //loop over all urls of this word to compare with url
                //Getting hashmap of this word
                String Query_Word = words.get(i);
                JSONObject word_Url = Inedxer_Results.get(Query_Word).get(url);
                if (word_Url == null) {//this word isn't in this url
//                    common=false;
                } else {//this word is in this url
                    if (freq < words.size()) {//non common
                        common = false;
                        break;
                    } else if (word_Url.has(Query_Word)) {//has original form like word in the query
                        Temp_Original.add(word_Url.getJSONObject(Query_Word));
                    } else {//this word's stem is in the ur//Invalid URL
                        break;
                    }
                }
            }

            boolean Valid_URL=false;
            if (Temp_Original.size() == words.size())//Valid URL
            {
                //for this url check it has the Phrase
                Indexer Id=new Indexer();
                Iterator<String> it=Id.tagsnames.iterator();
                Vector<JSONArray> TagArray = new Vector<>(0);
                //loop over all tags ==> to get common Tag & Phrase Word
                //O(n2)
                while (it.hasNext()) {
                    String Tag = it.next();
                    if (Temp_Original.get(0).has(Tag)) {
                        TagArray.add(Temp_Original.get(0).getJSONArray(Tag));

                        boolean has = true;
                        boolean Done_Snippet=false;
                        //loop over other words ==> to see if this tag is common
                        for (int k = 1; k < Temp_Original.size(); k++) {
                            if (!Temp_Original.get(k).has((Tag))) {
                                has = false;
                                break;
                            }
                            TagArray.add(Temp_Original.get(k).getJSONArray(Tag));
                        }
                        if (has)
                        {
                            //check order
                            int first_Index = compare_Json_array(TagArray);
                            long Time10=System.currentTimeMillis();
                            if (first_Index != -1)//if true then this url is valid
                            {
                                //add  this row to the resutlst to be returned
                                Original_Results.put(url, new Vector<>(Temp_Original));
                                 //getting Snippet
                                String s =shosho.get(url).toJson();
                                JSONObject Jsonobj = new JSONObject(s);
                               // snippet_for_all_urls.add(snippet_url(bodies.get(url), first_Index))
                                long Time11=System.currentTimeMillis();
                                totaltime+=Time11-Time10;
                                 snippet_for_all_urls.add(snippet_url(Jsonobj.getJSONArray("_body"), first_Index));

                                break;//break out of loop of searching for tags
                            }
                            //else//this Tag doesn't contain this Phrase==> go to another tag

                        }
                        TagArray.clear();
                    }
                }//end of loop of tags

            }
            //else{} //==>Invlaid URL

            Temp_Original.clear();
        }//end loop of urls
        System.out.println("\nTotal==>:"+totaltime);
        long time6 =System.currentTimeMillis();
        System.out.println("\nloop3:"+(time6-time5));


    }



    //Compare Json Arrays to find consecutive numbers
    private int compare_Json_array(Vector<JSONArray>tagarr) throws JSONException {int k=0;
        for (;k<tagarr.get(0).length();k++)
        {
            int x= ((int) tagarr.get(0).get(k));
            int j=1;
            for (;j< tagarr.size();j++)
            {
                int y=next_number_greater_than_or_equal_target(tagarr.get(j),x);
                if(x+1==y||x==y)
                    x = y;
                else
                    break;
            }
            if(j==tagarr.size())
            {
                return  ((int) tagarr.get(0).get(k));
            }
        }
        return -1;
    }


    private static int next_number_greater_than_or_equal_target(JSONArray arr, int target) throws JSONException {
            int start = 0, end = arr.length() - 1;

            int ans = -1;
            while (start <= end) {
                int mid = (start + end) / 2;

                // Move to right side if target is
                // greater.
                if ((int)arr.get(mid) < target) {
                    start = mid + 1;
                }

                // Move left side.
                else {
                    ans = (int)arr.get(mid);
                    end = mid - 1;
                }
            }
            return ans;
        }

    //Get Snippet from the Start index(It is Logically correct Sentence )
    private String snippet_url(JSONArray body,int start_index) throws JSONException {
        StringBuffer snippet=new StringBuffer();
        int i=start_index;
        while (i>=0&&!body.getString(i).endsWith(".")&&!body.getString(i).endsWith(",")&&!body.getString(i).endsWith("-"))
        {
            //snippet.insert(0,body.getString(i)+" ");
            StringBuffer temp=new StringBuffer(body.getString(i));
            temp.reverse();
            snippet.append(temp+" ");
            i--;
        }
        snippet.reverse();
        snippet.append(" ");
        snippet.deleteCharAt(0);//remove space;
        i=start_index+1;
        if(body.getString(start_index).endsWith("."))
            return snippet.toString();
        while (i<= body.length()-1&&!body.getString(i).endsWith(".")&&!body.getString(i).endsWith(",")&&!body.getString(i).endsWith("-"))
        {
            snippet.append(body.getString(i)+" ");
            i++;
        }
        if (i<= body.length()-1)
            snippet.append(body.getString(i)+" ");

        return snippet.toString();
    }



    ///***************************************MAIN for Test***********************************////
    public static void main(String[] args) throws JSONException {

        HashMap<String,Vector<JSONObject>> Original_Results=new HashMap<>();
        Vector<String>snippet_for_all_urls =new Vector<String>(0);
        Vector<Integer> DF=new Vector<Integer>(0);

        long time1=System.currentTimeMillis();
        Phrase_sreach ps=new Phrase_sreach();
        String query="\"virus\"";
        query=query.trim();

        if(query.startsWith("\"") && query.endsWith("\"")) {
//            ps.Phrase_Search(query, Original_Results, snippet_for_all_urls, DF);
        }
        long time2 =System.currentTimeMillis();
        System.out.println("\nTime"+(time2-time1));


        System.out.println("No of Urls:"+Original_Results.size());
        Iterator<String> it=Original_Results.keySet().iterator();
        int i=-1;
        while(it.hasNext())
        {
            i++;
            String url=it.next();
            Vector<JSONObject>Documnets_for_this_url=Original_Results.get(url);
            System.out.println("URL:"+url);
            for (int k = 0; k < Documnets_for_this_url.size(); k++) {
                System.out.println("Phase search:" + Documnets_for_this_url.get(k));
            }
            System.out.println(snippet_for_all_urls.get(i)+"\n\n");//snippet
        }
    }
}
