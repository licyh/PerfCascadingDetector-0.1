package com.comm;

import java.io.*;
import java.util.*;

public class CalleeList {
  String filePath;
  ArrayList<Callee> calleeList = new ArrayList<Callee>();
  public CalleeList(String path) { filePath = path; }
  public void init() {
    try {
      BufferedReader buf = new BufferedReader(new FileReader(filePath));
      String line;
      while ((line = Util.readLine(buf)) != null) {
        // new a callee object
        Callee tmp = new Callee(line);
        calleeList.add(tmp);
      }
      buf.close();
    } catch (Exception e) {e.printStackTrace();}
  }

  public boolean isCallee (String cName, String mName, String sig) {
    for (Callee i : calleeList) {
      if (i.equal(cName, mName, sig)) return true;
    }
    return false;
  }
}




