/*package MongoDBPackage;*/
package com.example.demo;
//Mongo DB
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
//import com.mongodb.MongoClient;
import com.mongodb.client.*;
import org.bson.Document;




public class MongoDB {
    private MongoClient mongoClient ;
    public MongoDatabase database ;
    public MongoCollection<Document> collection;

    //1.constructor;
    public MongoDB(String Database,String collection_Name)
    {
//       mongoClient = new MongoClient("localhost", 27017);
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase database = mongo.getDatabase(Database);
//       database = mongoClient.getDatabase(Database);
       if(database!=null)
       {
           collection = database.getCollection(collection_Name);
       }



    }


    protected void finalize() throws Throwable {
        super.finalize();
        mongoClient.close();
    }



}
