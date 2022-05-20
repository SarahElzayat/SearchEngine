package com.example.demo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import opennlp.tools.stemmer.PorterStemmer;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class Phrase_sreach {
    private MongoDB database;
    private MongoDB database_Indexer;
    private PorterStemmer porterStemmer;//stemmer
    private static HashSet<String> stopWords;
    private static HashSet<String> ImportantWords;

    Indexer indexer;
    public HashSet<String> tags;


    //Constructor
    public Phrase_sreach() {
        //connect to DB
        database = new MongoDB("SearchEngine", "URLSWithHTML");
        database_Indexer = new MongoDB("SearchEngine", "Indexer");
        porterStemmer = new PorterStemmer();
        stopWords = Indexer.getStopWords();
        ImportantWords = Indexer.getImportantword();


        indexer = new Indexer();
        tags = indexer.tagsnames;

    }

    //Gets Urls that contain the Phrase, The snippets ,The DF for each Word
    //return -1 if one No website conatin one of the words of the Phrase and 0 sucess
    //But Note still the Original Results may be empty if the Phrase isn't found in The URLS
    public int Phrase_Search(String QP_str, HashMap<String, JSONObject> Original_Results, HashMap<String, String> snippet_for_all_urls, Vector<Integer> DF) throws JSONException {
        long time01 = System.currentTimeMillis();

        Vector<String> Steam_Words_Arr = new Vector<>();
        //remove quotations
        StringBuilder To_remove_space = new StringBuilder(QP_str);
        To_remove_space.deleteCharAt(0);
        To_remove_space.deleteCharAt(QP_str.length() - 2);
        QP_str = To_remove_space.toString();

        //Preprocessing the Query
        String[] words = (QP_str).toLowerCase().split("\\s+");//splits the string based on whitespace
        Vector<String> finalword = new Vector<String>(0);
        Vector<String>QueryStringBinary=new Vector<>(0);

        for (int i = 0; i < words.length; i++) {
            if (!ImportantWords.contains(words[i])) {
                words[i] = words[i].replaceAll("[^a-zA-Z0-9]", " ");
                //System.out.println(words[i]);
                String[] subwords = words[i].split("\\s+");//splits the string based on whitespace
                for (String sw : subwords) {
                    if (stopWords.contains(sw)) {
//                        finalword.add(sw);
                        QueryStringBinary.add(sw);
                        continue;
                    }
                    String stemw = porterStemmer.stem(sw);
                    Steam_Words_Arr.add(stemw);
                    QueryStringBinary.add("");
                    finalword.add(sw);
                }
            } else {
                String stemw = porterStemmer.stem(words[i]);
                Steam_Words_Arr.add(stemw);
                QueryStringBinary.add("");
                finalword.add(words[i]);
            }
        }//end of loop

        //get Documents for those final word
        JSONArray[] docarr_Array = new JSONArray[finalword.size()];
        FindIterable<Document> DBresult = database_Indexer.collection.find(new Document("_id", new BasicDBObject("$in", (Steam_Words_Arr))));
        MongoCursor<Document> iterator = DBresult.iterator();
        int counter_for_Documents_from_DB = 0;
        while (iterator.hasNext()) {
            counter_for_Documents_from_DB++;
            String s = iterator.next().toJson();
            JSONObject Jsonobj = new JSONObject(s);
            String id = Jsonobj.getString("_id");//stem word
            int index = Steam_Words_Arr.indexOf(id);
            docarr_Array[index] = Jsonobj.getJSONArray("DOC");
            DF.add(Jsonobj.getInt("DF"));
        }
        if (counter_for_Documents_from_DB != finalword.size()||counter_for_Documents_from_DB==0)
            return -1;


        phrase_Search_work(finalword,QueryStringBinary, new Vector<JSONArray>(List.of(docarr_Array)), Original_Results, snippet_for_all_urls);
        long time03 = System.currentTimeMillis();
        System.out.println("\nTime to phrase query:" + (time03 - time01));
        return 1;
    }


    //*********************************************Private Functions
    private void phrase_Search_work(Vector<String> words,Vector<String>QueryStringBinary ,Vector<JSONArray> docarr, HashMap<String, JSONObject> Original_Results, HashMap<String, String> snippet_for_all_urls) throws JSONException {
        HashMap<String, HashMap<String, JSONObject>> Inedxer_Results = new HashMap();
        Vector<JSONObject> Temp_Original = new Vector<JSONObject>(0);


        //unique urls
        HashMap<String, Integer> Urls = new HashMap<String, Integer>();
        //loop over each word
        //O(n2)
        long time1 = System.currentTimeMillis();
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
        long time2 = System.currentTimeMillis();
        long loop1 = time2 - time1;


//        long time3 =System.currentTimeMillis();
//        long time4 =System.currentTimeMillis();
//        System.out.println("\nloop2:"+(time4-time3));
        //filing bodies and getting snippets
        //get Documents for those final word
        long time3 = System.currentTimeMillis();
        //HashMap<String,JSONArray>bodies=new HashMap<>();
        HashMap<String, String> bodies = new HashMap<>();
        FindIterable<Document> DBresult = database.collection.find(new Document("_id", new BasicDBObject("$in", Urls.keySet())));
        MongoCursor<Document> iterator = DBresult.iterator();
        JSONArray body = null;
        Document doc = null;
        while (iterator.hasNext()) {
            doc = iterator.next();
//            JSONObject Jsonobj = new JSONObject(s);
//            String url=Jsonobj.getString("_id");
//            bodies.put(url,Jsonobj.getJSONArray("_body"));
            bodies.put(doc.getString("_id"), doc.getString("_body"));
        }
        long time4 = System.currentTimeMillis();
        long loop2 = time4 - time3;


        //loop over all urls
        //O(n2)
        long totaltime = 0;
        long time5 = System.currentTimeMillis();
        long totaltime1 = 0;
        for (Map.Entry<String, Integer> set : Urls.entrySet()) {
            //each url
            Boolean common = true;
            String url = set.getKey();
            Integer freq = set.getValue();
            //loop over all words in the query
            long time20 = System.currentTimeMillis();
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
            long time30 = System.currentTimeMillis();
            totaltime1 = time20 - time30;


            //tik=ll here this url is common betweeen the 2 words
            long time40 = System.currentTimeMillis();
            boolean Valid_URL = false;
            if (Temp_Original.size() == words.size())//Valid URL
            {
                //for this url check it has the Phrase

                Iterator<String> it = tags.iterator();
                Vector<JSONArray> TagArray = new Vector<>(0);
                Vector<Integer> Tags_Indexes = new Vector<Integer>(0);
                //loop over all tags ==> to get common Tag & Phrase Word
                //O(n2)
                JSONObject Weights_Of_Phrase = new JSONObject();
                Boolean PhraseURL=false;
                while (it.hasNext()) {
                    String Tag = it.next();
                    if (Temp_Original.get(0).has(Tag)) {
                        TagArray.add(Temp_Original.get(0).getJSONArray(Tag));

                        boolean has = true;
                        boolean Done_Snippet = false;
                        //loop over other words ==> to see if this tag is common
                        for (int k = 1; k < Temp_Original.size(); k++) {
                            if (!Temp_Original.get(k).has((Tag))) {
                                has = false;
                                break;
                            }
                            TagArray.add(Temp_Original.get(k).getJSONArray(Tag));
                        }
                        if (has) {
                            //check order
                            int first_Index = compare_Json_array(TagArray, Tags_Indexes,bodies.get(url),QueryStringBinary);
                            long Time10 = System.currentTimeMillis();
                            if (first_Index != -1)//if true then this url is valid
                            {
                                //add  this row to the resutlst to be returned
                                Weights_Of_Phrase.put(Tag, new JSONArray(Tags_Indexes));
                                if(PhraseURL==false)
                                   snippet_for_all_urls.put(url, snippet_url(bodies.get(url), first_Index));
                                PhraseURL=true;
                                // Original_Results.put(url, new Vector<>(Temp_Original));
//                                 //getting Snippet
//                                String s =shosho.get(url).toJson();
//                                JSONObject Jsonobj = new JSONObject(s);
                                // snippet_for_all_urls.add(snippet_url(Jsonobj.getJSONArray("_body"), first_Index));
                                long Time11 = System.currentTimeMillis();
                                totaltime += Time11 - Time10;
                                /////////////////////
                                // break;//break out of loop of searching for tags
                            }
                            //else//this Tag doesn't contain this Phrase==> go to another tag

                        }
                        TagArray.clear();
                        Tags_Indexes.clear();
                    }
                }//end of loop of tags

                if(PhraseURL==true)
                  Original_Results.put(url,Weights_Of_Phrase);
            }
            //else{} //==>Invlaid URL

            Temp_Original.clear();
        }//end loop of urls
        System.out.println("\nTotal==>:" + totaltime);
        long time6 = System.currentTimeMillis();
        System.out.println("\nloop1" + loop1);
        System.out.println("\nloop2" + loop2);
        System.out.println("\nloop3:" + (time6 - time5));


    }


    private int compare_Json_array(Vector<JSONArray> tagarr, Vector<Integer> Indexes_Of_Tags,String body_String,Vector<String>Query_Words) throws JSONException {
        //  ""   "and"   ""
        //The "" "" ""
        String[] body = body_String.split("\\s+");

        Boolean start_with_stop_word = false;
        //special case if the first word is stop word
        int index_Query_words = 0;
        while (!Query_Words.get(index_Query_words).equals("")) {
            start_with_stop_word = true;//stop word
            index_Query_words++;
        }


        int l=1;

        int k = 0;
        for (; k < tagarr.get(0).length(); k++) {
            int x = ((int) tagarr.get(0).get(k));
            if(start_with_stop_word==true)
            {
                int st;
                for(st=1;st<=index_Query_words;st++) {
                    if ((x-st) < 0)
                        break;
                    if (!(body[x - st].toLowerCase()).equals(Query_Words.get(0))) {
                        break;
                    }
                    l++;
                }
                if(st!=index_Query_words+1)
                    continue;
            }

            int j = 1;
            for (; l < Query_Words.size(); l++){
                if(!Query_Words.get(l).equals(""))
                {
                    //stop words
                    if(x+1==body.length)
                        break;
                    if((body[x+1].toLowerCase()).equals(Query_Words.get(l)))
                    {
                        x = x+1;
                    }
                    else if((body[x].toLowerCase()).equals(Query_Words.get(l))); //right
                    else break;

//                    continue;
                }
                //Not stop word
                else {
                    int y = next_number_greater_than_or_equal_target(tagarr.get(j), x);
                    j++;
                    if (x + 1 == y || x == y)
                        x = y;
                    else
                        break;
                }
            }
            if (l == Query_Words.size()) {
//                    return  ((int) tagarr.get(0).get(k));
                Indexes_Of_Tags.add((int) tagarr.get(0).get(k));
            }
        }
        if (Indexes_Of_Tags.size() == 0)
            return -1;
        return Indexes_Of_Tags.get(0);
    }


    private static int next_number_greater_than_or_equal_target(JSONArray arr, int target) throws JSONException {
        int start = 0, end = arr.length() - 1;

        int ans = -1;
        while (start <= end) {
            int mid = (start + end) / 2;

            // Move to right side if target is
            // greater.
            if ((int) arr.get(mid) < target) {
                start = mid + 1;
            }

            // Move left side.
            else {
                ans = (int) arr.get(mid);
                end = mid - 1;
            }
        }
        return ans;
    }

    //Get Snippet from the Start index(It is Logically correct Sentence )
//    private String snippet_url(JSONArray body,int start_index) throws JSONException {
    private String snippet_url(String body_String, int start_index) throws JSONException {
        String[] body = body_String.split("\\s+");

        StringBuffer snippet = new StringBuffer();
        int i = start_index;
        StringBuffer temp2 = new StringBuffer(body[start_index]);
        temp2.reverse();
        snippet.append(temp2 + "****** ");
        i--;
        int counter=0;
        while (i >= 0 && counter!=10) {
            //snippet.insert(0,body.getString(i)+" ");
            StringBuffer temp = new StringBuffer(body[i]);
            temp.reverse();
            snippet.append(temp + " ");
            i--;
            counter++;
        }
        snippet.reverse();
        snippet.append(" ");
        snippet.deleteCharAt(0);//remove space;
        i = start_index + 1;
        if (body[start_index].endsWith("."))
            return snippet.toString();
        counter=0;
        while (i <= body.length - 1 &&counter!=10) {
            snippet.append(body[i] + " ");
            i++;
            counter++;
        }
        if (i <= body.length - 1)
            snippet.append(body[i] + " ");

        return snippet.toString();
    }


    ///***************************************MAIN for Test***********************************////
    public static void main(String[] args) throws JSONException {

//        HashMap<String,Vector<JSONObject>> Original_Results=new HashMap<>();
//        HashMap<String,String>snippet_for_all_urls =new HashMap<>(0);
////        Vector<String>snippet_for_all_urls =new Vector<String>(0);
//        Vector<Integer> DF=new Vector<Integer>(0);
//
//        long time1=System.currentTimeMillis();
//        Phrase_sreach ps=new Phrase_sreach();
//        String query="\"first-class\"";
//        query=query.trim();
//
//        if(query.startsWith("\"") && query.endsWith("\"")) {
//            ps.Phrase_Search(query, Original_Results, snippet_for_all_urls, DF);
//        }
//        long time2 =System.currentTimeMillis();
//        System.out.println("\nTime"+(time2-time1));
//
//
//        System.out.println("No of Urls:"+Original_Results.size());
//        Iterator<String> it=Original_Results.keySet().iterator();
//        int i=-1;
//        while(it.hasNext())
//        {
//            i++;
//            String url=it.next();
//            Vector<JSONObject>Documnets_for_this_url=Original_Results.get(url);
//            System.out.println("URL:"+url);
//            for (int k = 0; k < Documnets_for_this_url.size(); k++) {
//                System.out.println("Phase search:" + Documnets_for_this_url.get(k));
//            }
//            System.out.println(snippet_for_all_urls.get(url)+"\n\n");//snippet
//        }
    }
}
