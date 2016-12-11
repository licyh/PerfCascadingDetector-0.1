package com;

import java.io.*;
import java.util.*;

public class CalleeInfo {
  String filePath;
  ArrayList<Callee> calleeList;
  public CalleeInfo() {
    calleeList = new ArrayList<Callee>();
  }

  // like: "resource/api.txt"
  public void setInfoFilePath(String str) {
    filePath = str;
  }

  public void readFile() {
    try {
      InputStream in = CalleeInfo.class.getClassLoader().getResourceAsStream(filePath);
      Reader read = new InputStreamReader(in);
      BufferedReader buf = new BufferedReader(read);
      String line;
      String[] words;

      while ((line = buf.readLine()) != null) {
        if (line.startsWith("//")) { continue; }
        words = line.split(" ");
        Callee calleeI = new Callee();
        calleeI.className = words[0];
        calleeI.methodName = words[1];
        calleeI.sig = words[2];
        calleeList.add(calleeI);
      }
      buf.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean isCallee(String className, String methodName, String sig) {
    for (Callee i : calleeList) {
      if (i.className.equals(className) &&
          i.methodName.equals(methodName) &&
          i.sig.equals(sig)) {
        return true;
      }
    }
    return false;
  }
}

class Callee {
  String className;
  String methodName;
  String sig;
}
