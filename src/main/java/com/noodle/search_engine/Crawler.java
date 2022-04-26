package com.noodle.search_engine;

import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Scanner;

public class Crawler extends Thread {
  DBManager dbMongo;
  RobotsManager robotsTxt;
  HashSet<String> urlsBGD = new HashSet<>(5000);
  HashSet<String> hashedHTMLS = new HashSet<>(5000);
  Object obj = new Object();

  static long URLSWithHTMLID;
  static long fetchedURLSID;

  Crawler(DBManager db, RobotsManager rb) throws IOException {
    dbMongo = db;
    robotsTxt = rb;
  }

  public void run() {

    int currentID;
    System.out.println("AAAAAAA " + URLSWithHTMLID);
    while (URLSWithHTMLID < 5000) {
      Document returnedDoc = new Document();
      returnedDoc = dbMongo.returnDocwithstate(0, new Document("_state", 1), 1);
      currentID = Integer.parseInt(returnedDoc.get("_id").toString());
      System.out.println("Current ID: @run" + currentID);
      if (urlsBGD.contains(returnedDoc.get("_url").toString())) {
        System.out.println("@run URL exists");
        dbMongo.updateDoc(
            new Document("_state", 1), Integer.parseInt(returnedDoc.get("_id").toString()));
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
        doc = Jsoup.connect(returnedDoc.get("_url").toString()).get(); // fetch the link html source
        if (!doc.toString().toLowerCase().contains("<!doctype html>")) {
          System.out.println("@Run doesn't contain <doc html>");
          dbMongo.updateDoc(new Document("_state", 1), currentID);
          continue;
        }
        String encryptedHTML = encryptThisString(doc.toString());
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
                URLSWithHTMLID, returnedDoc.get("_url").toString(), doc.toString(), encryptedHTML);
          } else {
            System.out.println("URLSWithHTMLID >>>> 6");

            dbMongo.insertIntoDBHtmls(
                URLSWithHTMLID, returnedDoc.get("_url").toString(), doc.toString(), "");
          }
          System.out.println("URLS HTML ID: AFTER Insertion" + URLSWithHTMLID);
          System.out.println("URL: " + returnedDoc.get("_url").toString());

          synchronized (obj) {
            //            URLSWithHTMLID++;
            urlsBGD.add(returnedDoc.get("_url").toString());
          }

          Elements el = doc.select("a[href]");
          for (Element lis : el) {
            this.addinFetched(lis, returnedDoc.get("_url").toString(), currentID);
          }
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        } else {
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void initializeSeeds() throws IOException {

    File myObj = new File("./seed.txt");
    Scanner myReader = new Scanner(myObj);
    URLSWithHTMLID = dbMongo.getHTMLURLsCount();
    fetchedURLSID = dbMongo.getFetchedCount();
    dbMongo.retrieveSeeds(urlsBGD);
    while (myReader.hasNextLine()) {
      String title = myReader.nextLine();
      if (urlsBGD.contains(title)) continue;
      // fetch the link here
      System.out.println("URL added to db @initSeeds: " + fetchedURLSID + " " + title);
      dbMongo.insertInFetchedurls(fetchedURLSID++, title, 0);
    }
    dbMongo.retrieveElements(urlsBGD);
    dbMongo.retrieveLinkWithState1();
  }

  public void addinFetched(Element url, String url1, int currentID) throws MalformedURLException {
    String firstLink = url.attr("href");
    if (firstLink.contains("https")) {
      URL temp = new URL(url1);
      if (!robotsTxt.checkifAllowed(firstLink, temp)) {
        return;
      }

      synchronized (obj) {
        dbMongo.insertInFetchedurls(fetchedURLSID++, firstLink, 0);
      }
    }
  }

  public static String encryptThisString(String input) {
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
  }

  public void recrawlSeeds() throws IOException {
    FindIterable<Document> documents = dbMongo.URLSWithHTML.find().limit(6);
    int tempID = 0;
    for (Document doc : documents) {
      String url = doc.get("_url").toString();
      org.jsoup.nodes.Document temp;
      temp = Jsoup.connect(url).get();
      String encryptedHTML = encryptThisString(temp.toString());
      String tempEncrypted = doc.get("_encryption").toString();
      if (!encryptedHTML.equals(tempEncrypted)) {
        dbMongo.updateSeed(temp.toString(), tempID++, encryptedHTML);

        System.out.println("Updated document url " + url);
      }
    }
  }

  public static void main(String[] args)
      throws IOException, URISyntaxException, InterruptedException {
    DBManager db = new DBManager();
    Crawler crawl = new Crawler(db, new RobotsManager());
    crawl.initializeSeeds();
    Scanner sc = new Scanner(System.in);
    int numberOfThreads;
    numberOfThreads=sc.nextInt();
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
  }
}
