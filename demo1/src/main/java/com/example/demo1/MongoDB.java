package com.example.demo1;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.*;
import org.bson.Document;
public class MongoDB {
    MongoCollection<Document> URLSWithHTML;
    public MongoDB(){
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase db = mongo.getDatabase("AAA");
        URLSWithHTML = db.getCollection("AAA");
    }
    public void printName(String name){
        System.out.println(name);
    }

    public String returnFromDB(){
        Document dbs=URLSWithHTML.find().first();
        return dbs.toString();
    }
}
