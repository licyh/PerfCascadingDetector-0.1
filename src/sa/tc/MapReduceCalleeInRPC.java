package sa.tc;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
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

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;



public class MapReduceCalleeInRPC { 
  public static String format(String str) {
    String rt = str;
    if (rt.startsWith("L")) {
      rt = rt.substring(1);
    }
    rt = rt.replaceAll("/", ".");
    return rt;
  }

  public static boolean containMethod(IClass c, String method) {
    boolean rt = false;
    for (IMethod im : c.getDeclaredMethods()) {
      if (im.getName().toString().equals(method)) {
        rt = true;
      }
    }
    return rt;
  }

  public static boolean isHandleMethod(IMethod m) {
    IClass cc = m.getDeclaringClass();
    //check class
    if (cc.getName().toString().startsWith("Lorg/apache/") == false) {
      return false;
    }
    if (cc.isInterface()) {
      return false;
    }
    for (IClass icc : cc.getAllImplementedInterfaces()) {
      if (icc.getName().toString().endsWith("/EventHandler")) { //a subclass of eventHandler
        //check method
        if (m.getName().toString().equals("handle")) {
          if (m.getSignature().endsWith("/Event;)V") == false) { //remove interface method
           return true;
          }
        }
      }
    }
    return false;
  }

  public static boolean isRPCMethod(IMethod m) {
    //overall 60 rpc functions have 45 callees. No need to overengineer.
    return false;
  }

  public static boolean isHandleOrRPCMethod(IMethod m) {
    return isHandleMethod(m) || isRPCMethod(m);
  }

  public static ArrayList<IMethod> allHandleMethod(ArrayList<IClass> classList) {
    ArrayList<IMethod> rt = new ArrayList<IMethod>();
    for (IClass c : classList) {
      for (IMethod m : c.getDeclaredMethods()) {
        if (MapReduceCalleeInRPC.isHandleMethod(m)) {
          rt.add(m);
        }
      }
    }
    return rt;
  }


