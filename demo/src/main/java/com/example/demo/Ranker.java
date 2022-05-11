package com.example.demo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
//import com.mongodb.util.JSON;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

public class Ranker {
    Vector<Vector<JSONObject>> originalResults = new Vector<Vector<org.json.JSONObject>>(1);
    Vector<Vector<org.json.JSONObject>> stemmedResults = new Vector<Vector<org.json.JSONObject>>(1);
    Vector<Vector<JSONObject>> nonCommon = new Vector<Vector<org.json.JSONObject>>(1);
    Vector<Integer> noOfDocumentsForWord = new Vector<Integer>(1); // EH DA?
    queryprocessing queryProcessor = new queryprocessing();
    MongoCollection<Document> rankerCollection;
    MongoDatabase db;

    public Ranker() {
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        db = mongo.getDatabase("SearchEngine");
        db.getCollection("Ranker").drop();
        rankerCollection = db.getCollection("Ranker");
    }

    public void getResults(String query) throws JSONException {
        queryProcessor.query_process(query, originalResults, stemmedResults, nonCommon, noOfDocumentsForWord);
        for (int i = 0; i < originalResults.size(); i++) {
            for (int j = 0; j < originalResults.get(i).size(); j++) {
                JSONObject obj = originalResults.get(i).get(j);
                String s = obj.getString("_url");
                Document temp = new Document();
                temp.append("url", s);
                temp.append("header", s);
                temp.append("paragraph", s);
                rankerCollection.insertOne(temp);
            }
        }
    }

    public static void main(String[] args) throws JSONException {
        Ranker r = new Ranker();
        r.getResults("c++");
    }
}
