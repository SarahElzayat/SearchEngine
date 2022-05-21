package com.noodle.search_engine;

import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Scanner;

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
    // looping over count to crawl 5100 pages only
    while (URLSWithHTMLID < 5100) {
      Document returnedDoc = new Document();
      synchronized (obj) {
        // getting the first url with state 0 which indicates it was not crawled yet
        returnedDoc = dbMongo.returnDocwithstate(0, new Document("_state", 1), 1);
        if (returnedDoc == null) {
          break;
        }
      }
      // getting id used in updating its state
      currentID = (ObjectId) returnedDoc.get("_id");
      // checking if the same url was fetched before
      if (urlsBGD.contains(returnedDoc.get("_url").toString())) {
        // if it was fetched before increase it popularity and change it state to 2
        dbMongo.updatePopularity(new Document("popularity", 1), returnedDoc.get("_url").toString());
        dbMongo.updateDoc(new Document("_state", 1), currentID);
        continue;
      }
      try {
        // get robots.txt file of this url to get dissallows
        robotsTxt.getRobotsfile(returnedDoc.get("_url").toString());
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
        // checking that the link is html file to fetch and if it's language is english
        Element taglang = doc.select("html").first();
        if (!doc.toString().toLowerCase().contains("<!doctype html>")
            || !doc.toString().toLowerCase().contains(" lang=\"en")) {
          // if they weren't both change their state to 2 so It won't be fetched again
          dbMongo.updateDoc(new Document("_state", 1), currentID);
          continue;
        }
        String encryptedHTML = encryptThisString(doc);
        // check if two url navigate to the same page I fetched before
        if (!hashedHTMLS.contains(encryptedHTML)) {
          synchronized (obj) {
            hashedHTMLS.add(encryptedHTML);
            URLSWithHTMLID++;
            System.out.println(
                TEXT_BLUE
                    + "FROM THREAD "
                    + currentThread()
                    + " "
                    + returnedDoc.get("_url").toString()
                    + TEXT_RESET);
            // insert the url in database
            dbMongo.insertIntoDBHtmls(
                returnedDoc.get("_url").toString(), doc.toString(), encryptedHTML);
          }
          synchronized (obj) {
            // add url in the hashset
            urlsBGD.add(returnedDoc.get("_url").toString());
          }
          // fetching the url to get first 50 hyperlinks in it
          Elements el = doc.select("a[href]");
          int counter = 0;
          for (Element lis : el) {
            if (counter > 50) break;
            String firstLink = lis.attr("href");
            // check if the link started with https or no
            if (firstLink.startsWith("https://")
                // ignore all the links that contains the following
                && !(firstLink.contains("register")
                    || firstLink.contains("signup")
                    || firstLink.contains("signin")
                    || firstLink.contains("help")
                    || firstLink.contains("twitter")
                    || firstLink.contains("login"))) {
              // check if the url is from dissallowed or not
              URL temp = new URL(returnedDoc.get("_url").toString());
              if (!robotsTxt.checkifAllowed(firstLink, temp)) {
                continue;
              }
              // add this link in database to fetch again
              dbMongo.insertInFetchedurls(firstLink, 0);
            }
            counter++;
          }
          // update the state to 2 to indicate that this url was fetched successfully
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        } else {
          // update the state to 2 to indicate that this url was fetched successfully
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        }
      } catch (IOException e) {
        // update the state to 2 to indicate that this url was fetched successfully
        dbMongo.updateDoc(new Document("_state", 1), currentID);
        continue;
      }
    }
  }

  public void initializeSeeds() throws IOException {
    // opening seed file
    File myObj = new File("./seed.txt");
    Scanner myReader = new Scanner(myObj);
    // retreiving count of documents in db
    URLSWithHTMLID = dbMongo.getHTMLURLsCount();
    // retreive elements in the hashset
    dbMongo.retrieveSeeds(urlsBGD);
    while (myReader.hasNextLine()) {
      String title = myReader.nextLine();
      if (urlsBGD.contains(title))
        continue; // checking if url was fetched before in case of any disturbance occured
      // fetch the link here
      dbMongo.insertInFetchedurls(title, 0);
    }
    dbMongo.retrieveElements(urlsBGD);
    dbMongo.retrieveTitles(hashedHTMLS);
    // change state of url from 1 to 0 in order to bet fetched again because it didn't fetch
    // successfully
    dbMongo.retrieveLinkWithState1();
  }

  public static String encryptThisString(org.jsoup.nodes.Document input) {
    // returning title of url to check if it was visited before
    if (input.title().equals("")) return input.attr("title");
    return input.title();
  }

  public void recrawlSeeds() throws IOException {
    dbMongo.OldSeeds.drop();
    // getting the seeds from db to recrawl it
    FindIterable<Document> documents = dbMongo.URLSWithHTML.find().limit(6);
    int tempID = 0;
    for (Document doc : documents) {
      String url = doc.get("_id").toString();
      org.jsoup.nodes.Document temp;
      temp = Jsoup.connect(url).get();
      // getting hashcode of new page source
      int hashedNew = temp.hashCode();
      String html = doc.get("html").toString();
      // getting hashcode of old page source
      int hashedOld = html.hashCode();
      // check if it wasn't equal which indicates it was updated to change it
      if (hashedOld != hashedNew) {
        // updating html file with the new source
        dbMongo.updateSeed(temp.toString(), url, html, temp.title());
      }
    }
  }

  public static void main(String[] args)
      throws IOException, URISyntaxException, InterruptedException {
    DBManager db = new DBManager();
    Crawler crawl = new Crawler(db, new RobotsManager());
    // adding the seeds in db to be crawled
    crawl.initializeSeeds();
    System.out.println("ENTER NUMBER OF THREADS");
    Scanner sc = new Scanner(System.in);
    int numberOfThreads;
    // reading number of threads from the user
    numberOfThreads = sc.nextInt();

    Crawler threads[] = new Crawler[numberOfThreads];
    for (int i = 0; i < numberOfThreads; i++) {
      threads[i] = new Crawler(new DBManager(), new RobotsManager());
      threads[i].setName(Integer.toString(i));
      threads[i].start();
      threads[i].sleep(3000);
    }
    // wait for the threads to join then terminate
    try {
      for (int i = 0; i < numberOfThreads; i++) {
        threads[i].join();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    //    function to recrawl the seeds uncomment it to use
    crawl.recrawlSeeds();
  }
}
