package com.comm;

import java.io.*;
import java.util.*;

public class RPCList {
  //String filePath;
  public ArrayList<RPC> rpcList = new ArrayList<RPC>();
  //public RPCList(String path) { filePath = path; }
  public void init(String path, int rpcVersion) {
    try {
      BufferedReader buf = new BufferedReader(new FileReader(path));
      String line;
      while ((line = Util.readLine(buf)) != null) {
        // new a rpc object
        RPC tmp = new RPC(line, rpcVersion);
        rpcList.add(tmp);
      }
      buf.close();
    } catch (Exception e) {e.printStackTrace();}
  }

  public int containCC (String cName, String mName, int paraNum) {
    for (RPC rpc : rpcList) {
      int codeFlag = rpc.equalCC(cName + " " + mName + " " + paraNum);
      if (codeFlag != -1) return codeFlag;
    }
    return -1;
  }

  public int containIface (String cName, String mName, int paraNum) {
    for (RPC rpc : rpcList) {
      int codeFlag = rpc.equalIface(cName + " " + mName + " " + paraNum);
      if (codeFlag != -1) return codeFlag;
    }
    return -1;
  }

  public int getRPCVersion (String cName, String mName) {
    for (RPC rpc : rpcList) {
      int paraNum = rpc.paraNum;
      int codeFlag = rpc.equalIface(cName + " " + mName + " " + paraNum);
      if (codeFlag != -1) return rpc.version;
    }
    return -1;
  }

}

