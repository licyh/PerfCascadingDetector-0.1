package com.comm;

import java.io.*;
import java.util.*;

public class API {
  public enum RW { R, W } //read or write
  public enum LR { L, R } //local or remote

  String cc;
  String method;
  String rtType;
  int paraNum;
  ArrayList<String> paraType = new ArrayList<String>();
  RW rOrw;
  boolean throwE;
  LR locOrRem;

  public String className() { return cc; }
  public String methodName() { return method; }
  public String returnType() { return rtType; }
  public int paraNumber() { return paraNum; }
  public String ithPara(int i) { return paraType.get(i); }
  public boolean isRead() { return rOrw == RW.R; }
  public boolean throwException() { return throwE; }

  public API (String line) {
    String[] words = line.split(" ");
    int idx = 0;
    this.cc = words[idx++];
    this.method = words[idx++];
    this.rtType = words[idx++];
    this.paraNum = Integer.parseInt(words[idx++]);
    for (int i=0; i < this.paraNum; i++) {
      if (words[idx].equals("Ljava/lang/Object") == false &&
          words[idx].equals("Ljava/lang/Enum") == false) {
        Util.exitWithMsg("API parameter type is not object/enum. Exit.");
      }
      paraType.add(words[idx++]);
    }
    this.rOrw = API.RW.valueOf(words[idx++]);
    this.throwE = words[idx++].equals("Y");
    this.locOrRem = API.LR.valueOf(words[idx++]);
  }


  private boolean equal (String cName, String mName) {
    if (Util.strEqual(cc, cName) == false) return false;
    return Util.strEqual(method, mName);
  }
  public boolean isAPIRead (String cName, String mName) {
    return rOrw == RW.R && equal(cName, mName);
  }
  public boolean isAPIWrite (String cName, String mName) {
    return rOrw == RW.W && equal(cName, mName);
  }
}


