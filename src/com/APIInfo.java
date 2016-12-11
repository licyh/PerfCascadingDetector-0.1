package com;

import java.io.*;
import java.util.*;

public class APIInfo {
  String filePath;
  ArrayList<API> apiList;

  public APIInfo() {
    apiList = new ArrayList<API>();
  }

  public void setInfoFilePath(String str) { filePath = str; }

  public void readFile() {
    try {
      InputStream apiIn = APIInfo.class.getClassLoader().getResourceAsStream("resource/api.txt");
      //BufferedReader buf = new BufferedReader(new FileReader(filePath));
      Reader apiReader = new InputStreamReader(apiIn);
      BufferedReader buf = new BufferedReader(apiReader);
      String line;
      String[] words;

      while ((line = buf.readLine()) != null) {
        if (line.startsWith("//")) {
          continue;
        }
        words = line.split(" ");
        int ind = 0;
        API apiI = new API();
        apiI.cc = words[ind++];
        apiI.method = words[ind++];
        apiI.rtType = words[ind++];
        apiI.paraNum = Integer.parseInt(words[ind++]);
        for (int i = 0; i < apiI.paraNum; i++ ) {
          if (words[ind].equals("Ljava/lang/Object") == false &&
              words[ind].equals("Ljava/lang/Enum") == false) {
            System.out.println("api: " + line);
            System.out.println("API Parameter type is not Object. Exit");
            System.exit(-1);
          }
          apiI.paraType.add(words[ind++]);
        }
        apiI.rOrw = API.RW.valueOf(words[ind++]);
        if (words[ind++].equals("Y"))
          apiI.throwE = true;
        else
          apiI.throwE = false;
        apiI.locOrRem = API.LR.valueOf(words[ind++]);
        apiList.add(apiI);
      }
      buf.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ArrayList<API> allReadAPI() {
    ArrayList<API> rt = new ArrayList<API>();
    for (API apiI : apiList) {
      if (apiI.isRead()) {
        rt.add(apiI);
      }
    }
    return rt;
  }

  public ArrayList<API> allWriteAPI() {
    ArrayList<API> rt = new ArrayList<API>();
    for (API apiI : apiList) {
      if (apiI.isRead() == false) {
        rt.add(apiI);
      }
    }
    return rt;
  }

  //TODO compare api.
}
