package com.noodle.search_engine;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.HashSet;

public class DBManager {
  MongoCollection<Document> fetchedURLS;
  MongoCollection<Document> URLSWithHTML;
  MongoCollection<Document> OldSeeds;

  DBManager() {
    String uri = "mongodb://localhost:27017";
    MongoClient mongo = MongoClients.create(uri);
    MongoDatabase db = mongo.getDatabase("a");
    fetchedURLS = db.getCollection("FetchedURLs");
    URLSWithHTML = db.getCollection("URLSWithHTML");
    OldSeeds = db.getCollection("OldSeeds");
  }

//  public void retrieveElements(HashMap<String, Integer> urls) {
        public void retrieveElements(HashSet<String> urls){

    FindIterable<Document> ddd = URLSWithHTML.find();
    for (Document ff : ddd) {
      urls.add((String) ff.get("_id"));
    }
  }
      public void retrieveSeeds(HashSet<String> urls){
//  public void retrieveSeeds(HashMap<String, Integer> urls) {
    int max;
    int i = 0;
    if (fetchedURLS.countDocuments() > 6) max = 6;
    else max = (int) fetchedURLS.countDocuments();

    FindIterable<Document> ddd = fetchedURLS.find();
    for (Document ff : ddd) {
      urls.add((String) ff.get("_url"));
      i++;

      if (i > max) break;
    }
  }

  public void retrieveTitles(HashSet<String> hashedUrls){
    FindIterable<Document> d = URLSWithHTML.find();
    for(Document doc: d){
      hashedUrls.add((String) doc.get("title"));
    }
  }

  public long getFetchedCount() {
    return fetchedURLS.countDocuments();
  }

  public long getHTMLURLsCount() {
    return URLSWithHTML.countDocuments();
  }

  public void insertIntoDBHtmls(
      String url, String html,String title){//, String hash) { // (long id, String url, String html,String hash){

    Document s =
        new Document // ("_id", id)
            // .append
            ("_id", url)
            .append("html", html).append("popularity",1).append("title",title);
    try{
    URLSWithHTML.insertOne(s);
    }
    catch (Exception e){
//      System.out.println("YALAHWWYYYYYY");
    }
  }

  public void insertInFetchedurls(String url, int state) { // (long id, String url, int state ){
    Document link =
        new Document // ("_id", id)
            // .append
            ("_url", url)
            .append("_state", state); // 0 added, 1 being processed, 2 done
    fetchedURLS.insertOne(link);
  }

  public Document returnDocwithstate(int state, Document doc, int check) {

    Document find0;
    if (check == 1) find0 = new Document("_state", state);
    else {
      find0 = new Document("_id", state);
    }
    // System.out.println(state);
    Document increase = new Document("$inc", doc);
    // System.out.println(doc.toString());
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
    options.returnDocument(ReturnDocument.AFTER);
    Document returned = new Document();
    returned = (Document) fetchedURLS.findOneAndUpdate(find0, increase, options);

    return returned;
  }

  public void updateDoc(Document doc, ObjectId id) {
    //        returnDocwithstate(id,doc,2);
    Document find0;
    find0 = new Document("_id", id);
    // System.out.println(state);
    Document increase = new Document("$inc", doc);
    // System.out.println(doc.toString());
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
    options.returnDocument(ReturnDocument.AFTER);
    Document returned = new Document();
    returned = (Document) fetchedURLS.findOneAndUpdate(find0, increase, options);
  }
  public void updatePopularity(Document doc, String url) {
    //        returnDocwithstate(id,doc,2);
    Document find0;
    find0 = new Document("_id", url);
    // System.out.println(state);
    Document increase = new Document("$inc", doc);
    // System.out.println(doc.toString());
    FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
    options.returnDocument(ReturnDocument.AFTER);
    Document returned = URLSWithHTML.findOneAndUpdate(find0, increase, options);
  }
  public void retrieveLinkWithState1() {
    while (true) {
      if (returnDocwithstate(1, new Document("_state", -1), 1) == null) break;
    }
  }

  public void updateSeed(String html, String id,String oldHTML,String title ){

    Document update = new Document();
    update.append("$set", new Document().append("html", html).append("_body","").append("NoOfWords",0).append("title",title));
    URLSWithHTML.updateOne(new Document().append("_id", id), update);
    OldSeeds.insertOne(new Document("_id",id).append("html",oldHTML));

  }
}
