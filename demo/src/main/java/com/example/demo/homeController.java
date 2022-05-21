package com.example.demo;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;


@RestController// Controller
public class homeController {

    /*Ranker constructor called to initialize database connections
     and fetch the URLSWithHTML database to reduce unnecessary overhead*/
    Ranker r = new Ranker();

    public homeController() throws JSONException {
    }

    /*When the user goes to localhost:[port]/ -> it receives a get request
       and redirects it to this function*/
    @GetMapping(value = "/")
    @ResponseBody
    public ModelAndView homePage() {
        ModelAndView modelAndView = new ModelAndView();
        MongoCollection<Document> suggestions;
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase db = mongo.getDatabase("SearchEngine");
        suggestions = db.getCollection("Suggestions"); /*gets the previous searches*/
        /*drops the ranker database, so each time the user searches for something different, the results don't concatenate*/
        r.db.getCollection("Ranker").drop();
        FindIterable<Document> d = suggestions.find();
        String[] responseArray = new String[(int) suggestions.countDocuments()];
        int i = 0;
        //Puts the suggestions in an array to send it to the UI
        for (Document doc : d) {
            responseArray[i++] = doc.get("query").toString();
        }
        modelAndView.addObject("suggestions", responseArray);
        modelAndView.setViewName("home");
        /*returns the home.html route*/
        return modelAndView;
    }

    /*When the user searches -> it receives a get request
   and redirects it to this function*/
    @GetMapping(value = "/search")
    @ResponseBody
    public ModelAndView GetForm(@RequestParam(name = "query", required = false) String query) throws JSONException {
        System.out.println(query.trim());//trims the query to git rid of all whitespaces
        MongoCollection<Document> suggestions = r.db.getCollection("Suggestions");
        //connects with the suggestions collection to insert the current search query
        Bson filter = Filters.eq("query", query);
        Bson update = new Document("$set",
                new Document("query", query));
        UpdateOptions options = new UpdateOptions().upsert(true);
        suggestions.updateOne(filter, update, options);//if the query already exists, nothing is done.
        double time = r.getResults(query.trim());
        time /= 1000;
        /*getResults returns the time needed to finish query processing, searching and ranking in milliseconds*/
        FindIterable<Document> d = r.rankerCollection.find().sort(new Document("type", 1).append("rank", -1));
        /* the ranked results are returned and sorted by type(original results- stememd results - non common results)
        then sorted by their ranking*/
        JSONArray responseArray = new JSONArray();
        /* loops over results to put them in JSON format to be displayed in the UI */
        for (Document doc : d) {
            JSONObject record = new JSONObject();
            record.put("url", doc.get("url"));
            record.put("header", doc.get("header"));
            record.put("paragraph", doc.get("paragraph"));
            responseArray.add(record);
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("results", responseArray); //sends the search results to the UI
        modelAndView.addObject("time", time); //sends the time needed
        modelAndView.addObject("resultQuery", query); //sends the search query to show what you're searching for
        modelAndView.setViewName("results");


        return modelAndView; //returns the results.html route

    }
}
