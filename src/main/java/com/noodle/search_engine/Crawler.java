  package com.noodle.search_engine;

  import io.mola.galimatias.GalimatiasParseException;
  import io.mola.galimatias.canonicalize.CombinedCanonicalizer;
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

  public class Crawler {
    DBManager dbMongo;
    RobotsManager robotsTxt;
    HashSet<String> urlsBGD = new HashSet<>(5000);
    HashSet<String> hashedHTMLS = new HashSet<>(5000);

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
      File myObj = new File("./seed.txt");
      Scanner myReader = new Scanner(myObj);
      URLSWithHTMLID = dbMongo.getHTMLURLsCount();
      fetchedURLSID = dbMongo.getFetchedCount();
      dbMongo.retrieveElements(urlsBGD);
      while (myReader.hasNextLine()) {
        title = myReader.nextLine();
        if (urlsBGD.contains(title)) continue;
        // fetch the link here
        this.crawl(title, 0);
      }
      //  System.out.println("done initialize seed");
    }

    public void fetchLinksfromDB() throws IOException, URISyntaxException {
      // System.out.println("fetching from db");

      dbMongo.retrieveLinkwithstate1();
      // System.out.println("returned from state 1");

      while (URLSWithHTMLID < 5000) {
        Document returnedDoc = new Document();
        returnedDoc = dbMongo.returnDocwithstate(0, new Document("_state", 1), 1);
        // System.out.println(returnedDoc.toString());
        currentID = Integer.parseInt(returnedDoc.get("_id").toString());
        // System.out.println(currentID);
        title = returnedDoc.get("_url").toString();
        // System.out.println(title);
        if (urlsBGD.contains(title)) {
          // System.out.println("helloooooo");
          dbMongo.updateDoc(new Document("_state", 1), currentID);
          // System.out.println(returnedDoc);
          continue;
        }
        this.crawl(title, 1);
      }
    }

    public void crawl(String url, int check) throws IOException {
      robotsTxt.getRobotsfile(url);
      org.jsoup.nodes.Document doc;
      doc = Jsoup.connect(url).get(); // fetch the link html source
      if (!doc.toString().contains("<!doctype html>")) {
        if (check == 1) {
          dbMongo.updateDoc(new Document("_state", 1), currentID);
        }
        return;
      }
      //    System.out.println("Before normalization " + url);
      url = normalize(url);
  //    System.out.println("Link: "+url.toString());
      String encryptedHTML = encryptThisString(doc.toString());
      if (checkIfHTMLAlreadyExists(encryptedHTML)) {
        dbMongo.insertIntoDBHtmls(URLSWithHTMLID++, url, doc.toString());
        urlsBGD.add(url);
        Elements el = doc.select("a[href]");
        for (Element lis : el) {
          this.addinFetched(lis, check, url);
        }
        if (check == 1) dbMongo.updateDoc(new Document("_state", 1), currentID);
      }
    }
    // fetched from currently used url
    public void addinFetched(Element url, int check, String url1) throws MalformedURLException {
      String firstLink = url.attr("href");
      if (firstLink.contains("https")) {
        //   System.out.println(firstLink);
        // URI temp1 = new URI(firstLink);
        URL temp = new URL(url1);
        if (!robotsTxt.checkifAllowed(firstLink, temp)) {
          if (check == 1) {
            dbMongo.updateDoc(new Document("_state", 1), currentID);
          }
          return;
        }

        dbMongo.insertInFetchedurls(fetchedURLSID++, firstLink, 0);
      }
    }

    public String normalize(String s) {
      io.mola.galimatias.URL url;
      try {
        url = io.mola.galimatias.URL.parse(s);
        CombinedCanonicalizer CC = new CombinedCanonicalizer();
        s = CC.canonicalize(url).toString();
        // System.out.println(s);
      } catch (GalimatiasParseException ex) {
        // Do something with non-recoverable parsing error
      }

      return s;
    }

    public boolean checkIfHTMLAlreadyExists(String encryptedHTML) {

      if (hashedHTMLS.contains(encryptedHTML)) return false;
      hashedHTMLS.add(encryptedHTML);
      return true;
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
  //      System.out.println("hashed "+hashtext);
        return hashtext;
      }

      // For specifying wrong message digest algorithms
      catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
      DBManager db = new DBManager();
      Crawler crawl = new Crawler(db, new RobotsManager());
      crawl.initializeSeeds();
      crawl.fetchLinksfromDB();
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
      // long URLSWithHTMLID = dbMongo.getHTMLURLsCount();//URLSWithHTML.countDocuments();
      //////////////////////// hi

      /*for (int i = 0; i < URLSWithHTML.countDocuments(); i++) {
        FindIterable<Document> ddd = URLSWithHTML.find();
        for (Document ff : ddd) {
          urlsBGD.add((String) ff.get("_url"));
        }
      }
      System.out.println(urlsBGD);*/
      // long fetchedURLSID = dbMongo.getFetchedCount();//fetchedURLS.countDocuments();
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
