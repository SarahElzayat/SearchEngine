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
import java.util.*;
///////////////////////////////////////////
///////////////////////TODO///////////////
/////////////////////CHECK FOR HOST////////////
/////////////////////NEGLECT SIGNIN/SIGNUP...//////


public class Crawler extends Thread {
  DBManager dbMongo;
  RobotsManager robotsTxt;
  static HashSet<String> urlsBGD = new HashSet<>(5100);
  static HashSet<String> hashedHTMLS = new HashSet<>(5100);

  Object obj = new Object();

  static long URLSWithHTMLID;

  Crawler(DBManager db, RobotsManager rb) throws IOException {
    dbMongo = db;
    robotsTxt = rb;
  }

  public void run() {

    ObjectId currentID;
    System.out.println("AAAAAAA " + URLSWithHTMLID);
    while (URLSWithHTMLID < 5000) {
      Document returnedDoc = new Document();
      synchronized (obj) {
        returnedDoc = dbMongo.returnDocwithstate(0, new Document("_state", 1), 1);
      }
      currentID =
          (ObjectId)
              returnedDoc.get(
                  "_id");
      System.out.println("Current ID: @run" + currentID);
            if (urlsBGD.contains(returnedDoc.get("_url").toString())) {

        dbMongo.updatePopularity(new Document("popularity", 1), returnedDoc.get("_url").toString());
        System.out.println("@run URL exists");
        dbMongo.updateDoc(new Document("_state", 1), currentID);
        continue;
      }
      try {
        robotsTxt.getRobotsfile(returnedDoc.get("_url").toString());
        System.out.println("@Run got robots.txt file");
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
        if (!doc.toString().toLowerCase().contains("<!doctype html>")
            || !(doc.toString().toLowerCase().contains("lang=\"en\""))) {
          System.out.println("@Run doesn't contain <doc html>");
          dbMongo.updateDoc(new Document("_state", 1), currentID);
          continue;
        }
        String encryptedHTML = encryptThisString(doc.toString());
//        String encryptedHTML = encryptThisString(doc);
        if (!hashedHTMLS.contains(encryptedHTML)) {
          System.out.println("@Run hashed doesn't contain encryption");
          synchronized (obj) {
            hashedHTMLS.add(encryptedHTML);
            URLSWithHTMLID++;
            System.out.println("URLS HTML ID: " + URLSWithHTMLID);
          }

          if (URLSWithHTMLID < 6) {
            System.out.println("URLSWithHTMLID < 6");
            dbMongo.insertIntoDBHtmls(
                returnedDoc.get("_url").toString(), doc.toString());//, encryptedHTML);
          } else {
            System.out.println("URLSWithHTMLID >>>> 6");

            dbMongo.insertIntoDBHtmls(returnedDoc.get("_url").toString(), doc.toString());//, "");
          }
          System.out.println(
              "THREAD: "
                  + currentThread().getName()
                  + " URLS HTML ID: AFTER Insertion"
                  + URLSWithHTMLID);
          System.out.println("URL: " + returnedDoc.get("_url").toString());

          synchronized (obj) {
             urlsBGD.add(returnedDoc.get("_url").toString());
          }

          Elements el = doc.select("a[href]");
          int counter =0;
//          String hashedLinks="";
          for (Element lis : el) {
            if(counter >50)
              break;
            String firstLink = lis.attr("href");
            if (firstLink.startsWith("https://")&&!(firstLink.contains("register")
            ||firstLink.contains("signup")
            ||firstLink.contains("signin")
            ||firstLink.contains("help"))
            )
            {

              URL temp = new URL(returnedDoc.get("_url").toString());
//              hashedLinks+=temp.toString();
              if (!robotsTxt.checkifAllowed(firstLink, temp)) {
                continue;
              }

              dbMongo.insertInFetchedurls(firstLink, 0);
            }
            counter++;
            System.out.println("THREAD" + currentThread().getName()+ "COUNTER: "+counter);
          }
//          hashedHTMLS.add(hashedLinks);
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        } else {
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        }
      } catch (IOException e) {

//        e.printStackTrace();
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

    dbMongo.retrieveLinkWithState1();
  }

  public static String encryptThisString(String input) {
//  public static String encryptThisString(org.jsoup.nodes.Document input) {
    try {
      // getInstance() method is called with algorithm SHA-1
      MessageDigest md = MessageDigest.getInstance("SHA-1");

      // digest() method is called
      // to calculate message digest of the input string
      // returned as array of byte
      byte[] messageDigest = md.digest(input.getBytes());

      // Convert byte array into signum representation
      BigInteger no = new BigInteger(1, messageDigest);

      // Convert message digest into hex value
      String hashtext = no.toString(16);

      // Add preceding 0s to make it 32 bit
      while (hashtext.length() < 32) {
        hashtext = "0" + hashtext;
      }
      // return the HashText

      return hashtext;
    }

    // For specifying wrong message digest algorithms
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
//  int i=0;
//    Elements el = input.select("a[href]");
//    String s = "";
//    for (Element lis : el) {
//      s+= lis.attr("href");
//      i++;
//      if(i>20);
//      break;
//    }
//    return s;
  }

  public void recrawlSeeds() throws IOException {
    FindIterable<Document> documents = dbMongo.URLSWithHTML.find().limit(6);
    ObjectId tempID ;
    for (Document doc : documents) {
      String url = doc.get("_url").toString();
      tempID = (ObjectId) doc.get("_id");
      org.jsoup.nodes.Document temp;
      temp = Jsoup.connect(url).get();
      String encryptedHTML = encryptThisString(temp.toString());
//      String encryptedHTML = encryptThisString(temp);
      String tempEncrypted = encryptThisString(doc.get("html").toString());
//      String tempEncrypted = encryptThisString(doc.get("html").toString());
      if (!encryptedHTML.equals(tempEncrypted)) {
        dbMongo.updateSeed(temp.toString(), tempID);

        System.out.println("Updated document url " + url);
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
    numberOfThreads = sc.nextInt();
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
//    crawl.recrawlSeeds();
  }
}
