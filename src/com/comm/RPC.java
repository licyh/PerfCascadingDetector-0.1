package com.comm;

import java.io.*;
import java.util.*;

public class RPC {
  String className;
  public String ifaceName;
  public String methodName;
  int codeFlag;
  public int version; //mrv1 or mrv2
  public int paraNum;
  ArrayList<String> paraType = new ArrayList<String>();

  public RPC(String line, int rpcVersion) {
    String[] words = line.split(" ");
    int idx = 0;
    this.className = words[idx++];
    this.ifaceName = words[idx++];
    this.methodName = words[idx++];
    this.codeFlag = Integer.parseInt(words[idx++]);
    this.version = rpcVersion;
    try {
      this.paraNum = Integer.parseInt(words[idx]);
      idx++;
      for (int i=0 ; i < this.paraNum; i++) {
        paraType.add(words[idx++]);
      }
    }catch(NumberFormatException e)
    {
      String strN = words[idx];

      if(strN.equals("*"))
      {
        paraNum = -2;
      }
      else
      {
        e.printStackTrace();
      }
    }


  }

  public int equalCC(String line) {
    String[] words = line.split(" ");
    if (Util.strEqual(className, words[0]) == false) return -1;
    if (Util.strEqual(methodName, words[1]) == false) return -1;
    if ((paraNum != Integer.parseInt(words[2])) && (paraNum != -2)) return -1;
    return codeFlag;
  }
  public int equalIface(String line) {
    String[] words = line.split(" ");
    if (Util.strEqual(ifaceName, words[0]) == false) return -1;
    if (Util.strEqual(methodName, words[1]) == false) return -1;
    if ((paraNum != Integer.parseInt(words[2])) && (paraNum != -2)) return -1;
    return codeFlag;
  }

}
