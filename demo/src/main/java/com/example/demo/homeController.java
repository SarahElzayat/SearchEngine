package com.example.demo;


import com.fasterxml.jackson.databind.util.JSONPObject;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

@RestController// Controller
public class homeController {

    @GetMapping("/")
    public ModelAndView homePage() {
        ModelAndView modelAndView = new ModelAndView("home");
        return modelAndView;
    }

    @GetMapping(value = "/search")
    @ResponseBody
    public ModelAndView GetForm(@RequestParam(name = "query",required = false) String query) {
        MongoCollection<Document> collection;
        String uri = "mongodb://localhost:27017";
        MongoClient mongo = MongoClients.create(uri);
        MongoDatabase db = mongo.getDatabase("AAAA");
        collection = db.getCollection("AAAA");
        FindIterable<Document> d = collection.find();
        JSONArray responseArray = new JSONArray();
        for(Document doc: d){
            JSONObject record = new JSONObject();
            record.put("url",doc.get("url"));
            record.put("header",doc.get("header"));
            record.put("paragraph",doc.get("paragraph"));
            responseArray.add(record);
        }
//        return query + " ++++ "+ responseArray.toJSONString();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("results",responseArray);
        modelAndView.setViewName("results");
        System.out.println(query);
        return modelAndView;

    }
}
