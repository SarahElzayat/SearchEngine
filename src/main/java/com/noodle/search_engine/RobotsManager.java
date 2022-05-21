
package com.noodle.search_engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.regex.*;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.Vector;



public class RobotsManager {

  URL currentURL;
  static HashMap<String,Vector<String>> hostsWithFetchedRobotsTxt = new HashMap<String,Vector<String>>(5000);

  RobotsManager() {
  }

  public void getRobotsfile(String url) throws IOException {
    currentURL = new URL(url);
    //checking if the url robots.txt file was added before if it then return
    if(hostsWithFetchedRobotsTxt.containsKey(currentURL.getHost()))
      return;
    //conctenating to the string to get robots.txt file
    URL temp;
    try{
      temp = new URL(currentURL.getProtocol() + "://" + currentURL.getHost() + "/robots.txt");
    }
    catch (MalformedURLException e){
      return;
    }
    BufferedReader readIn;
    try{
      //openning the robots.txt to read its content
      readIn=new BufferedReader(new InputStreamReader(temp.openStream()));
    }
    catch (IOException e) {
      return;
    }
    //to merk the end or beginning of reading file
    Scanner robots7aga = new Scanner(readIn).useDelimiter("\\A");
    String result = robots7aga.hasNext() ? robots7aga.next() : ""; //add the results in result
    String[] array = result.split("\n"); // array of robots.txt as strings
    Vector<String> disallows = new Vector<String>(array.length); //vector that contains the disallows of the url in order not to be added
    for (int x = 0; x < array.length; x++) {
      if (array[x].contains("User-agent: *")) { //asking if it starts with the line we want to start adding in vector from
        x++;
        while (x < array.length && array[x].contains("Disallow")) {
          array[x] = array[x].replace("Disallow: ", "");
          //adding the disallows in vector
          disallows.add(array[x]);
          x++;
        }
      }
    }
    //adding the vector with its key in hashmap
    hostsWithFetchedRobotsTxt.put(currentURL.getHost().toString(),disallows);
  }

  public Boolean checkifAllowed(String url,URL urlll) {
    //retrieving the disallow vector from hashmap
    Vector<String> disallows = hostsWithFetchedRobotsTxt.get(urlll.getHost());
    if(disallows==null)
      return true;
    for (int y = 0; y < disallows.size(); y++) {
      //changing the disallow to match with anything before and after it in url
      String regex = disallows.get(y).replaceAll("\\*", ".*");
      regex = ".*" + regex + ".*";
      Pattern p = Pattern.compile(regex);
      Matcher mat = p.matcher(url);
      //if it was from disallows return false
      if (mat.matches()) return false;
    }
    //it is safe to fetch
    return true;
  }


}
