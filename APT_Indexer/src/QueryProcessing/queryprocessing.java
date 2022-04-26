package QueryProcessing;
//Database
import IndexerPackage.HtmlParsing;
import MongoDBPackage.MongoDB;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.json.JSONObject;
import org.json.JSONArray;
import static com.mongodb.client.model.Filters.*;

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
        JSONArray []docarr=new JSONArray[words.length];
        for (int i=0;i< words.length;i++) {
            Document DBresult = database.collection.find(new Document("_id",words[i])).first();
            if (DBresult != null) {
                JSONObject obj = new JSONObject(DBresult.toJson());
                JSONArray arr = obj.getJSONArray("DOC");
                  docarr[i]=arr;
//                System.out.println(docarr[i].getJSONObject(0).getString("url"));
            }
        }
//        System.out.println(Arrays.toString(docarr));
    }

}
