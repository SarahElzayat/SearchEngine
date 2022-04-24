package QueryProcessing;
//Database
import IndexerPackage.HtmlParsing;
import MongoDBPackage.MongoDB;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class queryprocessing {
    private MongoDB database;
    queryprocessing()
    {
        //connect to DB
        database=new MongoDB("Search_Engine","Indexer");
    }
    public void queryprocess(String QP_str)
    {
        String[] words = (QP_str).toLowerCase().split("\\s");//splits the string based on whitespace
        for (String w : words)
        {
            Bson fillter=and(eq("_id",w));
           // Document doc= (Document) database.collection.find(fillter);
            MongoCursor<Document> cursor = database.collection.find(fillter).iterator();
            while (cursor.hasNext()) {
                System.out.println("collection is " +cursor.next() );}
        }


    }

}
