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


import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

import com.sa.CalleeCG;

public class ZKCallee {
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

  public static boolean containsType(ArrayList<IClass> cc, String type) {
    for (IClass c : cc) {
      if (c.getName().toString().equals(type)) {
        return true;
      }
    }
    return false;
  }

  public static void main(String[] args) {
    if (args.length != 1 ||
        (args[0].equals("1270") == false && args[0].equals("1144") == false)) {
      System.out.println("Choose zk version 1270 or 1144. Exit.");
      System.exit(-1);
    }
    String versionId = args[0];
    String basePath = "/mnt/storage/haopeng/workstation/java-ws/DC-Detector/zk_test/zk-" + versionId + "/zookeeper/trunk/build/";
    String path = versionId.equals("1270") ? basePath + "zookeeper-3.5.0.jar" : basePath + "zookeeper-3.4.0.jar";

    AnalysisScope scope = null;
    ClassHierarchy cha = null;

    ArrayList<IClass> classList = new ArrayList<IClass>();
    ArrayList<IClass> recordCC = new ArrayList<IClass>();
    ArrayList<IClass> requestCC = new ArrayList<IClass>();
    ArrayList<IClass> outCC = new ArrayList<IClass>();
    ArrayList<IClass> inCC = new ArrayList<IClass>();

    try {
      scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(path, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
      cha = ClassHierarchy.make(scope);

      ArrayList<String> classFamily = new ArrayList<String>();
      for (IClass c : cha) {
        String cName = c.getName().toString();
        // exclude undesired class
        if (cName.startsWith("Lorg/apache/zookeeper") == false &&
            cName.startsWith("Lorg/apache/jute") == false) {
          continue;
        }
        classList.add(c);

        classFamily.clear();
        //check class itself
        classFamily.add(cName);

        //check class's interface
        for (IClass cc : c.getAllImplementedInterfaces()) {
          classFamily.add(cc.getName().toString());
        }

        //check superclass.
        IClass superCC = c.getSuperclass();
        classFamily.add(superCC.getName().toString());

        //check superclass's interface
        for (IClass cc : superCC.getAllImplementedInterfaces()) {
          classFamily.add(cc.getName().toString());
        }

        for (String ciName : classFamily) {
          if (ciName.endsWith("/Record") && recordCC.contains(c) == false) {
            recordCC.add(c);
          }
          else if (ciName.endsWith("/Request") && requestCC.contains(c) == false) {
            requestCC.add(c);
          }
          else if (ciName.endsWith("/InputArchive") && inCC.contains(c) == false) {
            inCC.add(c);
          }
          else if (ciName.endsWith("/OutputArchive") && outCC.contains(c) == false) {
            outCC.add(c);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    MethodIR methodIR = new MethodIR();

    ArrayList<IMethod> recordMethod = new ArrayList<IMethod>();
    for (IClass c : classList) {
      for (IMethod m : c.getDeclaredMethods()) {
        if (m.isNative()) continue;
        for (SSANewInstruction i : methodIR.getSSANewInst(m)) {
          String type = i.getConcreteType().getName().toString();
          if (ZKCallee.containsType(recordCC, type)) {
            //System.out.println("CC: " + c.getName().toString() + ": " + m.getName().toString());
            //System.out.println(" --> " + i.getConcreteType().getName().toString());
            recordMethod.add(m);
            continue;
          }
        }
      }
    }

    ArrayList<IMethod> requestMethod = new ArrayList<IMethod>();
    for (IClass c : classList) {
      for (IMethod m : c.getDeclaredMethods()) {
        if (m.isNative()) continue;
        if (m.getName().toString().equals("main")) continue;
        if (m.getNumberOfParameters() == 2) {
          String paraType = m.getParameterType(1).getName().toString();
          if (ZKCallee.containsType(requestCC, paraType)) {
            //System.out.println("Sig: " + m.getSignature());
            requestMethod.add(m);
          }
        }
      }
    }

    System.out.println("Output for auto add msg id.");
    try {
      String outFile = "../../src/com/zk_class_" + versionId + ".txt_tmp";
      PrintWriter out = new PrintWriter(outFile, "UTF-8");
      out.println("Record interface: ");
      for (IClass c : recordCC) {
        if (c.isInterface()) {
          out.println(ZKCallee.format(c.getName().toString()));
        }
      }
      out.println("Record class: ");
      for (IClass c : recordCC) {
        if (c.isInterface() == false) {
          out.println(ZKCallee.format(c.getName().toString()));
        }
      }

      out.println("Inarchive interface: ");
      for (IClass c : inCC) {
        if (c.isInterface()) {
          out.println(ZKCallee.format(c.getName().toString()));
        }
      }
      out.println("Inarchive class: ");
      for (IClass c : inCC) {
        if (c.isInterface() == false) {
          out.println(ZKCallee.format(c.getName().toString()));
        }
      }

      out.println("Outarchive interface: ");
      for (IClass c : outCC) {
        if (c.isInterface()) {
          out.println(ZKCallee.format(c.getName().toString()));
        }
      }
      out.println("Outarchive class: ");
      for (IClass c : outCC) {
        if (c.isInterface() == false) {
          out.println(ZKCallee.format(c.getName().toString()));
        }
      }

      out.println("Request: ");
      for (IClass c : requestCC) {
        out.println(ZKCallee.format(c.getName().toString()));
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("Overall request method: " + requestMethod.size() + " ; record method: " + recordMethod.size());

    if (versionId.equals("1144")) {
      for (IClass c : classList) {
        for (IMethod m : c.getDeclaredMethods()) {
          if (c.getName().toString().endsWith("QuorumPeer") &&
              m.getName().toString().equals("<init>") &&
              m.getSignature().endsWith("/ServerCnxnFactory;)V")) {
            System.out.println("Debug... c: " + c.getName() + " m: " + m.getName() + " sig: " + m.getSignature());
            recordMethod.add(m);
          }
          else if (c.getName().toString().endsWith("QuorumPeer") &&
                   m.getName().toString().equals("makeLeader")) {
            System.out.println("Debug... c: " + c.getName() + " m: " + m.getName() + " sig: " + m.getSignature());
            recordMethod.add(m);
          }
        }
      }
    }

    //package filter: only conside class in these packages
    ArrayList<String> packageFilter = new ArrayList<String>();
    packageFilter.add("org.apache.zookeeper.");
    packageFilter.add("org.apache.jute.");

    //method filter: exclude these methods
    ArrayList<IMethod> methodFilter = new ArrayList<IMethod>();
    for (IMethod m : recordMethod) { methodFilter.add(m); }
    for (IMethod m : requestMethod) { methodFilter.add(m); }

    CalleeCG calleeCG = new CalleeCG(scope, cha);
    calleeCG.setEntryMethods(methodFilter);
    //calleeCG.setClassList(classList);
    calleeCG.buildCG();
    ArrayList<IMethod> callee = calleeCG.getAllCallee(packageFilter, methodFilter);

    /* add to count notify/wait
     */
    ArrayList<IMethod> allMethods = new ArrayList<IMethod>();
    allMethods.addAll(recordMethod);
    allMethods.addAll(requestMethod);
    allMethods.addAll(callee);
    calleeCG.cntSignalWait(allMethods);
    /* end of add
     */

    System.out.println("Overall callee: " + callee.size());

    String file = "../../src/com/zk_callee_" + versionId +".txt_tmp";
    System.out.println("write record to file.");
    calleeCG.writeToFile(recordMethod, file, false);

    System.out.println("write request to file.");
    calleeCG.writeToFile(requestMethod, file, true);

    System.out.println("write callee to file.");
    calleeCG.writeToFile(callee, file, true);

    /*ArrayList<String> rpcList = new ArrayList<String>();
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
                //rpcList.add(HBaseRPC.format(c.getName().toString()) + " " + HBaseRPC.format(cc.getName().toString())
                //             + " " + m.getName().toString());
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
      PrintWriter out = new PrintWriter("../../src/com/hbase_rpc_4539.txt", "UTF-8");
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

    System.out.println("write callee to file.");
    handleCG.writeToFile(handleCallee, "../../src/com/hbase_callee_4539.txt", false);
    rpcCG.writeToFile(rpcCallee, "../../src/com/hbase_callee_4539.txt", true);

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
                str += " " + m.getLineNumber(iSrcLine);
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
      PrintWriter out = new PrintWriter("../../src/com/hbase_callsite_4539.txt", "UTF-8");
      out.println("//format: class iface method");
      for (String i : rpcCallsite) {
        out.println(i);
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }*/
  }
}

class MethodIR {
  AnalysisOptions options;
  AnalysisCache cache;
  IMethod method;
  IR ir;

  public MethodIR() {
    options = new AnalysisOptions();
    options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
    cache = new AnalysisCache();
  }

  public void setMethod(IMethod m) {
    method = m;
    ir = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions());
  }

  public IR getIR(IMethod m) {
    setMethod(m);
    return ir;
  }

  public ArrayList<SSAInstruction> getSSAInst(IMethod m) {
    ArrayList<SSAInstruction> inst = new ArrayList<SSAInstruction>();
    IR mIR = getIR(m);
    if (mIR != null) {
      Iterator ii = mIR.iterateAllInstructions();
      while(ii.hasNext()) {
        inst.add((SSAInstruction)ii.next());
      }
    }
    return inst;
  }

  public ArrayList<SSANewInstruction> getSSANewInst(IMethod m) {
    ArrayList<SSANewInstruction> inst = new ArrayList<SSANewInstruction>();
    for (SSAInstruction i : getSSAInst(m)) {
      if (i instanceof SSANewInstruction) {
        inst.add((SSANewInstruction)i);
      }
    }
    return inst;
  }
}

  
