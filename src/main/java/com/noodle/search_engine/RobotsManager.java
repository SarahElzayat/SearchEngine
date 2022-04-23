
package com.noodle.search_engine;

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
    InputStream in = new URL(robotsURL).openStream();
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
   // System.out.println("iin getrobotsfile "+currentURL.getHost());
    hostsWithFetchedRobotsTxt.put(currentURL.getHost().toString(),disallows);
  }

  public Boolean checkifAllowed(String url,URL urlll) {
   // System.out.println("iin checkifallowed "+urlll.getHost());
    Vector<String> disallows = hostsWithFetchedRobotsTxt.get(urlll.getHost());
    //System.out.println(disallows.size());
    //String url = urlll.toString();
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
// * //? = //$