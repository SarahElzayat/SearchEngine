package com.noodle.search_engine;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.HashSet;


public class DBManager {
    MongoCollection<Document> fetchedURLS;
    MongoCollection<Document> URLSWithHTML;
    Document returnedlink;
    DBManager(){
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase db = mongo.getDatabase("SearchEngine");
        fetchedURLS = db.getCollection("FetchedURLs");
        URLSWithHTML = db.getCollection("URLSWithHTML");
    }
    public void retrieveElements(HashSet<String> urls){
        for (int i = 0; i < URLSWithHTML.countDocuments(); i++) {
            FindIterable<Document> ddd = URLSWithHTML.find();
            for (Document ff : ddd) {
                urls.add((String) ff.get("_url"));
            }
        }
    }
     public long getfetchedcount(){
        return fetchedURLS.countDocuments();
     }
    public long gethtmlurlsCount(){
        return URLSWithHTML.countDocuments();
    }
    public void insertIntodbHtmls(long id,String url,String html ){
        Document s =
                new Document("_id", id)
                        .append("_url", url)
                        .append("html", html);
        // state = n --> not downloaded yet
        URLSWithHTML.insertOne(s);
    }
    public void insertinFetchedurls(long id,String url,int state ){
        Document link =
                new Document("_id", id)
                        .append("_url", url)
                        .append("_state", state); // 0 added, 1 being processed, 2 done
        fetchedURLS.insertOne(link);
    }

    public void returnDocwithstate(int state,Document doc,Document returned){
        Document find0 = new Document("_state", state);

        Document increase = new Document("$inc", doc);
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.returnDocument(ReturnDocument.AFTER);
        returnedlink = (Document) fetchedURLS.findOneAndUpdate(find0, increase, options);
        returned=returnedlink;

    }

    public void updateDoc(Document doc){
        returnDocwithstate(Integer.parseInt(returnedlink.get("_id").toString()),doc,new Document());
    }

    public void retrieveLinkwithstate1(){
        while(true){
            Document coco=new Document();
            returnDocwithstate(1,new Document("_state", -1),coco);
            if(coco==null)
                break;
        }
    }
}
