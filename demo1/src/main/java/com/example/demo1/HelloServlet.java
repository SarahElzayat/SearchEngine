package com.example.demo1;

import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
//import org.jsoup.Jsoup;
import java.io.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "helloServlet", value = "/hello-servlet")
public class HelloServlet extends HttpServlet {
    private String message;

    public void init() {
        message = "Hello World!";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        String Search = request.getParameter("Search");
        // Hello
        MongoDB db=new MongoDB();
        db.printName(Search);
        FindIterable<Document> s=db.returnFromDB();
        String title="";
        String url="";
        String describtion="";
        String html="";
        int i=0;
        for(Document d:s){
            title=d.get("header").toString();
            url=d.get("url").toString();
            describtion=d.get("paragraph").toString();
            if(i==0){
                html="<html><body><dev id='tryDiv'>"+"<h1>" + title + "</h1>"+"<br>"+"<a href="+url+"\">"+title+"</a>"+"<br>"+describtion+"</dev></body></html>";
            }
            else{
                System.out.println("problem happened here");
                org.jsoup.nodes.Document doc= Jsoup.parse(html);
                if(doc ==null)
                    System.out.println("doc is null");
                Element div = doc.getElementById("tryDiv");
                if(div!=null) {
                    System.out.println("div is null");
                    div.append("<h1>" + title + "</h1>" + "<br>" + "<a href=" + url + "\">" + title + "</a>" + "<br>" + describtion);
                }
            }
            i++;
        }
        //  String name="https://www.google.com/";
        PrintWriter out = response.getWriter();
        // String html="<html><body><dev class=\"try\">"+"<h1>" + title + "</h1>"+"<br>"+"<a href="+url+"\">"+title+"</a>"+"<br>"+describtion+"</dev></body></html>";
       /* out.println("<html><body><dev class=\"try\">");
        out.println("<h1>" + title + "</h1>");
       // out.println(url);
        out.println("<br>");
        out.println("<a href="+url+"\">"+title+"</a>");
        out.println("<br>");
        out.println(describtion);
        out.println("</dev></body></html>");*/
        out.println(html);
    }

    public void destroy() {
    }
}