package IndexerPackage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

//Elements
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//Database
import MongoDBPackage.MongoDB;


//Stemmer
//import opennlp.tools.stemmer.PorterStemmer;

//get from DB
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Projections;

import com.mongodb.client.model.Sorts;//sort

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;//eq
import com.mongodb.client.model.Updates;//update





public class Indexer {
    //private String[] tags={"h1","h2","h3","h4","h5","h6","p","abbr","article","div","header","label","q","title","thead","th","textarea","sub","caption","td","li","option","dt"};
private String[] tags={"title","h1","h2","h3","h4","h5","h6","p"};

    private static HashSet<String> stopWords ;
    private static HashSet<String> ImportantWords;

    //private PorterStemmer porterStemmer = new PorterStemmer();//stemmer

    private MongoDB database;


    Indexer()
    {
        //load stopwords
        stopWords=new HashSet<String>();
        ImportantWords=new HashSet<String>();
        getStopWords();
        getImportantword();
        //connect to DB
        database=new MongoDB("Search_Engine","Indexer");
    }

    public void Index(String url)
    {
        for (String tag : tags) {
            Elements paragraphs = HtmlParsing.Parse_Tags(tag);//all tags
            //each eleemtn
            for (Element p : paragraphs) {
                String[] words = (p.text()).toLowerCase().split("\\s");//splits the string based on whitespace

                //each word in the par
                for (String w : words) {//all words

                    if (!ImportantWords.contains(w))
                    {
                        w = w.replaceAll("[^a-zA-Z0-9]", " ");
                        String[] subwords = w.split("\\s");//splits the string based on whitespace
                        for (String sw : subwords)
                        {
                            if (stopWords.contains(sw))
                            {
                                continue;
                            }
                            addWordtoDB(sw, url, tag);
                        }
                    }
                    else
                    {
                        addWordtoDB(w, url, tag);
                    }
                }
            }

        }
//        addWordtoDB("zeinab","WWW.Google.COM","p");
        MongoCursor<Document> cursor = database.collection.find().iterator();
        while (cursor.hasNext()) {
            System.out.println("collection is " +cursor.next() );}


    }

    private void addWordtoDB(String word,String url,String tag)
    {
        //steaming the word
        //word = porterStemmer.stem(word);//hello
        Bson fillter=and(eq("_id",word),eq("DOC.url",url));
        Bson update=Updates.inc("DOC.$."+tag,1);
        UpdateResult Up_result= database.collection.updateMany(fillter,update);
        if(Up_result.getMatchedCount()==0)//new url or word
        {
            // new_url
            Document doc=new Document("url",url).append(tag,1);
            fillter=eq("_id",word);
            update=Updates.combine(Updates.push("DOC",doc),Updates.inc("DF",1));
            Up_result= database.collection.updateMany(fillter,update);

            if(Up_result.getMatchedCount()==0)//new  word
            {

                Document arrdoc=new Document("url",url).append(tag,1);
                doc=new Document("_id",word).append("DF",1).append("DOC", Arrays.asList(arrdoc));
                database.collection.insertOne(doc);
            }
        }
    }

    private static void getStopWords() {
        try {
            File websitesFile = new File("D:\\2nd-term\\OS\\Project\\SearchEngine\\APT_Indexer\\src\\stopwords.txt");
            Scanner reader = new Scanner(websitesFile);
            while (reader.hasNextLine()) {
                String word = reader.nextLine();
                stopWords.add(word);
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("An exception occured while reading the stop words!");
            e.printStackTrace();
        }
    }
    private static void getImportantword() {
        try {
            File websitesFile = new File("D:\\2nd-term\\OS\\Project\\SearchEngine\\APT_Indexer\\src\\MongoDBPackage\\important.txt");
            Scanner reader = new Scanner(websitesFile);
            while (reader.hasNextLine()) {
                String word = reader.nextLine();
                ImportantWords.add(word);
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("An exception occured while reading the stop words!");
            e.printStackTrace();
        }
    }
}
