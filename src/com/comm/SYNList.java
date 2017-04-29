package com.comm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class SYNList {

  String filePath;
  ArrayList<SYN> synList = new ArrayList<SYN>();

  public SYNList(String path) { filePath = path; }
  public void init() {
    try {
      BufferedReader buf = new BufferedReader(new FileReader(filePath));
      String line;
      while ((line = Util.readLine(buf)) != null) {
        // new a API object
        SYN tmp = new SYN(line);
        synList.add(tmp);
      }
      buf.close();
    } catch (Exception e) {e.printStackTrace();}
  }

  public ArrayList<SYN> allRelease() {
    ArrayList<SYN> rt = new ArrayList<SYN>();
    for (SYN synI : synList) {
      if (synI.isRelease()) rt.add(synI);
    }
    return rt;
  }

  public ArrayList<SYN> allWait() {
    ArrayList<SYN> rt = new ArrayList<SYN>();
    for (SYN synI : synList) {
      if (synI.isRelease() == false) rt.add(synI);
    }
    return rt;
  }

  public String getAPIRelease(ArrayList<String> cNames, String mName) {
    for (SYN syn : synList) {
      for (String cName : cNames) {
        if (syn.isAPIRelease(cName, mName)) return cName;
      }
    }
    return "";
  }
  public String getAPIWait(ArrayList<String> cNames, String mName) {
    for (SYN syn : synList) {
      for (String cName : cNames) {
        if (syn.isAPIWait(cName, mName)) return cName;
      }
    }
    return "";
  }


}
