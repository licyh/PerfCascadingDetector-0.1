package com.comm;

import java.util.ArrayList;

public class SYN {
  public enum RW { R, W } //release(R) or wait(W)

  String cc;
  String method;
  String rtType;
  int paraNum;
  ArrayList<String> paraType = new ArrayList<String>();
  RW rOrw;
  boolean throwE;

  public String className() { return cc; }
  public String methodName() { return method; }
  public String returnType() { return rtType; }
  public int paraNumber() { return paraNum; }
  public String ithPara(int i) { return paraType.get(i); }
  public boolean isRelease() { return rOrw == RW.R; }
  public boolean throwException() { return throwE; }

  public SYN(String line) {
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
    this.rOrw = SYN.RW.valueOf(words[idx++]);
    this.throwE = words[idx++].equals("Y");
  }


  private boolean equal (String cName, String mName) {
    if (Util.strEqual(cc, cName) == false) return false;
    return Util.strEqual(method, mName);
  }
  public boolean isAPIRelease (String cName, String mName) {
    return rOrw == RW.R && equal(cName, mName);
  }
  public boolean isAPIWait (String cName, String mName) {
    return rOrw == RW.W && equal(cName, mName);
  }
}


