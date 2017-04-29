package com.comm;

public class CCMethodPair {
  public String className;
  public String methodName;
  public int paraNum;
  int codeFlag; //see methodUtil

  public CCMethodPair (String str) {
    String[] words = str.split(" ");
    this.className = words[0];
    this.methodName = words[1];
    this.paraNum = Integer.parseInt(words[2]);
    this.codeFlag = Integer.parseInt(words[3]);
  }

  public boolean equal (String cName, String mName) {
    if (Util.strEqual(className, cName) == false) return false;
    if (Util.strEqual(methodName, mName) == false) return false;
    return true;
  }

  public int equal (String str) {
    String[] words = str.split(" ");
    return equal(words[0], words[1]) ? codeFlag : -1;
  }
}



