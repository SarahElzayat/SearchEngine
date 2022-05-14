
package com.noodle.search_engine;



import com.ibm.icu.impl.Assert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.regex.*;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.util.Scanner;
import java.util.Vector;



public class RobotsManager {

  //  Vector<String> disallows;
  URL currentURL;
  static HashMap<String,Vector<String>> hostsWithFetchedRobotsTxt = new HashMap<String,Vector<String>>(5000);

  RobotsManager() {
//    disallows = new Vector<String>(0);
  }

  public void getRobotsfile(String url) throws IOException {
    currentURL = new URL(url);
    if(hostsWithFetchedRobotsTxt.containsKey(currentURL.getHost().toString()))
      return;

    String robotsURL =
            (new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt"))
                    .toString();

    if (hostsWithFetchedRobotsTxt.containsKey(currentURL.getHost())) return;

    URL temp;
    // = new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt");
    try{
      temp = new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt");
    }
    catch (MalformedURLException e){
      return;
    }
//    HttpURLConnection connection =  (HttpURLConnection)temp.openConnection();
//    connection.setRequestMethod("GET");
//    connection.connect();
//    int responseCode ;
//    try{
//    responseCode =connection.getResponseCode();
//      if(responseCode!=200) return;
//    }
//    catch (UnknownHostException e) {
//      connection.disconnect();
//    }
//    System.out.println("At robots url //////////////////////////+////// "+temp.toString());
    BufferedReader readIn;
    try{
      readIn=new BufferedReader(new InputStreamReader(temp.openStream()));
    }
    catch (IOException e) {
      return;
    }
   // InputStream in = new URL(temp.toString()).openStream();
    Scanner robots7aga = new Scanner(readIn).useDelimiter("\\A");
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
    // System.out.println("iin getrobotsfile "+currentURL.getHost());
    hostsWithFetchedRobotsTxt.put(currentURL.getHost().toString(),disallows);
  }

  public Boolean checkifAllowed(String url,URL urlll) {
    // System.out.println("iin checkifallowed "+urlll.getHost());
    Vector<String> disallows = hostsWithFetchedRobotsTxt.get(urlll.getHost());

    //System.out.println(disallows.size());

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
