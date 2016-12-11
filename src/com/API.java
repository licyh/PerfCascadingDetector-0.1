package com;

import java.io.*;
import java.util.*;

public class API {
  public enum RW {
    R, W
  }
  public enum LR {
    L, R
  }

  String cc;
  String method;
  String rtType;
  int paraNum;
  ArrayList<String> paraType;
  RW rOrw;
  boolean throwE;
  LR locOrRem;

  public API() {
    paraType = new ArrayList<String>();
  }

  public boolean isRead() {
    return rOrw == RW.R;
  }

  public String className() { return cc; }
  public String methodName() { return method; }
  public String returnType() { return rtType; }
  public int paraNumber() { return paraNum; }

  public String ithPara(int i) {
    return paraType.get(i);
  }

  public boolean throwException() {
    return throwE;
  }
}


