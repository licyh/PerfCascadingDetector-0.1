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

import com.ibm.wala.classLoader.IBytecodeMethod;

import com.sa.CalleeCG;

public class HBaseRPC { 
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
    String path = "/mnt/storage/haopeng/workstation/java-ws/DC-Detector/hbase_test_multinodes/hbase/target/hbase-0.93-SNAPSHOT.jar";
    AnalysisScope scope = null;;
    ClassHierarchy cha = null;

    ArrayList<IClass> classList = new ArrayList<IClass>();
    ArrayList<IClass> rpcIface = new ArrayList<IClass>();
    ArrayList<IClass> rpcClass = new ArrayList<IClass>();

    try {
      scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(path, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
      cha = ClassHierarchy.make(scope);

      for (IClass c : cha) {
        if (c.getName().toString().startsWith("Lorg/apache/hadoop/hbase") == false) continue;
        classList.add(c);
        for (IClass cc : c.getAllImplementedInterfaces()) {
          if (cc.getName().toString().endsWith("VersionedProtocol")) {
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

    /*System.out.println("RPC Iface: ");
    for (IClass c : rpcIface) {
      System.out.println(" " + c.getName().toString());
    }*/
    ArrayList<String> rpcList = new ArrayList<String>();
    ArrayList<IMethod> rpcMethods = new ArrayList<IMethod>();
    ArrayList<RPCIface> rpcTuple = new ArrayList<RPCIface>();

    System.out.println("RPC Class: ");
    for (IClass c : rpcClass) {
      for (IClass cc : c.getAllImplementedInterfaces()) {
        if (rpcIface.contains(cc)) {
          for (IMethod m : c.getDeclaredMethods()) {
            if (containMethod(cc, m)) {
              if (m.isPublic()) {
                String str = HBaseRPC.format(c.getName().toString()) + " ";
                str += HBaseRPC.format(cc.getName().toString()) + " ";
                str += m.getName().toString() + " ";
                str += (m.getNumberOfParameters()-1) + " ";
                for (int i=1; i < m.getNumberOfParameters(); i++) {
                  if (m.getParameterType(i).isReferenceType()) {
                    str += "Ljava/lang/Object ";
                  }
                  else {
                    str += m.getParameterType(i).getName() + " ";
                  }
                }
                rpcList.add(str);
                /*rpcList.add(HBaseRPC.format(c.getName().toString()) + " " + HBaseRPC.format(cc.getName().toString())
                             + " " + m.getName().toString());*/
                rpcMethods.add(m);
                RPCIface tmp = new RPCIface();
                tmp.setIface(cc);
                tmp.setClass(c);
                tmp.setMethod(m);
                rpcTuple.add(tmp);
              }
            }
          }
        }
      }
    }

    System.out.println("Write rpc list to file.");
    try {
      PrintWriter out = new PrintWriter("../../src/com/hbase_rpc_4539.txt_tmp", "UTF-8");
      out.println("//format: class iface method");
      for (String i : rpcList) {
        out.println(i);
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    ArrayList<IMethod> handleMethods = new ArrayList<IMethod>();
    for (IClass c : classList) {
      IClass cc = c.getSuperclass();
      if (cc.getName().toString().endsWith("/EventHandler")) {
        for (IMethod m : c.getDeclaredMethods()) {
          if (m.getName().toString().equals("process")) {
           handleMethods.add(m);
          }
        }
      }
    }


    if (path.contains("hbase_test_multinodes")) {
      /* it's mainly for hb-4539 call-graph*/
      for (IClass c : classList) {
        for (IMethod m : c.getDeclaredMethods()) {
          if (m.getName().toString().equals("getZooKeeper")) {
            System.out.println("Debug... c: " + c.getName() + " m: " + m.getName());
            handleMethods.add(m);
          }
        }
      }
    }

    //package filter: only conside class in these packages
    ArrayList<String> packageFilter = new ArrayList<String>();
    packageFilter.add("org.apache.hadoop.hbase.");

    //method filter: exclude these methods
    ArrayList<IMethod> methodFilter = new ArrayList<IMethod>();
    for (IMethod m : rpcMethods) { methodFilter.add(m); }
    for (IMethod m : handleMethods) { methodFilter.add(m); }

    CalleeCG rpcCG = new CalleeCG(scope, cha);
    rpcCG.setEntryMethods(rpcMethods);
    rpcCG.buildCG();
    ArrayList<IMethod> rpcCallee = rpcCG.getAllCallee(packageFilter, methodFilter);
    System.out.println("Overall rpc: " + rpcMethods.size() + " callee: " + rpcCallee.size());

    CalleeCG handleCG = new CalleeCG(scope, cha);
    handleCG.setEntryMethods(handleMethods);
    handleCG.buildCG();
    ArrayList<IMethod> handleCallee = handleCG.getAllCallee(packageFilter, methodFilter);
    System.out.println("Overall handle: " + handleMethods.size() + " callee: " + handleCallee.size());


    //cnt signal/wait
    ArrayList<IMethod> allMethods = new ArrayList<IMethod>();
    allMethods.addAll(handleCallee);
    allMethods.addAll(rpcCallee);
    handleCG.cntSignalWait(allMethods);

    System.out.println("write callee to file.");
    handleCG.writeToFile(handleCallee, "../../src/com/hbase_callee_4539.txt_tmp", false);
    rpcCG.writeToFile(rpcCallee, "../../src/com/hbase_callee_4539.txt_tmp", true);

    AnalysisOptions options = new AnalysisOptions();
    options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
    AnalysisCache cache = new AnalysisCache();
    ArrayList<String> rpcCallsite = new ArrayList<String>();
    int cnt = 0;
    for (IClass c : classList) {
      for (IMethod m : c.getDeclaredMethods()) {
        if (m.isAbstract() || m.isBridge() || m.isClinit() ||
            m.isInit() || m.isNative() || m.isSynthetic()) { continue; }
        IR ir = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions());
        for (Iterator<CallSiteReference> iter = ir.iterateCallSites(); iter.hasNext(); ) {
          CallSiteReference isite = iter.next();
          for (RPCIface i : rpcTuple) {
            if (i.isRPCCall(isite)) {
              //System.out.println("call: " + isite.toString());
              //System.out.print("In m: " + m.getSignature() + " Line: ");
              String str = m.getSignature().substring(0, m.getSignature().lastIndexOf("."));
              if (str.indexOf("$") != -1) {
                str = str.substring(0, str.indexOf("$"));
              }
              String tmp = isite.toString();
              tmp = tmp.substring(tmp.indexOf("<"), tmp.indexOf(">"));
              String[] siteStr = tmp.split(",");
              String calledMethod = siteStr[2].trim();
              calledMethod = calledMethod.substring(0, calledMethod.indexOf("("));
              str += " " + calledMethod;
              com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(isite);
              for (com.ibm.wala.util.intset.IntIterator iterInt = indices.intIterator(); iterInt.hasNext(); ) {
                int iSrcLine = iterInt.next();
                //System.out.print(" " + m.getLineNumber(iSrcLine));
                try {
                  int bcIndex = ((IBytecodeMethod)m).getBytecodeIndex(iSrcLine);
                  str += " " + m.getLineNumber(bcIndex);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
              rpcCallsite.add(str);
              cnt++;
            }
          }
        }
      }
    }
    System.out.println("Overall rpc callsite: " + cnt);
    System.out.println("Write rpc callsite to file.");
    try {
      PrintWriter out = new PrintWriter("../../src/com/hbase_callsite_4539.txt_tmp", "UTF-8");
      out.println("//format: class iface method");
      for (String i : rpcCallsite) {
        out.println(i);
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class RPCIface {
  IClass iface;
  String ifaceStr;
  IClass cc;
  IMethod method;
  String methodSig;

  public void setIface(IClass i) {
    iface = i;
    ifaceStr = iface.getName().toString().trim();
  }
  public void setClass(IClass cc) {
    this.cc = cc;
  }
  public void setMethod(IMethod m) {
    method = m;
    methodSig = method.getSignature();
    methodSig = methodSig.substring(methodSig.lastIndexOf(".") + 1);
  }

  public boolean isRPCCall(CallSiteReference isite) {
    if (isite.isInterface() ==false) {
      return false;
    }
    String tmp = isite.toString();
    tmp = tmp.substring(tmp.indexOf("<"), tmp.indexOf(">"));
    String[] siteStr = tmp.split(",");
    //eg: invokeinterface < Application, Lorg/apache/hadoop/hbase/ipc/HRegionInterface, lockRow([B[B)J >@18
    String calledClass = siteStr[1].trim();
    String calledMethodSig = siteStr[2].trim();
    //System.out.println("Called cc1: " + calledClass);
    //System.out.println("Called cc2: " + ifaceStr);
    //System.out.println("Called method: " + methodSig);
    if (ifaceStr.equals(calledClass) == false) {
      return false;
    }
    //System.out.println("Here");

    //System.out.println("called mSig: " + methodSig);
    if (methodSig.equals(calledMethodSig)) {
      return true;
    }
    return false;
  }
}


