package com.prepare;

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.classLoader.IMethod;

import com.ibm.wala.ipa.callgraph.AnalysisScope;

import java.io.*;
import java.util.*;

import com.prepare.Util.WALACG;
import com.prepare.Util.PrepareUtil;

public class Callee {
  Config config;
  WALACG calleeCG;
  ArrayList<IMethod> methodFilter;

  public Callee (AnalysisScope scope, ClassHierarchy cha, Config config_) {
    config = config_;
    calleeCG = new WALACG(scope, cha);
  }

  public void setEntryMethods(ArrayList<IMethod> methodList) {
    calleeCG.setEntryMethods(methodList);
    calleeCG.buildCG();
  }
  public void setMethodFilter(ArrayList<IMethod> methodFilter_) {
    methodFilter = methodFilter_;
  }

  /* not useful under CFinder.
   * Callee in cfinder is not as important as in DCatch.
   */
  public void updateEntry() {
    ArrayList<IMethod> unClearList = calleeCG.unClearMethod(config.packageFilter, methodFilter);
    int times = 0;
    while (unClearList.size() > 0 && times < 2) {
      System.out.println("Time: " + times);
      times++;
      for (IMethod m : unClearList) System.out.println("  m: " + m.getDeclaringClass().getName().toString() + "::" + m.getName().toString());
      System.out.println();
      ArrayList<IMethod> newEntries = new ArrayList<IMethod>();
      newEntries.addAll(unClearList);
      newEntries.addAll(calleeCG.entryMethods);
      AnalysisScope scope = calleeCG.scope;
      ClassHierarchy cha = (ClassHierarchy)calleeCG.cha;

      calleeCG = new WALACG(scope, cha);
      calleeCG.setEntryMethods(newEntries);
      calleeCG.buildCG();
      unClearList = calleeCG.unClearMethod(config.packageFilter, methodFilter);
    }
  }



  public ArrayList<String> entryMethodToStrArray() {
    ArrayList<String> rt = new ArrayList<String>();
    rt.add("//format: class method sign");
    for (IMethod m : calleeCG.entryMethods) {
      String str = PrepareUtil.typeToPack(m.getDeclaringClass().getName().toString()) + " ";
      str += m.getName().toString() + " ";
      str += m.getDescriptor().toString();
      rt.add(str);
    }
    return rt;
  }


  public ArrayList<String> toStrArray() {
    ArrayList<String> rt = new ArrayList<String>();
    rt.add("//format: class method sign");
    for (IMethod m : calleeCG.getAllCallee(config.packageFilter, methodFilter)) {
      String str = PrepareUtil.typeToPack(m.getDeclaringClass().getName().toString()) + " ";
      str += m.getName().toString() + " ";
      str += m.getDescriptor().toString();
      rt.add(str);
    }
    return rt;
  }

}

