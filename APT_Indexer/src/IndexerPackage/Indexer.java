package IndexerPackage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

//Elements
import com.mongodb.client.model.Updates;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//Database
import MongoDBPackage.MongoDB;


//Stemmer
import opennlp.tools.stemmer.PorterStemmer;

//get from DB
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Projections;

import com.mongodb.client.model.Sorts;//sort
import static com.mongodb.client.model.Filters.eq;//eq
import com.mongodb.client.model.Updates;//update





public class Indexer {
    private String[] tags={"h1","h2","h3","h4","h5","h6","p","abbr","article","div","header","label","q","title","thead","th","textarea","sub","caption","td","li","option","dt"};
    private static HashSet<String> stopWords ;


    private PorterStemmer porterStemmer = new PorterStemmer();//stemmer

    private MongoDB database;


    Indexer()
    {
        //load stopwords
        stopWords=new HashSet<String>();
        getStopWords();

        //connect to DB
        database=new MongoDB("Search_Engine","Indexer");
    }

    public void Index()
    {
        for (String tag : tags) {
            Elements paragraphs = HtmlParsing.Parse_Tags(tag);//all tags
            //each eleemtn
            for (Element p : paragraphs) {
                String[] words = (p.text()).toLowerCase().split("\\s");//splits the string based on whitespace

            //each word in the par
            for (String w : words) {//all words
                if (stopWords.contains(w))
                {
                    continue;
                }
                else //add it
                {
                    //addWordtoDB(w);
                }

            }
            }

        }
        addWordtoDB("Hello","url2");


    }

    private void addWordtoDB(String word,String url)
    {
        //steaming the word
        word = porterStemmer.stem(word);//hello
        //1.get hello DF Document
        Bson projectionFields = Projections.fields(Projections.include(), Projections.excludeId());
        Document doc = database.collection.find(eq("word", word)).projection(projectionFields).sort(Sorts.descending("imdb.rating")).first();
        //doc==null
        //word exist before doc ! null//DF DOC1    DOC2     ..
        //1.get DF
        Long DF = (Long) doc.get("DF");
        //2.DOC DF//DOC1
        String DOCString = "DOC" + Long.toString(DF); //DOC2
        ArrayList<?> DOC = (ArrayList<?>) doc.get(DOCString);

        if (!DOC.get(0).equals(url))//new DOC for existing word
        {
            DF++;
            DOCString="DOC"+Long.toString(DF);//DOC2
           // database.collection.updateOne(eq("word", word), Updates.push(DOCString, url));
        }
      //  String[]DOCEE=



    }


    private static void getStopWords() {
        try {
            File websitesFile = new File("D:\\APT_Indexer\\src\\stopwords.txt");
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
}
