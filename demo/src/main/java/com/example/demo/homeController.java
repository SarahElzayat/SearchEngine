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

    //    Phrase_sreach phraseSearch=new Phrase_sreach();
    Ranker r = new Ranker();

    public homeController() throws JSONException {
    }

    @GetMapping(value = "/")
    @ResponseBody
    public ModelAndView homePage() {
        ModelAndView modelAndView = new ModelAndView();
        MongoCollection<Document> collection;
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase db = mongo.getDatabase("SearchEngine");
        collection = db.getCollection("Suggestions");
        r.db.getCollection("Ranker").drop();
        FindIterable<Document> d = collection.find();
        String[] responseArray = new String[(int) collection.countDocuments()];
        int i = 0;
        for (Document doc : d) {
            responseArray[i++] = doc.get("query").toString();
        }
//        FindIterable<Document> d = r.rankerCollection.find().sort(new Document("rank",-1));
        modelAndView.addObject("suggestions", responseArray);
        modelAndView.setViewName("home");
        return modelAndView;
    }

    @GetMapping(value = "/search")
    @ResponseBody
    public ModelAndView GetForm(@RequestParam(name = "query", required = false) String query) throws JSONException {
        System.out.println(query.trim());

        //****RANKER*****//
        // query -> Ranker [ call query processor [returns 3 2d vector of results]] --> some criteria (ex rank = pop * relevance) --> sorted results

        MongoCollection<Document> suggestions = r.db.getCollection("Suggestions");
        Bson filter = Filters.eq("query", query);
        Bson update = new Document("$set",
                new Document("query", query));
        UpdateOptions options = new UpdateOptions().upsert(true);
        suggestions.updateOne(filter, update, options);
        double time = r.getResults(query.trim());
        time /=1000;
        FindIterable<Document> d = r.rankerCollection.find().sort(new Document("type",1).append("rank",-1));
        JSONArray responseArray = new JSONArray();
        for (Document doc : d) {
            JSONObject record = new JSONObject();
            record.put("url", doc.get("url"));
            record.put("header", doc.get("header"));
            record.put("paragraph", doc.get("paragraph"));
            responseArray.add(record);
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("results", responseArray);
        modelAndView.addObject("time",time);
        modelAndView.addObject("resultQuery",query);
        modelAndView.setViewName("results");


        return modelAndView;

    }
}
