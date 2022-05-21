package com.example.demo;

import com.mongodb.client.*;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class Ranker {
    //Results
    HashMap<String, Vector<JSONObject>> originalResults = new HashMap<>();
    HashMap<String, JSONObject> phraseSearchResults = new HashMap<>();
    HashMap<String, String> snippetsForPhraseSearch = new HashMap<String, String>(0);
    HashMap<String, Vector<JSONObject>> stemmedResults = new HashMap<>();
    HashMap<String, Vector<JSONObject>> nonCommonResults = new HashMap<>();
    HashMap<String, Vector<String>> snippets = new HashMap<>(0);

    queryprocessing queryProcessor = new queryprocessing();
    Phrase_sreach PhraseSearch = new Phrase_sreach();

    MongoCollection<Document> rankerCollection;
    MongoDatabase db;

    Vector<String> searchWords = new Vector<String>(0);

    HashMap<String, Document> URLSWithHTMLDatabase = new HashMap<String, Document>(5007);

    public Ranker() {
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        db = mongo.getDatabase("SearchEngine");
        rankerCollection = db.getCollection("Ranker");

        /*Retrieves the URLSWithHTML database to avoid unnecessary overhead*/
        FindIterable<Document> crawlerDatabase = db.getCollection("URLSWithHTML").find();

        for (Document doc : crawlerDatabase) {
            URLSWithHTMLDatabase.put(doc.get("_id").toString(), doc);
        }
    }

    /*This function ranks the phrase search results*/
    public void phraseSearchRanker(String q) throws JSONException {

        String url = new String();
        Iterator<String> it = phraseSearchResults.keySet().iterator(); //an iterator for the urls that should be ranked

        int noOfURLS = phraseSearchResults.size();

        String query = q.replaceAll("\"", ""); //gets rid of ""
        StringBuilder builder = new StringBuilder(query.toLowerCase());
        builder.append(" ");
        /*adds space to the end of the query to avoid finding the query in the middle of another word
         ex -> finding new in newspaper
         */

        //iterating over the urls
        while (it.hasNext()) {

            url = it.next();

            JSONObject weights = phraseSearchResults.get(url); // gets the JSON Object associated with the current URL

            float rank = 0, headers = 0;

            StringBuilder title = new StringBuilder(URLSWithHTMLDatabase.get(url).get("title").toString().toLowerCase());
            title.append(" ");

            /*checks if the searching query exists in title, headers or paragraph,
            adds its corresponding weight to the rank of the url*/

            if (title.toString().contains(builder))
                headers += 100;

            if (weights.has("h1"))
                headers += weights.getJSONArray("h1").length() * 12;

            if (weights.has("h2"))
                headers += weights.getJSONArray("h2").length() * 6;

            if (weights.has("h3"))
                headers += weights.getJSONArray("h3").length() * 3;

            if (weights.has("h4"))
                headers += weights.getJSONArray("h4").length() * 1.5;

            if (weights.has("h5"))
                headers += weights.getJSONArray("h5").length();

            if (weights.has("h6"))
                headers += weights.getJSONArray("h6").length();

            if (weights.has("p"))
                headers += weights.getJSONArray("p").length() * .5;

            rank = headers;
            rank /= Integer.parseInt(URLSWithHTMLDatabase.get(url).get("NoOfWords").toString());//normalizes TF
            rank *= Math.log(5000.0 / noOfURLS);//Multiplies by DF
            rank *= Integer.parseInt(URLSWithHTMLDatabase.get(url).get("popularity").toString()); //Multiplies by the popularity of the URL


            Document temp = new Document();
            temp.append("url", url);//s=>url
            temp.append("header", URLSWithHTMLDatabase.get(url).get("title"));
            temp.append("paragraph", snippetsForPhraseSearch.get(url));
            temp.append("rank", rank);

            rankerCollection.insertOne(temp);//inserts the ranked result into the database

        }

    }

    public void originalResultsRanker(HashMap<String, Vector<JSONObject>> vec) throws JSONException {

        String url = new String();

        Iterator<String> it = vec.keySet().iterator();//iterator for search results

        float rank = 0;

        while (it.hasNext()) {

            url = it.next(); //current result url

            float internalRank = 0; //internal rank calculates the rank for each url inside the for loop

            Vector<JSONObject> weights = vec.get(url);//gets the corresponding weights of the search query in the current url

            for (int i = 0; i < weights.size(); i++) {

                /*checks if the searching query exists in title, headers or paragraph,
                adds its corresponding weight to the rank of the url*/
                float headers = 0; //stores weights of title, headers ana paragraphs for each word

                StringBuilder searchWord = new StringBuilder(searchWords.get(i));
                searchWord.append(" ");//adding space to the end of the word to avoid finding it in another word (new)spaper

                StringBuilder title = new StringBuilder(URLSWithHTMLDatabase.get(url).get("title").toString().toLowerCase());
                title.append(" ");

                if (title.toString().toLowerCase().replaceAll("\\p{Punct}", "").contains(searchWord))
                //getting rid of all the punctuation in the title to search for the search query word
                {
                    headers += StringUtils.countMatches(title.toString(), searchWord.toString()) * 100;
                }

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h1"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h1").length() * 12;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h2"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h2").length() * 6;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h3"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h3").length() * 3;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h4"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h4").length() * 1.5;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h5"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h5").length() * 1;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("h6"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("h6").length() * 1;

                if (weights.get(i).getJSONObject(searchWords.get(i)).has("p"))
                    headers += weights.get(i).getJSONObject(searchWords.get(i)).getJSONArray("p").length() * .5;

                internalRank = headers;
                internalRank /= Integer.parseInt(URLSWithHTMLDatabase.get(url).get("NoOfWords").toString());//Normalizes DF

                //sets a better, approximated DF for website with a small number of words
                if (Integer.parseInt(URLSWithHTMLDatabase.get(url).get("NoOfWords").toString()) < 50)
                    internalRank /= 10;

                internalRank *= Math.log(5000.0 / vec.size());//Multiplies by normalized DF

                internalRank *= Integer.parseInt(URLSWithHTMLDatabase.get(url).get("popularity").toString());

                rank += internalRank;//adds the rank of each word to the total rank

            }
            Document temp = new Document();
            temp.append("url", url);//s=>url
            temp.append("header", URLSWithHTMLDatabase.get(url).get("title"));
            temp.append("paragraph", snippets.get(url));
            temp.append("rank", rank);
            temp.append("type", 1);//indicates "Original urls"
            rankerCollection.insertOne(temp);//inserts the ranked result into the database
            rank = 0;
        }

    }

    public void stemmedAndNonCommonRanker(HashMap<String, Vector<JSONObject>> vec, int x) throws JSONException {

        // x = 0 -> stemmed , x = 1 -> non common

        String url = new String();

        Iterator<String> it = vec.keySet().iterator(); //iterator for search results

        float rank = 0;

        while (it.hasNext()) {

            url = it.next(); // current result url

            float internalRank = 0;//internal rank calculates the rank for each url inside the for loop

            Vector<JSONObject> weights = vec.get(url);//gets the corresponding weights of the search query in the current url

            for (int i = 0; i < weights.size(); i++) {
                  /*checks if the searching query exists in title, headers or paragraph,
                    adds its corresponding weight to the rank of the url*/

                float headers = 0;
                Iterator<String> it2 = weights.get(i).keys();//iterates over the search words

                while (it2.hasNext()) {
                    String form = it2.next();

                    if (form.equals("_url"))
                        continue;           //skips the url field in the JSON Object

                    if (weights.get(i).getJSONObject(form).has("h1"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h1").length() * 12;

                    if (weights.get(i).getJSONObject(form).has("h2"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h2").length() * 6;

                    if (weights.get(i).getJSONObject(form).has("h3"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h3").length() * 3;

                    if (weights.get(i).getJSONObject(form).has("h4"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h4").length() * 1.5;

                    if (weights.get(i).getJSONObject(form).has("h5"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h5").length() * 1;

                    if (weights.get(i).getJSONObject(form).has("h6"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("h6").length() * 1;

                    if (weights.get(i).getJSONObject(form).has("p"))
                        headers += weights.get(i).getJSONObject(form).getJSONArray("p").length() * .5;
                }
                internalRank = headers;

                internalRank /= Integer.parseInt(URLSWithHTMLDatabase.get(url).get("NoOfWords").toString());//Normalizes TF
                //Normalizes DF depending on the results being ranked
                if (x == 0)//stemmed
                    internalRank *= Math.log(5000.0 / vec.size());
                else//non common
                    internalRank *= Math.log(5000.0 / vec.size()) / searchWords.size();//approximation for DF

                internalRank *= Integer.parseInt(URLSWithHTMLDatabase.get(url).get("popularity").toString());

                rank += internalRank;

            }
            if (x == 0)
                rank /= 5.0;//divides the rank of stemmed results so it'd rank after the original ones
            else
                rank /= 10.0;//divides the rank of non common results so it'd rank after the stemmed ones

            Document temp = new Document();
            temp.append("url", url);//s=>url
            temp.append("header", URLSWithHTMLDatabase.get(url).get("title"));
            temp.append("paragraph", snippets.get(url));
            temp.append("rank", rank);
            if (x == 0)
                temp.append("type", 2);
            else
                temp.append("type", 3);
            rankerCollection.insertOne(temp);
            rank = 0;
        }

    }


    public long getResults(String query) throws JSONException {

        long Time1 = System.currentTimeMillis();//gets the start time of the processing

        if (query.startsWith("\"") && query.endsWith("\""))//checks for phrase search
        {
            if (PhraseSearch.Phrase_Search(query, phraseSearchResults, snippetsForPhraseSearch) == -1)//checks for empty results
            {
                return 0;
            }

            phraseSearchRanker(query);
        }

        else //regular searching
        {
            searchWords = queryProcessor.query_process(query, originalResults, stemmedResults, nonCommonResults, snippets);
            if (searchWords == null) //if all search words are stop words
                return 0;
            originalResultsRanker(originalResults);
            stemmedAndNonCommonRanker(stemmedResults, 0);
            stemmedAndNonCommonRanker(nonCommonResults, 1);


        }
        //clears hashmaps to get rid of the results for new search queries
        originalResults.clear();
        nonCommonResults.clear();
        stemmedResults.clear();
        phraseSearchResults.clear();
        snippetsForPhraseSearch.clear();
        snippets.clear();

        long Time2 = System.currentTimeMillis();//gets the time at the end of searching and ranking

        return Time2 - Time1; //returns the total time
    }

    public static void main(String[] args) throws JSONException {
        Ranker r = new Ranker();
        r.getResults("\"first-class\"");

    }

}
