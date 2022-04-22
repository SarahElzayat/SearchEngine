package MongoDBPackage;

//Mongo DB
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoClient;
import org.bson.Document;




public class MongoDB {
    private MongoClient mongoClient ;
    public MongoDatabase database ;
    public MongoCollection<Document> collection;

    //1.constructor;
    public MongoDB(String Database,String collection_Name)
    {
       mongoClient = new MongoClient("localhost", 27017);
       database = mongoClient.getDatabase(Database);
       if(database!=null)
       {
           collection=database.getCollection(collection_Name);
       }
    }


    protected void finalize() throws Throwable {
        super.finalize();
        mongoClient.close();
    }



}
