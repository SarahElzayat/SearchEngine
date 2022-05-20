//package IndexerPackage;
package com.example.demo;
import java.io.File;
import java.io.IOException;
import java.util.*;

//Elements
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.jsoup.Jsoup;
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

import com.mongodb.client.model.Updates;//update

import static com.mongodb.client.model.Filters.*;


public class Indexer {
    //private String[] tags={"h1","h2","h3","h4","h5","h6","p","abbr","article","div","header","label","q","title","thead","th","textarea","sub","caption","td","li","option","dt"};
    //static public String[] tags={"title","h1","h2","h3","h4","h5","h6","p"};

    private static HashSet<String> stopWords ;
    private static HashSet<String> ImportantWords;

    private PorterStemmer porterStemmer;//stemmer

    private MongoDB database_Index;
    private MongoDB database_Crawler;
    private MongoDB database_Old_Crawler;


    public HashSet<String>tagsnames;
    Indexer()
    {
        //load stopwords
        porterStemmer = new PorterStemmer();
        stopWords= getStopWords();
        ImportantWords= getImportantword();;
        tagsnames=new HashSet<String>();
        tagsnames.add("h1");
        tagsnames.add("h2");
        tagsnames.add("h3");
        tagsnames.add("h4");
        tagsnames.add("h5");
        tagsnames.add("h6");
        tagsnames.add("p");

        //connect to DB
        database_Index=new MongoDB("SearchEngine","Indexer");
        database_Crawler=new MongoDB("SearchEngine","URLSWithHTML");
        database_Old_Crawler = new MongoDB("SearchEngine","OldSeeds");
    }

    public void Index(String url,String source_str) throws IOException {
        StringBuffer body_String=new StringBuffer("");
        int no_Of_Words=0;
        int position=0;
        org.jsoup.nodes.Document doc= Jsoup.parse(source_str,"UTF-8");
        Elements bodyElements=doc.body().select("*");//select al tags in the body

//        Elements Tags = doc.select("title , h1 , h2 , h3 , h4 , h5 , h6 , p");
        //loop over all elements
        for(Element element:bodyElements)
        {
            //basam Hate
            if(element.ownText().isEmpty())
                continue;
            String[] word = (element.ownText().split("\\s+"));//splits the string based on whitespace

            String tag=element.tagName();
//            List<String>words=Arrays.asList(word) ;
            boolean take=false;
            if (!tagsnames.contains(tag)) {

                if(tagsnames.contains(element.parent().tagName())) {
                    tag=element.parent().tagName();
                }
                else continue;
            }
//            Bson update2 = Updates.pushEach("_body",words);
//            database_Crawler.collection.updateMany(filter, update2);
            //each word in the par
            for (int i = 0; i < word.length; i++) {//all words
                body_String.append(word[i]+" ");
                String search_word=word[i].toLowerCase();
                search_word=search_word.trim();

                if (!ImportantWords.contains(search_word))
                {
                    search_word =search_word.replaceAll("[^a-zA-Z0-9]", " ");
                    String[] subwords = search_word.split("\\s+");//splits the string based on whitespace
                    for (int j = 0; j < subwords.length; j++) {
                        search_word=subwords[j];
//                        if (search_word == null || search_word.trim().isEmpty()) {
//                            continue;
//                        }
                        if (stopWords.contains(search_word)) {
                            no_Of_Words++;
                            continue;
                        }

                        addWordtoDB(search_word, url, tag, position);
                        no_Of_Words++;
                    }
                    position++;
                } else {
                    addWordtoDB(search_word, url, tag, position);
                    no_Of_Words++;
                    position++;
                }
            }
        }

//        addWordtoDB("zeinab","WWW.Google.COM","p");
//        MongoCursor<Document> cursor = database_Index.collection.find().iterator();
//        while (cursor.hasNext()) {
//            System.out.println("collection is " +cursor.next() );}
        String body;
        if(body_String.length()==0)
            body=body_String.toString();
        else
            body=body_String.deleteCharAt(body_String.length()-1).toString();
        Bson filter=eq("_id",url);
        Bson update2=Updates.combine(Updates.set("NoOfWords",no_Of_Words),Updates.set("_body",body));
        database_Crawler.collection.updateMany(filter, update2);
    }


    private void addWordtoDB(String word,String url,String tag,int position )
    {
        //steaming the word
        String stemword = porterStemmer.stem(word);//hello
        Bson fillter=and(eq("_id",stemword),eq("DOC._url",url));
        Bson update=Updates.addToSet("DOC.$."+word+"."+tag,position);
        UpdateResult Up_result= database_Index.collection.updateMany(fillter,update);
        if(Up_result.getMatchedCount()==0)//new url or word
        {
            // new_url
            Document tagdoc=new Document(tag,Arrays.asList(position));////////////////////////////////
            Document doc=new Document("_url",url).append(word,tagdoc);
            fillter=eq("_id",stemword);
            update=Updates.combine(Updates.push("DOC",doc),Updates.inc("DF",1));
            Up_result= database_Index.collection.updateMany(fillter,update);

            if(Up_result.getMatchedCount()==0)//new  word
            {
                tagdoc=new Document(tag,Arrays.asList(position));
                Document arrdoc=new Document("_url",url).append(word,tagdoc);
                doc=new Document("_id",stemword).append("DF",1).append("DOC", Arrays.asList(arrdoc));
                database_Index.collection.insertOne(doc);

            }
        }
    }


