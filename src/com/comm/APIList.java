package com.comm;

import java.io.*;
import java.util.*;

public class APIList {

  String filePath;
  ArrayList<API> apiList = new ArrayList<API>();

  public APIList(String path) { filePath = path; }
  public void init() {
    try {
      BufferedReader buf = new BufferedReader(new FileReader(filePath));
      String line;
      while ((line = Util.readLine(buf)) != null) {
        // new a API object
        API tmp = new API(line);
        apiList.add(tmp);
      }
      buf.close();
    } catch (Exception e) {e.printStackTrace();}
  }

  public ArrayList<API> allReadAPI() {
    ArrayList<API> rt = new ArrayList<API>();
    for (API apiI : apiList) {
      if (apiI.isRead()) rt.add(apiI);
    }
    return rt;
  }

  public ArrayList<API> allWriteAPI() {
    ArrayList<API> rt = new ArrayList<API>();
    for (API apiI : apiList) {
      if (apiI.isRead() == false) rt.add(apiI);
    }
    return rt;
  }

  public String getAPIRead(ArrayList<String> cNames, String mName) {
    for (API api : apiList) {
      for (String cName : cNames) {
        if (api.isAPIRead(cName, mName)) return cName;
      }
    }
    return "";
  }
  public String getAPIWrite(ArrayList<String> cNames, String mName) {
    for (API api : apiList) {
      for (String cName : cNames) {
        if (api.isAPIWrite(cName, mName)) return cName;
      }
    }
    return "";
  }


}
