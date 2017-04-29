package com.prepare.Util;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;

import java.io.*;
import java.util.*;

public class PrepareUtil {

  public static boolean goodCC (String cName, ArrayList<String> packageFilter, ArrayList<String> packageExclude) {
    boolean filter = false;
    for (String str : packageFilter) {
      if (cName.startsWith(str)) {
        filter = true; break;
      }
    }
    if (filter == false) return false;
    for (String str : packageExclude) {
      if (cName.contains(str)) return false;
    }
    return true;
  }

  public static ArrayList<IClass> getAllCC (ClassHierarchy cha, ArrayList<String> packageFilter, ArrayList<String> packageExclude) {
    ArrayList<IClass> rt = new ArrayList<IClass>();
    for (IClass c: cha) {
      String cName = PrepareUtil.typeToPack(c.getName().toString());
      if (PrepareUtil.goodCC(cName, packageFilter, packageExclude)) {
        rt.add(c);
      }
    }
    return rt;
  }

  public static ArrayList<IClass> getCCFamily(IClass c) {
    ArrayList<IClass> rt = new ArrayList<IClass>();
    rt.add(c);
    for (IClass iface: c.getAllImplementedInterfaces()) rt.add(iface);
    IClass superCC = c.getSuperclass();
    if (superCC != null && !superCC.getName().toString().startsWith("Ljava/")) {
      rt.add(superCC);
      for (IClass iface : superCC.getAllImplementedInterfaces()) rt.add(iface);
    }
    return rt;
  }

  public static ArrayList<IClass> getCCByName(ArrayList<IClass> ccList, String postfix) {
    ArrayList<IClass> rt = new ArrayList<IClass>();
    for (IClass c : ccList) {
      for (IClass cFamily : PrepareUtil.getCCFamily(c)) {
        if (cFamily.getName().toString().endsWith(postfix)) {
          rt.add(c); break;
        }
      }
    }
    return rt;
  }

  public static ArrayList<IClass> getCCInList(ArrayList<IClass> ccList) {
    ArrayList<IClass> rt = new ArrayList<IClass>();
    for (IClass c : ccList) {
      if (c.isInterface() == false && c.isAbstract() == false) rt.add(c);
    }
    return rt;
  }
  public static ArrayList<IClass> getIfaceInList(ArrayList<IClass> ccList) {
    ArrayList<IClass> rt = new ArrayList<IClass>();
    for (IClass c : ccList) {
      if (c.isInterface() || c.isAbstract()) { rt.add(c); }
    }
    return rt;
  }

  public static String typeToPack (String type) {
    String rt = type;
    if (rt.startsWith("L")) rt = rt.substring(1);
    rt = rt.replaceAll("/", ".");
    return rt;
  }

  public static boolean methodInCC (IClass c, String method) {
    for (IMethod im : c.getDeclaredMethods()) {
      if (im.getName().toString().equals(method)) return true;
    }
    return false;
  }

  public static boolean methodInCC (IClass c, IMethod method) {
    String targetSig = method.getDescriptor().toString();
    for (IMethod im : c.getDeclaredMethods()) {
      if (im.getName().toString().equals(method.getName().toString())) {
        String methodSig = im.getDescriptor().toString();
        if (methodSig.equals(targetSig)) { return true; }
      }
    }
    return false;
  }

  public static boolean hasType (ArrayList<IClass> cc, String type) {
    for (IClass c : cc) {
      if (c.getName().toString().equals(type)) return true;
    }
    return false;
  }

  public static boolean cfHasType (ArrayList<IClass> cc, String type) {
    for (IClass c : cc) {
      for (IClass cf : PrepareUtil.getCCFamily(c)) {
        if (cf.getName().toString().equals(type)) return true;
      }
    }
    return false;
  }

  public static void writeToFile (String path, ArrayList<String> strs, boolean append) {
    if (path.equals("null")) return;
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(path, append));
      for (String str : strs) {
        writer.write(str + "\n");
      }
      writer.flush();
      writer.close();
    } catch (Exception e) { e.printStackTrace(); }
  }

}
