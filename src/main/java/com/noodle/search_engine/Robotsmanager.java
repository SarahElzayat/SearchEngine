package com.noodle.search_engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.Vector;

public class Robotsmanager {

    Vector<String> disallows;
    URL currentURL;
    Robotsmanager (){
        Vector<String> disallows = new Vector<String>(0);
    }

    public void getRobotsfile(String url) throws IOException {
        currentURL = new URL(url);
        String robotsURL =
                (new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt"))
                        .toString();
        InputStream in = new URL(robotsURL).openStream();
        Scanner robots7aga = new Scanner(in).useDelimiter("\\A");
        String result = robots7aga.hasNext() ? robots7aga.next() : "";
        String[] array = result.split("\n"); // array of robots.txt as strings
       // Vector<String> disallows = new Vector<String>(0);

        for (int x = 0; x < array.length; x++) {
            if (array[x].contains("User-agent: *")) {
                x++;
                while (x < array.length && array[x].contains("Disallow")) {
                    array[x] = array[x].replace("Disallow: ", "");
                    disallows.add(array[x]);
                    x++;
                }
            }
        }
    }

    public Boolean checkifAllowed(String url){
        Boolean tmam= true;
        for (int y = 0; y < disallows.size(); y++) {
            if (url.contains(disallows.get(y))
                    && url.contains(currentURL.getHost())) {
                tmam = false;
            }
        }
        return tmam;
    }

}
