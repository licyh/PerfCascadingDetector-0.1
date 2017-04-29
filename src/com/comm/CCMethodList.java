package com.comm;

import java.io.*;
import java.util.*;

public class CCMethodList {
  String filePath;
  public ArrayList<CCMethodPair> ccMethodList = new ArrayList<CCMethodPair>();
  public CCMethodList(String path) { filePath = path; }
  public void init() {
    try {
      BufferedReader buf = new BufferedReader(new FileReader(filePath));
      String line;
      while ((line = Util.readLine(buf)) != null) {
        // new a ccmethod object
        CCMethodPair tmp = new CCMethodPair(line);
        ccMethodList.add(tmp);
      }
      buf.close();
    } catch (Exception e) {e.printStackTrace();}
  }

  public int contain (ArrayList<String> cNames, String mName, String mModifierStr) {
    for (CCMethodPair i : ccMethodList) {
      for (String cName : cNames) {
        int codeFlag = i.equal(cName + " " + mName);
        if (codeFlag != -1) {
          return mName.equals("main") ? (mModifierStr.contains("static") ? codeFlag:-1): codeFlag;
        }
      }
    }
    return -1;
  }
  public int contain (String cName, String mName, String mModifierStr) {
    for (CCMethodPair i : ccMethodList) {
      int codeFlag = i.equal(cName + " " + mName);
      if (codeFlag != -1) {
        return mName.equals("main") ? (mModifierStr.contains("static") ? codeFlag:-1): codeFlag;
      }
    }
    return -1;
  }

  public String getInCall (ArrayList<String> cNames, String mName) {
    for (CCMethodPair i : ccMethodList) {
      for (String cName : cNames) {
        if (i.equal(cName + " " + mName) != -1) return cName;
      }
    }
    return "";
  }
}
