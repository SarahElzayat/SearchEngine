package com.noodle.search_engine;

import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
///////////////////////////////////////////
/////////////////////// TODO///////////////
///////////////////// CHECK FOR HOST////////////
///////////////////// NEGLECT SIGNIN/SIGNUP...//////

public class Crawler extends Thread {
  DBManager dbMongo;
  RobotsManager robotsTxt;
  static HashSet<String> urlsBGD = new HashSet<>(5100);
  static HashSet<String> hashedHTMLS = new HashSet<>(5100);

  Object obj = new Object();
  public static final String TEXT_BLUE = "\u001B[34m";
  public static final String TEXT_RESET = "\u001B[0m";
  static long URLSWithHTMLID;

  Crawler(DBManager db, RobotsManager rb) throws IOException {
    dbMongo = db;
    robotsTxt = rb;
  }

  public void run() {

    ObjectId currentID;
    //    System.out.println("AAAAAAA " + URLSWithHTMLID);
    while (URLSWithHTMLID < 5100) {
      Document returnedDoc = new Document();
      synchronized (obj) {
        returnedDoc = dbMongo.returnDocwithstate(0, new Document("_state", 1), 1);
        if (returnedDoc == null) {
          break;
        }
      }
      currentID = (ObjectId) returnedDoc.get("_id");
      //      System.out.println("Current ID: @run" + currentID);

      if (urlsBGD.contains(returnedDoc.get("_url").toString())) {

        dbMongo.updatePopularity(new Document("popularity", 1), returnedDoc.get("_url").toString());
        //        System.out.println("@run URL exists");
        dbMongo.updateDoc(new Document("_state", 1), currentID);
        continue;
      }
      try {
        robotsTxt.getRobotsfile(returnedDoc.get("_url").toString());
        //        System.out.println("@Run got robots.txt file");
      } catch (IOException e) {
        e.printStackTrace();
      }
      org.jsoup.nodes.Document doc = null;
      try {
        doc =
            Jsoup.connect(returnedDoc.get("_url").toString())
                .ignoreHttpErrors(true)
                .timeout(5000)
                .get(); // fetch the link html source
        Element taglang = doc.select("html").first();
        //        System.out.println("LANG " + taglang.attr("lang"));

        if (!doc.toString().toLowerCase().contains("<!doctype html>") || !doc.toString().toLowerCase().contains(" lang=\"en")) {

          //          System.out.println(returnedDoc.get("_url") + " doesn't contain <doc html>");
          dbMongo.updateDoc(new Document("_state", 1), currentID);
          continue;
        }

        String encryptedHTML = encryptThisString(doc);
        if (!hashedHTMLS.contains(encryptedHTML)) {
          //          System.out.println("@Run hashed doesn't contain encryption");
          synchronized (obj) {
            hashedHTMLS.add(encryptedHTML);
            URLSWithHTMLID++;
            //            System.out.println("URLS HTML ID: " + URLSWithHTMLID);
            System.out.println(
                TEXT_BLUE
                    + "FROM THREAD "
                    + currentThread()
                    + " "
                    + returnedDoc.get("_url").toString()
                    + TEXT_RESET);


            dbMongo.insertIntoDBHtmls(
                returnedDoc.get("_url").toString(), doc.toString(), encryptedHTML);
          }
          //          System.out.println(
          //              "THREAD: "
          //                  + currentThread().getName()
          //                  + " URLS HTML ID: AFTER Insertion"
          //                  + URLSWithHTMLID);
          //          System.out.println("URL: " + returnedDoc.get("_url").toString());

          synchronized (obj) {
            urlsBGD.add(returnedDoc.get("_url").toString());
          }

          Elements el = doc.select("a[href]");
          int counter = 0;
          //          String hashedLinks="";
          for (Element lis : el) {
            if (counter > 50) break;
            String firstLink = lis.attr("href");
            if (firstLink.startsWith("https://")
                && !(firstLink.contains("register")
                    || firstLink.contains("signup")
                    || firstLink.contains("signin")
                    || firstLink.contains("help")
                    || firstLink.contains("twitter")
                    || firstLink.contains("login"))) {

              URL temp = new URL(returnedDoc.get("_url").toString());
              //              hashedLinks+=temp.toString();
              if (!robotsTxt.checkifAllowed(firstLink, temp)) {
                continue;
              }

              dbMongo.insertInFetchedurls(firstLink, 0);
            }
            counter++;
            //            System.out.println("THREAD" + currentThread().getName() + "COUNTER: " +
            // counter);
          }
          //          hashedHTMLS.add(hashedLinks);
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        } else {
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        }
      } catch (IOException e) {

        //        e.printStackTrace();
        dbMongo.updateDoc(new Document("_state", 1), currentID);

        continue;
      }
    }
  }

  public void initializeSeeds() throws IOException {

    File myObj = new File("./seed.txt");
    Scanner myReader = new Scanner(myObj);
    URLSWithHTMLID = dbMongo.getHTMLURLsCount();
    //    fetchedURLSID = dbMongo.getFetchedCount();
    dbMongo.retrieveSeeds(urlsBGD);
    //    dbMongo.retrieveSeeds(finalVersionBgdGamedAwy);
    while (myReader.hasNextLine()) {
      String title = myReader.nextLine();
      if (urlsBGD.contains(title)) continue;
      // fetch the link here
      dbMongo.insertInFetchedurls(title, 0);
    }
    dbMongo.retrieveElements(urlsBGD);
    dbMongo.retrieveTitles(hashedHTMLS);
    dbMongo.retrieveLinkWithState1();
  }

  public static String encryptThisString(org.jsoup.nodes.Document input) {

    if (input.title().equals("")) return input.attr("title");
    return input.title();
  }

  public void recrawlSeeds() throws IOException {
    dbMongo.OldSeeds.drop();
    FindIterable<Document> documents = dbMongo.URLSWithHTML.find().limit(6);
    int tempID = 0;
    for (Document doc : documents) {
      String url = doc.get("_id").toString();
      org.jsoup.nodes.Document temp;
      temp = Jsoup.connect(url).get();
      int hashedNew = temp.hashCode();
      String html = doc.get("html").toString();
      int hashedOld = html.hashCode();
      if (hashedOld != hashedNew) {

        dbMongo.updateSeed(temp.toString(), url, html,temp.title());

        //        System.out.println("Updated document url " + url);
      }
    }
  }

  public static void main(String[] args)
      throws IOException, URISyntaxException, InterruptedException {
    DBManager db = new DBManager();
    Crawler crawl = new Crawler(db, new RobotsManager());
     crawl.initializeSeeds();
     System.out.println("ENTER NUMBER OF THREADS");
     // crawl.run();
     Scanner sc = new Scanner(System.in);
     int numberOfThreads;
     int sleepSecs = 0;
     numberOfThreads = sc.nextInt();
 //    if (numberOfThreads > 6) sleepSecs = 3000;

     Crawler threads[] = new Crawler[numberOfThreads];
     for (int i = 0; i < numberOfThreads; i++) {
       threads[i] = new Crawler(new DBManager(), new RobotsManager());
       threads[i].setName(Integer.toString(i));
       threads[i].start();
       threads[i].sleep(3000);
     }
     try {
       for (int i = 0; i < numberOfThreads; i++) {
         threads[i].join();
       }
     } catch (InterruptedException e) {
       e.printStackTrace();
     }
     java.awt.Toolkit.getDefaultToolkit().beep();

//   crawl.recrawlSeeds();
  }
}
