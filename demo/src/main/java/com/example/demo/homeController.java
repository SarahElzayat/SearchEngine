package com.example.demo;

import com.mongodb.client.*;

import org.bson.Document;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;


@RestController// Controller
public class homeController {
    Phrase_sreach phraseSearch=new Phrase_sreach();
    queryprocessing queryProcessing=new queryprocessing();

    @GetMapping(value = "/")
    @ResponseBody
    public ModelAndView homePage() {
        ModelAndView modelAndView = new ModelAndView();
        MongoCollection<Document> collection;
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase db = mongo.getDatabase("Suggestions");
        collection = db.getCollection("Suggestions");

        FindIterable<Document> d = collection.find();
        String[] responseArray = new String[(int) collection.countDocuments()];
        int i = 0;
        for (Document doc : d) {
            responseArray[i++] = doc.get("query").toString();
        }
        modelAndView.addObject("suggestions", responseArray);
        modelAndView.setViewName("home");
        return modelAndView;
    }

    @GetMapping(value = "/search")
    @ResponseBody
    public ModelAndView GetForm(@RequestParam(name = "query", required = false) String query) throws JSONException {
        MongoCollection<Document> collection;
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase db = mongo.getDatabase("AAAA");
        collection = db.getCollection("AAAA");
        FindIterable<Document> d = collection.find();
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
        modelAndView.setViewName("results");
         query=query.trim();
        Vector<Vector<org.json.JSONObject>> resultorginal=new Vector<Vector<org.json.JSONObject>>(1);
        Vector<Vector<org.json.JSONObject>>  resultforms=new Vector<Vector<org.json.JSONObject>>(1);
         if(query.startsWith("\"") && query.endsWith("\"")){
            phraseSearch.Phraseprocess(query);
         }
         else{
             queryProcessing.query_process(query,resultorginal, resultforms);
         }
//         System.out.println(word.startsWith("\""));
//        System.out.println(word.endsWith("\""));
        System.out.println(query);
        return modelAndView;

    }
}
