package com.sa;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;


import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.classLoader.CallSiteReference;


import com.sa.CalleeCG;

public class CassandraCallee { 
  public static String format(String str) {
    String rt = str;
    if (rt.startsWith("L")) {
      rt = rt.substring(1);
    }
    rt = rt.replaceAll("/", ".");
    return rt;
  }

  public static boolean containMethod(IClass c, IMethod method) {
    boolean rt = false;
    String targetSig = method.getDescriptor().toString();
    for (IMethod im : c.getDeclaredMethods()) {
      if (im.getName().toString().equals(method.getName().toString())) {
        String methodSig = im.getDescriptor().toString();
        //System.out.println("Src cc:" + method.getDeclaringClass().getName().toString() + "; mehtod: " + method.getName().toString() + "; sig: " +  methodSig);
        //System.out.println("Targ cc: " + im.getDeclaringClass().getName().toString() + "; method: "+  im.getName().toString() + "; sig: " + targetSig);
        if (methodSig.equals(targetSig)) {
          rt = true;
        }
      }
    }
    return rt;
  }

  public static void main(String[] args) {
    //String path = "/mnt/storage/shared/apache-cassandra-0.6.6.jar";
    String path = "/mnt/storage/shared/apache-cassandra-0.6.6-0706.jar";
    AnalysisScope scope = null;;
    ClassHierarchy cha = null;

    ArrayList<IClass> classList = new ArrayList<IClass>();
    ArrayList<IClass> rpcIface = new ArrayList<IClass>();
    ArrayList<IClass> rpcClass = new ArrayList<IClass>();

    try {
      scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(path, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
      cha = ClassHierarchy.make(scope);

      for (IClass c : cha) {
        if (c.getName().toString().startsWith("Lorg/apache/cassandra") == false) continue;
        classList.add(c);
        for (IClass cc : c.getAllImplementedInterfaces()) {
          if (cc.getName().toString().endsWith("IVerbHandler")) {
            if (c.isInterface()) {
              rpcIface.add(c);
            }
            else {
              rpcClass.add(c);
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("RPC Class: ");

    ArrayList<IMethod> handleMethods = new ArrayList<IMethod>();
    for (IClass c : rpcClass) {
      System.out.println(c.getName().toString());
      for (IMethod m : c.getDeclaredMethods()) {
        if (m.getName().toString().equals("doVerb")) {
         System.out.println(" --> " + m.getName().toString());
         handleMethods.add(m);
        }
      }
    }

    //add StorageService.initClient into handlerMethods.
    //Since callgraph is context-sentsitive. Add this function, then wala can figure out that:
    //Gossiper.doNotifications callsite onChange can possible point to StorageService.java"
    for (IClass c : classList) {
      for (IMethod m : c.getDeclaredMethods()) {
        if (m.getName().toString().equals("initClient") &&
            c.getName().toString().endsWith("StorageService")) {
          handleMethods.add(m);
        }
      }
    }

    //package filter: only conside class in these packages
    ArrayList<String> packageFilter = new ArrayList<String>();
    packageFilter.add("org.apache.cassandra.");

    //method filter: exclude these methods
    ArrayList<IMethod> methodFilter = new ArrayList<IMethod>();
    for (IMethod m : handleMethods) { methodFilter.add(m); }

    CalleeCG handleCG = new CalleeCG(scope, cha);
    handleCG.setEntryMethods(handleMethods);
    handleCG.buildCG();
    ArrayList<IMethod> handleCallee = handleCG.getAllCallee(packageFilter, methodFilter);
    System.out.println("Overall handle: " + handleMethods.size() + " callee: " + handleCallee.size());


    //cnt signal/wait
    handleCG.cntSignalWait(handleCallee);

    System.out.println("write callee to file.");
    handleCG.writeToFile(handleCallee, "../../src/com/cassandra_callee.txt_new", false);
  }
}

