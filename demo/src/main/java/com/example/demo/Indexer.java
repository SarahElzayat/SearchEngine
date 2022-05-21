//package IndexerPackage;
package com.example.demo;
import java.io.File;
import java.io.IOException;
import java.util.*;

//Elements
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//Database
import org.bson.Document;

//Stemmer
import opennlp.tools.stemmer.PorterStemmer;

//get from DB
import org.bson.conversions.Bson;
import java.lang.String;
import static com.mongodb.client.model.Filters.*;


public class Indexer {

    //============================Data Members===========================//
    private static HashSet<String> stopWords ;
    private static HashSet<String> ImportantWords;

    private PorterStemmer porterStemmer;//stemmer


    //MongoDB Class Objects
    private MongoDB Search_Engine_Database;

    //Collections
    public MongoCollection<Document> database_Indexer;
    public MongoCollection<Document> database_Crawler;
    public MongoCollection<Document> database_Old_Crawler;

    public HashSet<String>tagsnames;

    /*
    Q4.Mention roughly how much time does the Indexer take to run over the output of the Crawler. [Indexer]
    35-50 mins
    Q5.Mention if it works without runtime errors. (Yes or No only) [Indexer]
    Yes
    */
    //======================================Member Functions============================//
    //1.Constructor
    Indexer()
    {
        porterStemmer = new PorterStemmer();
        //load stop words
        stopWords= getStopWords();
        //load important words
        ImportantWords= getImportantword();;

        //Tags_to_Search_for
        tagsnames=new HashSet<String>();
        tagsnames.add("h1");
        tagsnames.add("h2");
        tagsnames.add("h3");
        tagsnames.add("h4");
        tagsnames.add("h5");
        tagsnames.add("h6");
        tagsnames.add("p");

        //connect to Search Engine DB
        Search_Engine_Database=new MongoDB("SearchEngine");

        //Collections
        database_Crawler=Search_Engine_Database.GetCollection("URLSWithHTML");
        database_Indexer=Search_Engine_Database.GetCollection("Indexer");
    }
//Q1.Mention whether you calculated the TF_IDF and show the file(s) where you did this. (Don't explain the code, just mention where is it) [Indexer]
    //part1:TF:No of this word in this Website / No of all words in the website
    //No,we don't count the No of occurrence of  word in the url ==> We store all its locations
    //So the size of this array is then the Count (in addWordtoDB())
    //we Count No of all words in the website and store it in the Crawler DB (in Index())


    //part2:IDF:No of all websites/No of websites of this word
    //No,No of all websites:is by default 5000
    //No of websites of this word:we store the DF of the steam of this word

    //2.Index Url
    public void Index(String url,String source_str) throws IOException {
        StringBuffer body_String=new StringBuffer("");//to store body of this url
        int no_Of_Words=0;//count of words in this url
        int position=0;//position for words to add in the body

        //Parse Source Source file
        org.jsoup.nodes.Document doc= Jsoup.parse(source_str,"UTF-8");
        Elements bodyElements=doc.body().select("*");//select all ("*") tags in the body

        //======================================Main Loop====================================//
        //loop over all elements in the body==>element
        for(Element element:bodyElements)
        {
            if(element.ownText().isEmpty())
                continue;
            //element==> String[]
            String[] word = (element.ownText().split("\\s+"));//splits the string based on whitespace

            //tag name of the element
            String tag=element.tagName();
            boolean take=false;
            //check if this tag isn't from our desired tags
            if (!tagsnames.contains(tag)) {
                //another chance for the first parent tag  <h1><a>hello</a><h1>
                if(tagsnames.contains(element.parent().tagName()))
                {
                    tag=element.parent().tagName();
                }
                else continue;//not desired tag
            }
            //loop over each word in this String[] ==>has tag (valid one)
            for (int i = 0; i < word.length; i++) {

                body_String.append(word[i]+" ");//filling body
                String search_word=word[i].toLowerCase();//to lower
                search_word=search_word.trim();


                //Check if this word is an important word
                if (!ImportantWords.contains(search_word))
                {
                    //preprocessing
                    search_word =search_word.replaceAll("[^a-zA-Z0-9]", " ");
                    String[] subwords = search_word.split("\\s+");//splits the string based on whitespace
                    //further, sub words
                    for (int j = 0; j < subwords.length; j++) {
                        search_word=subwords[j];
                        if (stopWords.contains(search_word)) {
                            //don't add to the indexer
                            no_Of_Words++;
                            continue;
                        }
                        addWordtoDB(search_word, url, tag, position);
                        no_Of_Words++;
                    }//end of loop Sub words
                    position++;
                } else {
                    //No preprocessing
                    addWordtoDB(search_word, url, tag, position);
                    no_Of_Words++;
                    position++;
                }
            }//end of loop of words in each element
        }//end of Main loop


        //=====================================Putting body===============================================//
        String body;
        if(body_String.length()==0)
            body=body_String.toString();
        else
            body=body_String.deleteCharAt(body_String.length()-1).toString();//remove last space

        //put  this Body in the Crawler collection
        Bson filter=eq("_id",url);
        Bson update2=Updates.combine(Updates.set("NoOfWords",no_Of_Words),Updates.set("_body",body));
        database_Crawler.updateMany(filter, update2);
    }