  public static ArrayList<String> getRPCClassStr() {
    //read from rpc list file
    String v2Path = "../../src/com/mr_rpc.txt";
    String v1Path = "../../src/com/mr_rpc_v1.txt";
    ArrayList<String> classStr = new ArrayList<String>();
    BufferedReader in = null;
    int cnt = 0;
    try {
      in = new BufferedReader(new FileReader(v2Path));
      String line = null;
      while ((line = in.readLine()) != null) {
        if (line.startsWith("//")) { continue; }
        String[] words = line.split(" ");
        if (classStr.contains(words[0]) == false) {
          classStr.add(words[0]);
        }
      }
      in.close();

      in = new BufferedReader(new FileReader(v1Path));
      while ((line = in.readLine()) != null) {
        if (line.startsWith("//")) { continue; }
        String[] words = line.split(" ");
        if (classStr.contains(words[0]) == false) {
          classStr.add(words[0]);
        }
      }
      in.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return classStr;
  }

  public static ArrayList<String> getRPCMethodStr() {
    //read from rpc list file
    String v2Path = "../../src/com/mr_rpc.txt";
    String v1Path = "../../src/com/mr_rpc_v1.txt";
    ArrayList<String> methodStr = new ArrayList<String>();
    BufferedReader in = null;
    int cnt = 0;
    try {
      in = new BufferedReader(new FileReader(v2Path));
      String line = null;
      while ((line = in.readLine()) != null) {
        if (line.startsWith("//")) { continue; }
        String[] words = line.split(" ");
        methodStr.add(words[0] + "." + words[2]);
        cnt++;
        //System.out.println("RPC M: " + words[0] + "." + words[2]);
      }
      in.close();

      in = new BufferedReader(new FileReader(v1Path));
      while ((line = in.readLine()) != null) {
        if (line.startsWith("//")) { continue; }
        String[] words = line.split(" ");
        methodStr.add(words[0] + "." + words[2]);
        cnt++;
        //System.out.println("RPC M: " + words[0] + "." + words[2]);
      }
      in.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    //System.out.println("Read " + cnt + " RPC methods.");
    return methodStr;
  }

  public static ArrayList<IMethod> allRPCMethod(ArrayList<IClass> classList, ArrayList<String> classStr, ArrayList<String> methodStr) {
    ArrayList<IMethod> rt = new ArrayList<IMethod>();
    int cnt = 0;
    for (IClass c : classList) {
      String cName = MapReduceCalleeInRPC.format(c.getName().toString());
      if (classStr.contains(cName)) {
        for (IMethod m : c.getDeclaredMethods()) {
          String mName = m.getName().toString();
          String fullName = cName + "." + mName;
          if (mName.equals("refreshServiceAcls") &&
              m.getSignature().endsWith(")V")) {
            continue; //rcp function and normal function have same name.
          }
          if (methodStr.contains(fullName)) {
            rt.add(m);
            cnt++;
            //System.out.println("Find RPC: " + fullName);
          }
        }
      }
    }
    //System.out.println("Find " + cnt + " RPC methods.");
    return rt;
  }


  public static void main(String[] args) {
    String path = "/mnt/storage/haopeng/workstation/java-ws/DC-Detector/mr_test/hadoop-0.23.1-install/share/hadoop/";
    ArrayList<String> pathList = new ArrayList<String>();
    pathList.add(path + "common");
    pathList.add(path + "mapreduce");

    AnalysisScope scope = null;;
    ClassHierarchy cha = null;

    ArrayList<IClass> classList = new ArrayList<IClass>();
    try {
      for (String pathi : pathList) {
        File jarDIR = new File(pathi);
        for (File f : jarDIR.listFiles()) {
          if (f.isFile()) {
            if (f.getName().contains("test")) { continue; }
            if (scope == null) {
              scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(f.getAbsolutePath(), (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
            }
            else {
              scope.addToScope(ClassLoaderReference.Application, new JarFile(f.getAbsolutePath()));
            }
            System.out.println(f.getName());
          }
        }
      }
      cha = ClassHierarchy.make(scope);
      for (IClass c : cha) {
        if (c.getName().toString().startsWith("Lorg/apache/") == false) { continue; }
        classList.add(c);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //get all handle method
    ArrayList<IMethod> handleMethods = MapReduceCalleeInRPC.allHandleMethod(classList);

    //get all rpcMethod
    ArrayList<String> rpcClassStr = MapReduceCalleeInRPC.getRPCClassStr();
    ArrayList<String> rpcMethodStr = MapReduceCalleeInRPC.getRPCMethodStr();
    ArrayList<IMethod> rpcMethods = MapReduceCalleeInRPC.allRPCMethod(classList, rpcClassStr, rpcMethodStr);
    //get other special rpc method, like heartbeat in NM
    ArrayList<String> otherClassStr = new ArrayList<String>();
    otherClassStr.add("org.apache.hadoop.yarn.server.nodemanager.containermanager.localizer.ResourceLocalizationService");
    ArrayList<String> otherMethodStr = new ArrayList<String>();
    otherMethodStr.add("heartbeat");
    ArrayList<IMethod> otherRPCMethods = new ArrayList<IMethod>();
    for (IClass c : classList) {
      if (otherClassStr.contains(MapReduceCalleeInRPC.format(c.getName().toString()))) {
        for (IMethod m : c.getDeclaredMethods()) {
          if (otherMethodStr.contains(m.getName().toString())) {
            otherRPCMethods.add(m);
            System.out.println("ASDASDASDASD");
          }
        }
      }
    }

    //package filter: only consider class in these packages.
    ArrayList<String> packageFilter = new ArrayList<String>();
    packageFilter.add("org.apache.hadoop.yarn.");
    packageFilter.add("org.apache.hadoop.mapred.");
    packageFilter.add("org.apache.hadoop.mapreduce.");
    packageFilter.add("org.apache.hadoop.ipc.");
    packageFilter.add("org.apache.hadoop.util.RunJar");

    //method filter: exclude these methods.
    ArrayList<IMethod> methodFilter = new ArrayList<IMethod>();
    for (IMethod m : handleMethods) { methodFilter.add(m); }
    for (IMethod m : rpcMethods) { methodFilter.add(m); }

    System.out.println("Start Handle CG:");
    CalleeCG handleCG = new CalleeCG(scope, cha);
    handleCG.setEntryMethods(handleMethods);
    handleCG.buildCG();
    ArrayList<IMethod> handleCallee = handleCG.getAllCallee(packageFilter, methodFilter);
    /*for (IMethod m : handleCallee) {
      System.out.println("callee: " + m.getSignature());
    }*/
    System.out.println("Overall handle: " + handleMethods.size() + " callee: " + handleCallee.size());

    System.out.println("Start RPC CG:");
    CalleeCG rpcCG = new CalleeCG(scope, cha);
    rpcCG.setEntryMethods(rpcMethods);
    rpcCG.buildCG();
    ArrayList<IMethod> rpcCallee = rpcCG.getAllCallee(packageFilter, methodFilter);
    /*for (IMethod m : rpcCallee) {
      System.out.println("callee: " + m.getSignature());
    }*/
    System.out.println("Overall callee: " + rpcMethods.size() + " callee: " + rpcCallee.size());

    System.out.println("Start other RPC CG: ");
    CalleeCG otherCG = new CalleeCG(scope, cha);
    otherCG.setEntryMethods(otherRPCMethods);
    otherCG.buildCG();
    ArrayList<IMethod> otherCallee = otherCG.getAllCallee(packageFilter, methodFilter);
    System.out.println("Overall callee: " + otherRPCMethods.size() + " callee " + otherCallee.size());

    //cnt signal/wait
    ArrayList<IMethod> allMethods = new ArrayList<IMethod>();
    allMethods.addAll(handleMethods);
    allMethods.addAll(handleCallee);
    allMethods.addAll(rpcMethods);
    allMethods.addAll(rpcCallee);
    rpcCG.cntSignalWait(allMethods);

    System.out.println("Write callee to file.");
    String filePath = "../../src/com/mr_callee.txt_tmp";
    try {
      PrintWriter out = new PrintWriter(filePath, "UTF-8");
      out.println("//format: class method sign");
      for (IMethod m : handleCallee) {
        String cName = MapReduceCalleeInRPC.format(m.getDeclaringClass().getName().toString());
        String mName = m.getName().toString();
        out.println(cName + " " + mName + " " + m.getDescriptor().toString());
      }
      for (IMethod m : rpcCallee) {
        String cName = MapReduceCalleeInRPC.format(m.getDeclaringClass().getName().toString());
        String mName = m.getName().toString();
        out.println(cName + " " + mName + " " + m.getDescriptor().toString());
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

