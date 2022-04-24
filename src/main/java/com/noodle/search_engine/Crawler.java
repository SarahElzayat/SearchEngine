
package com.noodle.search_engine;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler {
    DBManager dbMongo;
    RobotsManager robotsTxt;
    HashSet<String> urlsBGD = new HashSet<>();

    long URLSWithHTMLID;
    long fetchedURLSID;
    String title;
    // Vector<String> disallows;
    // int crawledPages;
    int currentID;

    Crawler(DBManager db, RobotsManager rb) throws IOException {
        dbMongo = db;
        robotsTxt = rb;
    }

    public void initializeSeeds() throws IOException, URISyntaxException {
        File myObj =
                new File("./seed.txt");
        Scanner myReader = new Scanner(myObj);
        URLSWithHTMLID = dbMongo.gethtmlurlsCount();
        fetchedURLSID = dbMongo.getfetchedcount();
        dbMongo.retrieveElements(urlsBGD);
        while (myReader.hasNextLine()) {
            title = myReader.nextLine();
            if (urlsBGD.contains(title)) continue;
            // fetch the link here
            this.crawl(title, 0);
        }
        //  System.out.println("done initialize seed");
    }

    public void fetchlinksfromDB() throws IOException, URISyntaxException {
        // System.out.println("fetching from db");

        dbMongo.retrieveLinkwithstate1();
        // System.out.println("returned from state 1");

        while (URLSWithHTMLID < 5000) {
            Document returnedDoc = new Document();
            returnedDoc=dbMongo.returnDocwithstate(0, new Document("_state", 1),1);
            // System.out.println(returnedDoc.toString());
            currentID = Integer.parseInt(returnedDoc.get("_id").toString());
            // System.out.println(currentID);
            title = returnedDoc.get("_url").toString();
            //System.out.println(title);
            if (urlsBGD.contains(title)) {
                //System.out.println("helloooooo");
                dbMongo.updateDoc(new Document("_state", 1),currentID);
                //System.out.println(returnedDoc);
                continue;
            }
            this.crawl(title, 1);
        }
    }

    public void crawl(String url, int check) throws IOException, URISyntaxException {
        robotsTxt.getRobotsfile(url);
        org.jsoup.nodes.Document doc;
        doc = Jsoup.connect(url).get(); // fetch the link html source
        if (!doc.toString().contains("<!doctype html>")) {
            if (check == 1) {
                dbMongo.updateDoc(new Document("_state", 1),currentID);
            }
            return;
        }
        dbMongo.insertIntodbHtmls(URLSWithHTMLID++, url, doc.toString());
        urlsBGD.add(url);
        Elements el = doc.select("a[href]");
        for (Element lis : el) {
            this.addinFetched(lis, check,url);
        }
        if (check == 1) dbMongo.updateDoc(new Document("_state", 1),currentID);
    }
    //fetched from currently used url
    public void addinFetched(Element url, int check,String url1) throws MalformedURLException, URISyntaxException {
        String firstLink = url.attr("href");
        if (firstLink.contains("https")) {
            //   System.out.println(firstLink);
            // URI temp1 = new URI(firstLink);
            URL temp=new URL(url1);
            if (!robotsTxt.checkifAllowed(firstLink,temp)) {
                if (check == 1) {
                    dbMongo.updateDoc(new Document("_state", 1),currentID);
                }
                return;
            }

            dbMongo.insertinFetchedurls(fetchedURLSID++, firstLink, 0);
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        DBManager db = new DBManager();
        Crawler crawl = new Crawler(db, new RobotsManager());
        crawl.initializeSeeds();
        crawl.fetchlinksfromDB();
        ///////////////////////// NORMALIZATION////////////////////////////////////////

        // String uri = "mongodb://localhost:27017"; ////////////////////////////////
        //  HashSet<String> urls = new HashSet<>();
        // HashSet<String> urlsBGD = new HashSet<>();
        // File myObj =
        // new File(
        //   "D:\\CMP #2\\Second Semester\\Advanced Programming
        // Techniques\\IntelliJ\\SearchEngine\\seed.txt");
        // MongoClient mongo = MongoClients.create(uri);//////////////////////////////////
        // MongoDatabase db =
        // mongo.getDatabase("SearchEngine");//////////////////////////////////////////
        // MongoCollection<Document> fetchedURLS =
        // db.getCollection("FetchedURLs");////////////////////////////////////
        // MongoCollection<Document> URLSWithHTML =
        // db.getCollection("URLSWithHTML");//////////////////////////////////
        // long URLSWithHTMLID = dbMongo.gethtmlurlsCount();//URLSWithHTML.countDocuments();
        //////////////////////// hi

    /*for (int i = 0; i < URLSWithHTML.countDocuments(); i++) {
      FindIterable<Document> ddd = URLSWithHTML.find();
      for (Document ff : ddd) {
        urlsBGD.add((String) ff.get("_url"));
      }
    }
    System.out.println(urlsBGD);*/
        // long fetchedURLSID = dbMongo.getfetchedcount();//fetchedURLS.countDocuments();
        // Scanner myReader = new Scanner(myObj);

        // for (int i = 0; i < 6; i++) // read from seeds.txt
        // {

        // String title;
        // title = myReader.nextLine();
        // if (urlsBGD.contains(title)) continue;
    /*
          URL currentURL = new URL(title);
          String robotsURL =
              (new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt"))
                  .toString();
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
    */

    /*org.jsoup.nodes.Document doc;
    try {
      doc = Jsoup.connect(title).get();
    } catch (Exception e) {
      continue;
    }*/
        // if (!doc.toString().contains("<!doctype html>")) continue;

    /* Document s =
        new Document("_id", URLSWithHTMLID++)
            .append("_url", title)
            .append("html", doc.toString());
    // state = n --> not downloaded yet
    URLSWithHTML.insertOne(s);*/
        /*urlsBGD.add(title);*/

        // Elements el = doc.select("a[href]");

    /*for (Element lis : el) {
        Boolean tmam = true;
        //        if (!urls.contains(lis.attr("href"))) {
        for (int y = 0; y < disallows.size(); y++) {
          if (lis.attr("href").contains(disallows.get(y))
              && lis.attr("href").contains(currentURL.getHost())) {
            tmam = false;
          }
        }
        if (!tmam) continue;
        //          urls.add(lis.attr("href"));
        if (lis.attr("href").contains("https")) {
          Document link =
              new Document("_id", fetchedURLSID++)
                  .append("_url", lis.attr("href"))
                  .append("_state", 0); // 0 added, 1 being processed, 2 done
          fetchedURLS.insertOne(link);
        }
        //        }
      }
    }*/

        // ####################################################//
    /*  while(true){
        Document find0 = new Document("_state", 1);
        Document increase = new Document("$inc", new Document("_state", -1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
    //    options.upsert(true);
        options.returnDocument(ReturnDocument.AFTER);
        Document coco=(Document) fetchedURLS.findOneAndUpdate(find0,increase,options);
        if(coco==null)
          break;
        }*/
        // for (int i = 0; i < 100; i++) // read from seeds.txt
        // {
    /*
    Document find0 = new Document("_state", 0);
    Document increase = new Document("$inc", new Document("_state", 1));
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
    options.upsert(true);
    options.returnDocument(ReturnDocument.AFTER);
    Document coco = (Document) fetchedURLS.findOneAndUpdate(find0, increase, options);
    int currentID = Integer.parseInt(coco.get("_id").toString());
    Document find1 = new Document("_id", currentID);
    String title;
    title = coco.get("_url").toString();
    System.out.println("URL coco " + title);
    // return;
    if (urlsBGD.contains(title)) {
      fetchedURLS.findOneAndUpdate(find1, increase, options);
      System.out.println("COCO ALREADY EXISTS");
      continue;
    }
    URL currentURL = new URL(title);
    String robotsURL =
        (new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt"))
            .toString();
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
    }*/

    /*  org.jsoup.nodes.Document doc;
    try {
      doc = Jsoup.connect(title).get();
    } catch (Exception e) {
      continue;
    }
    if (!doc.toString().contains("<!doctype html>")) {
      fetchedURLS.findOneAndUpdate(find1, increase, options);
      System.out.println("COCO ISN'T HTML");
      continue;
    }
    Document s =
        new Document("_id", URLSWithHTMLID++)
            .append("_url", title)
            .append("html", doc.toString());
    // state = n --> not downloaded yet
    URLSWithHTML.insertOne(s);
    urlsBGD.add(title);*/

        // Elements el = doc.select("a[href]");

        // for (Element lis : el) {
    /* Boolean tmam = true;
    for (int y = 0; y < disallows.size(); y++) {
      if (lis.attr("href").contains(disallows.get(y))
          && lis.attr("href").contains(currentURL.getHost())) {
        tmam = false;
      }
    }
    if (!tmam) {
      fetchedURLS.findOneAndUpdate(find1, increase, options);
      System.out.println("NOT TMAM");
      continue;
    }*/

    /*if (lis.attr("href").contains("https")) {
        Document link =
            new Document("_id", fetchedURLSID++)
                .append("_url", lis.attr("href"))
                .append("_state", 0); // 0 added, 1 being processed, 2 done
        fetchedURLS.insertOne(link);
      }
    }*/
    /*    fetchedURLS.findOneAndUpdate(find1, increase, options);
      System.out.println("Finished coco");
    }*/
    }
}