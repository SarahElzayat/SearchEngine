package QueryProcessing;
//Database
import IndexerPackage.HtmlParsing;
import MongoDBPackage.MongoDB;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

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
        for (int i=0;i< words.length;i++)
        {
            Document DBresult = database.collection.find(new Document("_id",words[i])).first();

            if (DBresult != null)
            {
                JSONObject obj = new JSONObject(DBresult.toJson());
                JSONArray arr = obj.getJSONArray("DOC");
                  docarr[i]=arr;
              // System.out.println(docarr[i].getJSONObject(0).getString("url"));
            }
        }
        Vector<JSONObject> result=new Vector<JSONObject>(docarr[0].length());
        for (int i=0;i<docarr[0].length() ;i++)
        {
            String url=docarr[0].getJSONObject(i).getString("url");
            boolean comm=false;
            for (int j=1;j<docarr.length;j++)
            {
                comm=false;
                for(int k=0;k<docarr[j].length();k++)
                {
                 comm=false;
                    if((url.equals(docarr[j].getJSONObject(k).getString("url"))))
                    {
                        comm=true;
                        break;
                    }
                }
            }
            /////common for all words
            if(comm)
            {
               result.add(docarr[0].getJSONObject(i));
            }
        }
//        System.out.println(Arrays.toString(result));
    }

}
