package com.prepare;

import java.io.*;
import java.util.*;

import com.comm.Util;

public class Config {
  public String bugID;
  public String srcPath;
  ArrayList<String> jarPath = new ArrayList<String>(); //generated based on srcPath
  ArrayList<String> packageFilter = new ArrayList<String>();
  ArrayList<String> packageExclude = new ArrayList<String>();

  public String rpcIdentWay;
  public String rpcCCKeyWord;
  public String rpcMethodKeyWord;
  public String rpcParaKeyWord;
  public String rpcSpecInstKeyWord;
  public String rpcOutPath;

  public String eveIdentWay;
  public String eveCCKeyWord;
  public String eveMethodKeyWord;
  public String eveParaKeyWord;
  public String eveSpecInstKeyWord;
  public String eveOutPath;

  public String calleeOutPath;

  public Config(String path) {
    try {
      BufferedReader buf = new BufferedReader(new FileReader(new File(path)));
      bugID = Util.readLine(buf);

      srcPath = Util.readLine(buf);
      File srcFile = new File(srcPath);
      if (srcFile.isFile()) jarPath.add(srcPath);
      else if (srcFile.isDirectory()) {
        ArrayList<String> list = new ArrayList<String>();
        if (bugID.startsWith("MR")) {
          list.add(srcPath + "common");
          list.add(srcPath + "mapreduce");
          list.add(srcPath + "yarn");
          for (String pathI : list) {
            File dir = new File(pathI);
            if (dir.exists() == false) continue;
            for (File jarFile : dir.listFiles()) {
              if (jarFile.isFile() && jarFile.getName().contains("test") == false) jarPath.add(jarFile.getAbsolutePath());
            }
          }
        }
        else if (bugID.equals("HB-10090")){
          jarPath.add("/home/haopliu/CFinder/app_src/hb-10090/hbase/hbase-client/target/hbase-client-0.96.1.jar");
          jarPath.add("/home/haopliu/CFinder/app_src/hb-10090/hbase/hbase-common/target/hbase-common-0.96.1.jar");
          jarPath.add("/home/haopliu/CFinder/app_src/hb-10090/hbase/hbase-hadoop-compat/target/hbase-hadoop-compat-0.96.1.jar");
          jarPath.add("/home/haopliu/CFinder/app_src/hb-10090/hbase/hbase-it/target/hbase-it-0.96.1.jar");
          jarPath.add("/home/haopliu/CFinder/app_src/hb-10090/hbase/hbase-prefix-tree/target/hbase-prefix-tree-0.96.1.jar");
          jarPath.add("/home/haopliu/CFinder/app_src/hb-10090/hbase/hbase-protocol/target/hbase-protocol-0.96.1.jar");
          jarPath.add("/home/haopliu/CFinder/app_src/hb-10090/hbase/hbase-server/target/hbase-server-0.96.1.jar");
          jarPath.add("/home/haopliu/CFinder/app_src/hb-10090/protobuf-java-2.5.0.jar");
        }
      }

      int packNum = Integer.parseInt(Util.readLine(buf));
      for (int i=0; i < packNum; i++)
        packageFilter.add(Util.readLine(buf));

      packNum = Integer.parseInt(Util.readLine(buf));
      for (int i=0; i < packNum; i++)
        packageExclude.add(Util.readLine(buf));

      rpcIdentWay = Util.readLine(buf);
      if (rpcIdentWay.equals("CCMethod")) {
        rpcCCKeyWord = Util.readLine(buf);
        rpcMethodKeyWord = Util.readLine(buf);
      }
      else if (rpcIdentWay.equals("Para")) {
        rpcParaKeyWord = Util.readLine(buf);
      }
      else if (rpcIdentWay.equals("CCMethod|Para")) {
        rpcCCKeyWord = Util.readLine(buf);
        rpcMethodKeyWord = Util.readLine(buf);
        rpcParaKeyWord = Util.readLine(buf);
      }
      else if (rpcIdentWay.equals("SpecInst")) {
        rpcSpecInstKeyWord = Util.readLine(buf);
      }

      rpcOutPath = Util.readLine(buf);

      eveIdentWay = Util.readLine(buf);
      if (eveIdentWay.equals("CCMethod")) {
        eveCCKeyWord = Util.readLine(buf);
        eveMethodKeyWord = Util.readLine(buf);
      }
      else if (eveIdentWay.equals("Para")) {
        eveParaKeyWord = Util.readLine(buf);
      }
      else if (eveIdentWay.equals("SpecInst")) {
        eveSpecInstKeyWord = Util.readLine(buf);
      }
      eveOutPath = Util.readLine(buf);

      calleeOutPath = Util.readLine(buf);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

