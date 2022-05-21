package com.example.demo;
//MongoDB
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.*;
import org.bson.Document;


public class MongoDB {

    //Data Members
    private MongoClient mongoClient ;
    private MongoDatabase database ;


    //1.constructor
     public MongoDB(String Database)
    {
        //open Database
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase database = mongo.getDatabase(Database);
    }

    //2.getting collection
    public  MongoCollection<Document>  GetCollection(String collection_Name)
    {
        //return collection
        if(database!=null)
        {
          return database.getCollection(collection_Name);
        }
        return  null;
    }


    protected void finalize() throws Throwable {
        super.finalize();
        mongoClient.close();
    }



}
