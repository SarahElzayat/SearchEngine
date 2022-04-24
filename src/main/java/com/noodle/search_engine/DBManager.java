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
    public long getFetchedCount(){
        return fetchedURLS.countDocuments();
    }
    public long getHTMLURLsCount(){
        return URLSWithHTML.countDocuments();
    }
    public void insertIntoDBHtmls(long id, String url, String html ){
        Document s =
                new Document("_id", id)
                        .append("_url", url)
                        .append("html", html);
        // state = n --> not downloaded yet
        URLSWithHTML.insertOne(s);
    }
    public void insertInFetchedurls(long id, String url, int state ){
        Document link =
                new Document("_id", id)
                        .append("_url", url)
                        .append("_state", state); // 0 added, 1 being processed, 2 done
        fetchedURLS.insertOne(link);
    }

    public Document returnDocwithstate(int state,Document doc,int check){

        Document find0;
        if(check==1)
            find0= new Document("_state", state);
        else{
            find0= new Document("_id", state);
        }
        //System.out.println(state);
        Document increase = new Document("$inc", doc);
        // System.out.println(doc.toString());
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.returnDocument(ReturnDocument.AFTER);
        Document returned = new Document();
        returned = (Document) fetchedURLS.findOneAndUpdate(find0, increase, options);
        //System.out.println(returned);
        /*if(returned==null)
            return true;
       // returned=returnedlink;
        return false;*/
        return returned;
        // System.out.println(returned.toString());


    }

    public void updateDoc(Document doc,int id){
       // System.out.println("in update"+id);
        returnDocwithstate(id,doc,2);
    }

    public void retrieveLinkwithstate1(){
        //  Document coco=new Document();
        while(true){
            if(returnDocwithstate(1,new Document("_state", -1),1)==null)
                break;
        }
    }
}
