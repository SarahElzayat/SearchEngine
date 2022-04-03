import java.io.*;
import java.util.HashSet;
import java.util.Scanner;
import java.net.URL;

import groovy.lang.GString;
import org.bson.Document;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
//import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
//import com.mongodb.client.result.InsertOneResult;
//import com.mongodb.client.model.UpdateOptions;

import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


//import javafx.util.Pair;


public class HelloWorld {

    private static org.jsoup.nodes.Document doc;

    public static void main(String[] args) throws IOException {
        int docNum = 1;
        String uri = "mongodb://localhost:27017";
        HashSet<String> urls = new HashSet<>();
        File myObj = new File("D:\\CMP #2\\Second Semester\\Advanced Programming Techniques\\IntelliJ\\seed.txt");
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase db = mongo.getDatabase("myDB");
        MongoCollection<Document> col = db.getCollection("Crawler");
//        Document sampleDoc = new Document("_id", "1").append("name", "John Smith");
//        col.insertOne(sampleDoc);
        Scanner myReader = new Scanner(myObj);
        for (int i = 0; i < 5; i++) //read from seeds.txt
        {
            String title;//= myReader.nextLine();

            title = myReader.nextLine();
            URL currentURL = new URL(title);
            String protol = currentURL.getProtocol();
            String host = currentURL.getHost();
            org.jsoup.nodes.Document robots ;
            String robotsURL = (new URL(currentURL.getProtocol()+ "://"+currentURL.getHost()+"/robots.txt")).toString();
//            try{robots = Jsoup.connect(robotsURL).get();}
//            catch (Exception e){
//                continue;
//            }
//            try {
//                InputStream stream = (new URL(robotsURL)).openStream();
//                byte bytes[]=new byte[1000];
//                int numRead = stream.read(bytes);
//            }
//            catch (IOException e){
//                continue;
//            }
            InputStream in = new URL(robotsURL).openStream();
            Scanner aaa = new Scanner(in).useDelimiter("\\A");
            String result = aaa.hasNext()? aaa.next() : "";

            System.out.println("currentURL is "+ currentURL);
            System.out.println("robots is "+ robotsURL);
            System.out.println(result);
            org.jsoup.nodes.Document doc;
            try {

                doc = Jsoup.connect(title).get();
            }
            catch (Exception e) {
                continue;
            }
            // String documentTitle = "NewFile";
            //col.find();
            //BufferedWriter writer ;//= new BufferedWriter(new FileWriter("D:\\CMP #2\\Second Semester\\Advanced Programming Techniques\\IntelliJ\\s" + docNum + ".html"));
            //docNum++;
            //writer.write(doc.toString());

            Document s = new Document("_id", title).append("html", doc.toString()).append("state", "checked");
            //state = n --> not downloaded yet
            col.insertOne(s);

            Elements el = doc.select("a[href]");
            //appends url to seeds.txt
            FileWriter myWriter = new FileWriter("D:\\CMP #2\\Second Semester\\Advanced Programming Techniques\\IntelliJ\\seed.txt", true);

            BufferedWriter bufferedWriter = new BufferedWriter(myWriter);
            //  bufferedWriter.newLine();

            for (Element lis : el) {

                if (!urls.contains(lis.attr("href"))) {

                    urls.add(lis.attr("href"));

                    // if(!(lis.attr("href").charAt(0)=='/' || lis.attr("href").charAt(0)=='#'  )) {
                    if (lis.attr("href").contains("https")) {

                        bufferedWriter.newLine();
                        System.out.println(lis.attr("abs:href"));
                        bufferedWriter.write(lis.attr("abs:href"));
                    }
                }
            }
            bufferedWriter.close();
        }
    }
}

