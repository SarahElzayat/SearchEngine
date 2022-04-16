import java.io.*;
import java.util.*;
import java.net.URL;

import com.mongodb.BasicDBObject;
import groovy.lang.GString;
import org.bson.Document;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.jsoup.Jsoup;
import com.mongodb.client.model.Projections;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;

public class HelloWorld {

  public static void main(String[] args) throws IOException {

    String uri = "mongodb://localhost:27017";
    HashSet<String> urls = new HashSet<>();
    File myObj =
        new File(
            "D:\\CMP #2\\Second Semester\\Advanced Programming Techniques\\IntelliJ\\SearchEngine\\seed.txt");
    MongoClient mongo = MongoClients.create(uri);
    MongoDatabase db = mongo.getDatabase("SearchEngine");
    MongoCollection<Document> fetchedURLS = db.getCollection("FetchedURLs");
    MongoCollection<Document> URLSWithHTML = db.getCollection("URLSWithHTML");

    //loop collection --> fill hashset
    for (int i=0; i<fetchedURLS.countDocuments(); i++){
      BasicDBObject dbObject = new BasicDBObject();
      dbObject.put("_id",i);
      FindIterable<Document> d =  fetchedURLS.find(dbObject);
     for(Document doc : d)
     {
       urls.add((String) doc.get("_url"));
     }
    }
    //for (int i=0; i<URLSWithHTML.countDocuments(); i++){
      System.out.println(urls);
   // }

    Scanner myReader = new Scanner(myObj);
    int id=0;
    int fetchedURLID=0;
    for (int i = 0; i < 5; i++) // read from seeds.txt
    {

      String title;
      title = myReader.nextLine();
      URL currentURL = new URL(title);

      String robotsURL =
          (new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt")).toString();
      InputStream in = new URL(robotsURL).openStream();
      Scanner robots7aga = new Scanner(in).useDelimiter("\\A");
      String result = robots7aga.hasNext() ? robots7aga.next() : "";
      String[] array = result.split("\n"); // array of robots.txt as strings
      Vector<String> disallows = new Vector<String>(0);

      for (int x = 0; x < array.length; x++) {
        if (array[x].contains("User-agent: *")) {
          x++;

          while (x < array.length && array[x].contains("Disallow")) {
            array[x] = array[x].replace("Disallow: ", "");
            disallows.add(array[x]);

            x++;
          }
        }
      }

      org.jsoup.nodes.Document doc;
      try {

        doc = Jsoup.connect(title).get();
      } catch (Exception e) {
        continue;
      }

      Document s = new Document("_id", id++).append("_url",title).append("html", doc.toString());
      // state = n --> not downloaded yet
      URLSWithHTML.insertOne(s);

      Elements el = doc.select("a[href]");

      for (Element lis : el) {
        Boolean tmam = true;
        if (!urls.contains(lis.attr("href"))) {
          for (int y = 0; y < disallows.size(); y++) {
            if (lis.attr("href").contains(disallows.get(y))) {
              tmam = false;
            }
          }
          if (!tmam) continue;

          urls.add(lis.attr("href"));

          if (lis.attr("href").contains("https")) {

            Document link = new Document("_id", fetchedURLID++).append("_url",lis.attr("href"));
            fetchedURLS.insertOne(link);


          }
        }
      }
    }
  }
}
