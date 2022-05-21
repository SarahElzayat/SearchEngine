package com.noodle.search_engine;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.HashSet;

public class DBManager {
  MongoCollection<Document> fetchedURLS;
  MongoCollection<Document> URLSWithHTML;
  MongoCollection<Document> OldSeeds;

  DBManager() {
    //connect to localhost
    String uri = "mongodb://localhost:27017";
    MongoClient mongo = MongoClients.create(uri);
    //get the database if exist or create it
    MongoDatabase db = mongo.getDatabase("a");
    //get the collection if it exist or create it
    fetchedURLS = db.getCollection("FetchedURLs");
    URLSWithHTML = db.getCollection("URLSWithHTML");
    OldSeeds = db.getCollection("OldSeeds");
  }
  public void retrieveElements(HashSet<String> urls){
  //if crawler collection contains url add it in hashset to not fetch the same link twice
    FindIterable<Document> ddd = URLSWithHTML.find();
    for (Document ff : ddd) {
      urls.add((String) ff.get("_id"));
    }
  }
  public void retrieveSeeds(HashSet<String> urls){
    int max;
    int i = 0;

    if (fetchedURLS.countDocuments() > 6) max = 6;
    else max = (int) fetchedURLS.countDocuments();
    //if crawler collection contains url add it in hashset to not fetch the same seed twice
    FindIterable<Document> ddd = fetchedURLS.find();
    for (Document ff : ddd) {
      urls.add((String) ff.get("_url"));
      i++;
      if (i > max) break;
    }
  }
//retireve the tilles of urls added to not dd the same page twice
  public void retrieveTitles(HashSet<String> hashedUrls){
    FindIterable<Document> d = URLSWithHTML.find();
    for(Document doc: d){
      hashedUrls.add((String) doc.get("title"));
    }
  }
  //get count of documents inserted in db
  public long getHTMLURLsCount() {
    return URLSWithHTML.countDocuments();
  }
  //insert into db id==>url html==>page source title==>page title
  public void insertIntoDBHtmls(
      String url, String html,String title){
    Document s =
        new Document
            ("_id", url)
            .append("html", html).append("popularity",1).append("title",title);
    try{
      //command to insert in database
    URLSWithHTML.insertOne(s);
    }
    catch (Exception e){
    }
  }
  //insert intoo general database which is used to fetch from
  public void insertInFetchedurls(String url, int state) {
    Document link =
        new Document
            ("_url", url)
            .append("_state", state); // 0 added, 1 being processed, 2 done
    fetchedURLS.insertOne(link);
  }
  //
  public Document returnDocwithstate(int state, Document doc, int check) {
  //used to return record according to the state passed to the function
    Document find0;
    if (check == 1)
      //retrieve record that contain state 0 to fetch
      find0 = new Document("_state", state);
    else {
      //change the state of records from 1 to 0 to recrawl it
      find0 = new Document("_id", state);
    }
    //used to inc the specified record with the doc value
    Document increase = new Document("$inc", doc);
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
    options.returnDocument(ReturnDocument.AFTER);
    //after getting the document return it to check if it found record with these specifications
    Document returned = fetchedURLS.findOneAndUpdate(find0, increase, options);
    return returned;
  }

  public void updateDoc(Document doc, ObjectId id) {
    Document find0;
    //update the state of url according to the url given
    find0 = new Document("_id", id);
    //inc the state with value given
    Document increase = new Document("$inc", doc);
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
    options.returnDocument(ReturnDocument.AFTER);
    fetchedURLS.findOneAndUpdate(find0, increase, options);
  }
  public void updatePopularity(Document doc, String url) {
    Document find0;
    //update the popularity of url according to the url given
    find0 = new Document("_id", url);
    //increment the popularity with 1
    Document increase = new Document("$inc", doc);
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
    options.returnDocument(ReturnDocument.AFTER);
   URLSWithHTML.findOneAndUpdate(find0, increase, options);
  }
  public void retrieveLinkWithState1() {
    //change the state of documents that were disturbed during crawling to 0 to recrawl it
    while (true) {
      if (returnDocwithstate(1, new Document("_state", -1), 1) == null) break;
    }
  }

  public void updateSeed(String html, String id,String oldHTML,String title ){
  //update the html if url if it was updated and update its title
    Document update = new Document();
    update.append("$set", new Document().append("html", html).append("_body","").append("NoOfWords",0).append("title",title));
    URLSWithHTML.updateOne(new Document().append("_id", id), update);
    //add the old html page source in db
    OldSeeds.insertOne(new Document("_id",id).append("html",oldHTML));
  }
}