    //Q3. Mention what do you save for each keyword while indexing. (for example: saving that the keyword in which type of HTML tags) [Indexer]
    //Open DB Indexer

    //3.Add a word to the Indexer Collection
    private void addWordtoDB(String word,String url,String tag,int position)
    {
        //steaming the word
        String stemword = porterStemmer.stem(word);

        Bson fillter=and(eq("_id",stemword),eq("DOC._url",url));
        Bson update=Updates.addToSet("DOC.$."+word+"."+tag,position);
        UpdateResult Up_result= database_Indexer.updateMany(fillter,update);
        if(Up_result.getMatchedCount()==0)//new url or word
        {
            // new_url
            Document tagdoc=new Document(tag,Arrays.asList(position));//"p":[1,7,9]
            Document doc=new Document("_url",url).append(word,tagdoc);

            fillter=eq("_id",stemword);
            update=Updates.combine(Updates.push("DOC",doc),Updates.inc("DF",1));
            Up_result= database_Indexer.updateMany(fillter,update);

            if(Up_result.getMatchedCount()==0)//new word
            {
                tagdoc=new Document(tag,Arrays.asList(position));
                Document arrdoc=new Document("_url",url).append(word,tagdoc);
                doc=new Document("_id",stemword).append("DF",1).append("DOC", Arrays.asList(arrdoc));
                database_Indexer.insertOne(doc);
            }
        }
    }
    //Q2.Mention where the indexer takes the input from the crawler and in which format did you save the output of the crawler.
    //part1:take all urls and their source files from the crawler and loop over them Index_crawlar()
    //part2:the output of each url is saved as string and passed to (Index())
    //4.Index 5000 Webpages
    public void Index_crawlar() throws IOException
    {
        int i=0;
        FindIterable<Document> itratot_Doc=database_Crawler.find();
        for (Document d:itratot_Doc)
        {
            Index(d.get("_id").toString(),d.get("html").toString());
            i++;
            System.out.println("Indexed: "+i+" webpages:)\n\n");
        }
    }


    //5.Reindex 6 webpages
    public void ReIndex_Crawlar_New_URLS() throws IOException {
        //Old page Source Pages
        database_Old_Crawler=Search_Engine_Database.GetCollection("OldSeeds");

        //Remove previous version
        FindIterable<Document> itratdoc_to_remove=database_Old_Crawler.find();
        Vector<String>old_URLs=new Vector<>();
        int j=0;
        for (Document d:itratdoc_to_remove)
        {
            String url=d.get("_id").toString();
            ReIndex_URL(url,d.get("html").toString());
            old_URLs.add(url);
            j++;
            System.out.println("Reindexed:"+j+"webpages:)\n\n");
        }
        //drop this database
        database_Old_Crawler.drop();
        //newly index
        int i=0;
        FindIterable<Document> itratdoc=database_Crawler.find().limit(6);
        for (Document d:itratdoc)
        {
            Index(d.get("_id").toString(),d.get("html").toString());
            i++;
            System.out.println("indexed"+i+" webpages\n\n");
        }
    }

    //6.Reindex a new url
    public void ReIndex_URL(String url,String source_str) throws IOException {

        org.jsoup.nodes.Document doc= Jsoup.parse(source_str,"UTF-8");
        Elements bodyElements=doc.body().select("*");//select al tags in the body

        //loop over all elements
        for(Element element:bodyElements)
        {
            if(element.ownText().isEmpty())
                continue;
            String[] word = (element.ownText().split("\\s+"));//splits the string based on whitespace

            for (int i = 0; i < word.length; i++) {//all words
                String search_word=word[i].toLowerCase();
                search_word=search_word.trim();

                if (!ImportantWords.contains(search_word))
                {
                    search_word =search_word.replaceAll("[^a-zA-Z0-9]", " ");
                    String[] subwords = search_word.split("\\s+");//splits the string based on whitespace
                    for (int j = 0; j < subwords.length; j++) {
                        search_word=subwords[j];
                        if (stopWords.contains(search_word)) {
                            continue;
                        }
                        Remove_Words_Old_URL(search_word, url);
                    }
                } else {
                    Remove_Words_Old_URL(search_word, url);
                }
            }
        }
    }


    //6.Remove a word from the Old Page source
    private void Remove_Words_Old_URL(String word,String url )
    {
        //steaming the word
        String stemword = porterStemmer.stem(word);//hello

        Document fillter=new Document("_id",stemword);
        //remove this url from the DOC[] of this word
        Document update =new Document("$pull",new Document("DOC",new Document("_url",url)));

        UpdateResult Up_result=database_Indexer.updateOne(fillter,update);
        if(Up_result.getModifiedCount()==1) {
            //dec the DF
            Document fillter2 = new Document("_id", stemword);
            Bson update2 =Updates.inc("DF",-1);

           database_Indexer.updateOne(fillter2, update2);
        }
    }


   //7.Getting Stop words
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
            System.out.println("An exception occurred while reading the stop words!");
            e.printStackTrace();
        }
        return stopw;
    }


    //8.Getting Important words
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


    //====================================MAIN=======================================//
    public static void main(String[] args) throws IOException {
        Indexer indexer=new Indexer();

        //Indexing 5000
        indexer.Index_crawlar();

        //reindexing 6 webpages
//        indexer.ReIndex_Crawlar_New_URLS();
        return;
    }

}
