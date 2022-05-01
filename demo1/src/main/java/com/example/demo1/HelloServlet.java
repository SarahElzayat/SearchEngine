package com.example.demo1;

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
        String s=db.returnFromDB();
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>" + s + "</h1>");
        out.println("</body></html>");
    }

    public void destroy() {
    }
}