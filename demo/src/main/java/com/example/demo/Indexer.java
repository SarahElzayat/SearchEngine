//package IndexerPackage;
package com.example.demo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

//Elements
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//Database
/*import MongoDBPackage.MongoDB;*/
import org.bson.Document;
//Stemmer
import opennlp.tools.stemmer.PorterStemmer;
//get from DB
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Projections;
import java.lang.String;
import com.mongodb.client.model.Sorts;//sort

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;//eq
import com.mongodb.client.model.Updates;//update





public class Indexer {
    //private String[] tags={"h1","h2","h3","h4","h5","h6","p","abbr","article","div","header","label","q","title","thead","th","textarea","sub","caption","td","li","option","dt"};
   static public String[] tags={"title","h1","h2","h3","h4","h5","h6","p"};

    private static HashSet<String> stopWords ;
    private static HashSet<String> ImportantWords;

    private PorterStemmer porterStemmer;//stemmer

    private MongoDB database_Index;
    private MongoDB database_Crawler;

    Indexer()
    {
        //load stopwords
        porterStemmer = new PorterStemmer();
        stopWords= getStopWords();
        ImportantWords= getImportantword();;

        //connect to DB
        database_Index=new MongoDB("Search_Engine","Indexer");
        database_Crawler=new MongoDB("Search_Engine","URLSWithHTML");
    }

    public void Index(String url,String source_str) throws IOException {
        int no_Of_Words=0;
        HtmlParsing HTML_PAR=new HtmlParsing(source_str);
        for (String tag : tags) {
            Elements paragraphs =HTML_PAR.Parse_Tags(tag);//all tags
            //each eleemtn
            int position=1;
            for (Element p : paragraphs) {
                String[] words = (p.text()).toLowerCase().split("\\s");//splits the string based on whitespace

                //each word in the par
                for (int i=0;i<words.length; i++) {//all words

                    if (!ImportantWords.contains(words[i]))
                    {
                        words[i] = words[i].replaceAll("[^a-zA-Z0-9]", " ");
                        String[] subwords = words[i].split("\\s");//splits the string based on whitespace
                        for (int j=0;j<subwords.length; j++)
                        {
                            if(subwords[j]==null||subwords[j].trim().isEmpty())
                            {
                                continue;
                            }
                            if (stopWords.contains(subwords[j]))
                            {
                                position++;
                                continue;
                            }

                            no_Of_Words+=addWordtoDB(words[i], url, tag,position);
                            position++;
                        }
                    }
                    else
                    {
                        no_Of_Words+=addWordtoDB(words[i], url, tag,position);
                        position++;
                    }
                }
            }

        }
//        addWordtoDB("zeinab","WWW.Google.COM","p");
//        MongoCursor<Document> cursor = database_Index.collection.find().iterator();
//        while (cursor.hasNext()) {
//            System.out.println("collection is " +cursor.next() );}

        Bson filter=eq("_url",url);
        Bson update2 = Updates.set("NOOFWORDS",no_Of_Words);
        database_Crawler.collection.updateMany(filter, update2);
    }

    private int addWordtoDB(String word,String url,String tag,int position )
    {
        //steaming the word
        int no_ofnewword=0;
        String stemword = porterStemmer.stem(word);//hello
        Bson fillter=and(eq("_id",stemword),eq("DOC.url",url));
        Bson update=Updates.addToSet("DOC.$."+word+"."+tag,position);
        UpdateResult Up_result= database_Index.collection.updateMany(fillter,update);
        if(Up_result.getMatchedCount()==0)//new url or word
        {
            // new_url
            Document tagdoc=new Document(tag,Arrays.asList(position));////////////////////////////////
            Document doc=new Document("url",url).append(word,tagdoc);
            fillter=eq("_id",stemword);
            update=Updates.combine(Updates.push("DOC",doc),Updates.inc("DF",1));
            Up_result= database_Index.collection.updateMany(fillter,update);

            if(Up_result.getMatchedCount()==0)//new  word
            {
                tagdoc=new Document(tag,Arrays.asList(position));
                Document arrdoc=new Document("url",url).append(word,tagdoc);
                doc=new Document("_id",stemword).append("DF",1).append("DOC", Arrays.asList(arrdoc));
                database_Index.collection.insertOne(doc);
                no_ofnewword=1;
            }
        }
        return  no_ofnewword;
    }
   public void Index_crawlar() throws IOException {
        FindIterable<Document> itratdoc=database_Crawler.collection.find();
        for (Document d:itratdoc)
        {
           Index(d.get("_url").toString(),d.get("html").toString());
        }
    }
    public static HashSet<String>  getStopWords() {
        HashSet<String> stopw=new HashSet<String>();
        try {
            File websitesFile = new File("./stopwords.txt");
            Scanner reader = new Scanner(websitesFile);
            while (reader.hasNextLine()) {
                String word = reader.nextLine();
                stopw.add(word);
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("An exception occured while reading the stop words!");
            e.printStackTrace();
        }
        return stopw;
    }
    public static HashSet<String> getImportantword() {
        HashSet<String> impword=new HashSet<String>();
        try {
            File websitesFile = new File("./important.txt");
            Scanner reader = new Scanner(websitesFile);
            while (reader.hasNextLine()) {
                String word = reader.nextLine();
                impword.add(word);
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("An exception occured while reading the stop words!");
            e.printStackTrace();
        }
        return impword;
    }
}
