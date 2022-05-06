package com.noodle.search_engine;



import com.ibm.icu.impl.Assert;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.regex.*;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.util.Scanner;
import java.util.Vector;



public class RobotsManager {

  URL currentURL;
  static HashMap<String, Vector<String>> hostsWithFetchedRobotsTxt =
      new HashMap<String, Vector<String>>(5000);

  RobotsManager() {}

  public void getRobotsfile(String url) throws IOException {
    currentURL = new URL(url);
    if (hostsWithFetchedRobotsTxt.containsKey(currentURL.getHost())) return;

//    String robotsURL =
//        (new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt"))
//            .toString();
    URL temp = new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt");
    HttpURLConnection connection =  (HttpURLConnection)temp.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    int responseCode = connection.getResponseCode();
    if(responseCode!=200) return;
    System.out.println("At robots url //////////////////////////+////// "+temp.toString());

    InputStream in = new URL(temp.toString()).openStream();
    Scanner robots7aga = new Scanner(in).useDelimiter("\\A");
    String result = robots7aga.hasNext() ? robots7aga.next() : "";
    String[] array = result.split("\n"); // array of robots.txt as strings
    Vector<String> disallows = new Vector<String>(array.length);

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
    hostsWithFetchedRobotsTxt.put(currentURL.getHost().toString(), disallows);
  }

  public Boolean checkifAllowed(String url, URL urlll) {
    Vector<String> disallows = hostsWithFetchedRobotsTxt.get(urlll.getHost());
    if(disallows==null)
      return true;
    for (int y = 0; y < disallows.size(); y++) {
      String regex = disallows.get(y).replaceAll("\\*", ".*");
      regex = ".*" + regex + ".*";
      Pattern p = Pattern.compile(regex);
      Matcher mat = p.matcher(url);
      if (mat.matches()) return false;
    }
    return true;
  }
}