    public void Index_crawlar() throws IOException
    {
        int i=0;
        FindIterable<Document> itratdoc=database_Crawler.collection.find();
        for (Document d:itratdoc)
        {
            Index(d.get("_id").toString(),d.get("html").toString());
            i++;
            System.out.println("\n\n"+i+"\n\n");
        }
    }

    public static void main(String[] args) throws IOException {
        Indexer indexer=new Indexer();
//        indexer.Index_crawlar();


        //reindexing
        indexer.ReIndex_Crawlar_New_URLS();
        return;
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

    public void ReIndex_Crawlar_New_URLS() throws IOException {

        //Remove prvious version
        FindIterable<Document> itratdoc_to_remove=database_Old_Crawler.collection.find();
        Vector<String>old_URLs=new Vector<>();
        int j=0;
        for (Document d:itratdoc_to_remove)
        {
            String url=d.get("_id").toString();
            ReIndex_URL(url,d.get("html").toString());
            old_URLs.add(url);
            j++;
            System.out.println("\n\n"+j+"\n\n");
        }
        //drop this database
        database_Old_Crawler.collection.drop();


        int i=0;
        FindIterable<Document> itratdoc=database_Crawler.collection.find().limit(6);
        for (Document d:itratdoc)
        {
            Index(d.get("_id").toString(),d.get("html").toString());
            i++;
            System.out.println("\n\n"+i+"\n\n");
        }
    }



    public void ReIndex_URL(String url,String source_str) throws IOException {
        //StringBuffer body_String=new StringBuffer("");
        //int no_Of_Words=0;
        //int position=0;
        org.jsoup.nodes.Document doc= Jsoup.parse(source_str,"UTF-8");
        Elements bodyElements=doc.body().select("*");//select al tags in the body

//        Elements Tags = doc.select("title , h1 , h2 , h3 , h4 , h5 , h6 , p");
        //loop over all elements
        for(Element element:bodyElements)
        {
            //basam Hate
            if(element.ownText().isEmpty())
                continue;
            String[] word = (element.ownText().split("\\s+"));//splits the string based on whitespace

            //String tag=element.tagName();
//            List<String>words=Arrays.asList(word) ;
//            boolean take=false;
//            if (!tagsnames.contains(tag)) {
//
//                if(tagsnames.contains(element.parent().tagName())) {
//                    tag=element.parent().tagName();
//                }
//                else continue;
//            }
//            Bson update2 = Updates.pushEach("_body",words);
//            database_Crawler.collection.updateMany(filter, update2);
            //each word in the par
            for (int i = 0; i < word.length; i++) {//all words
                //      body_String.append(word[i]+" ");
                String search_word=word[i].toLowerCase();
                search_word=search_word.trim();

                if (!ImportantWords.contains(search_word))
                {
                    search_word =search_word.replaceAll("[^a-zA-Z0-9]", " ");
                    String[] subwords = search_word.split("\\s+");//splits the string based on whitespace
                    for (int j = 0; j < subwords.length; j++) {
                        search_word=subwords[j];
//                        if (search_word == null || search_word.trim().isEmpty()) {
//                            continue;
//                        }
                        if (stopWords.contains(search_word)) {
                            // no_Of_Words++;
                            continue;
                        }

                        Remove_Words_Old_URL(search_word, url);
//                        no_Of_Words++;
                    }
//                    position++;
                } else {
                    Remove_Words_Old_URL(search_word, url);
                    // no_Of_Words++;
                    //position++;
                }
            }
        }
//        String body;
//        if(body_String.length()==0)
//            body=body_String.toString();
//        else
//            body=body_String.deleteCharAt(body_String.length()-1).toString();
//        Bson filter=eq("_id",url);
//        Document update =new Document("$pull",new Document("DOC",new Document("_url",url)));
//        database_Crawler.collection.updateMany(filter, update2);
    }


    private void Remove_Words_Old_URL(String word,String url )
    {
        //steaming the word
        String stemword = porterStemmer.stem(word);//hello
        Document fillter=new Document("_id",stemword);
        Document update =new Document("$pull",new Document("DOC",new Document("_url",url)));


        UpdateResult Up_result=database_Index.collection.updateOne(fillter,update);
        if(Up_result.getModifiedCount()==1) {
            Document fillter2 = new Document("_id", stemword);
            Bson update2 =Updates.inc("DF",-1);


           database_Index.collection.updateOne(fillter2, update2);
        }


    }
}
