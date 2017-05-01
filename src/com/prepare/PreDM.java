package com.prepare;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;

import spoon.Launcher;
import spoon.processing.ProcessingManager;
import spoon.reflect.factory.Factory;
import spoon.support.QueueProcessingManager;

import com.prepare.Util.PrepareUtil;

import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.types.ClassLoaderReference;

import java.util.*;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarFile;
import com.prepare.Util.PrepareUtil;

import com.prepare.Modify.CCModifierWrap;
import com.prepare.Modify.IfaceModifierWrap;
import com.prepare.Modify.template.MRCCTemplate;
import com.prepare.Modify.checker.CCChecker;
import com.prepare.Modify.checker.IfaceChecker;

import com.prepare.Modify.MethodModifierWrap;
import com.prepare.Modify.template.MRMethodTemplate;
import com.prepare.Modify.checker.MethodChecker;

import com.prepare.Modify.template.InvokeTemplate;
import com.prepare.Modify.InvokeModifierWrap;
import com.prepare.Modify.template.MRInvokeTemplate;
import com.prepare.Modify.checker.InvokeChecker;

import com.prepare.Modify.template.ZKCCTemplate;
import com.prepare.Modify.ZKMethodModifierWrap;

/* task1: get all rpc (wala)
 * task2: get all callee of rpc & event handler (wala)
 * task3: add msg id for each socket/rpc (spoon)
 */

public class PreDM {
	
    Path inDirPath;
    String inClasspathStr;
    Path outDirPath;
    Path spoonedPath = Paths.get(System.getProperty("user.dir"), "spooned");
    
	
	public static void main(String[] args) throws IOException {
		new PreDM().doWork(args);
	}
	
  public void doWork(String[] args) throws IOException {
    /*Launcher launcher = new Launcher();
    launcher.setArgs(args);
    launcher.addInputResource(args[1]);

    final Factory factory = launcher.getFactory();
    final ProcessingManager processingManager = new QueueProcessingManager(factory);
    final AllCCProc allCCProc = new AllCCProc();
    launcher.addProcessor(allCCProc);
    processingManager.addProcessor(allCCProc);
    processingManager.process(factory.Class().getAll());
    launcher.run();*/

    //init config
    Config config = new Config(args[0]);
    // jx
    inDirPath = Paths.get( args[1] );
    inClasspathStr = args[2];
    outDirPath = Paths.get( args[3] );

    AnalysisScope scope = null;
    ClassHierarchy cha = null;
    ArrayList<IClass> allCC = null;
    try {
      for (String file : config.jarPath) {
        if (scope == null) {
          scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(file, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
        }
        else {
          scope.addToScope(ClassLoaderReference.Application, new JarFile(file));
        }
      }
      cha = ClassHierarchy.make(scope);
      allCC = PrepareUtil.getAllCC(cha, config.packageFilter, config.packageExclude);
    } catch (Exception e) {
      e.printStackTrace();
    }


    //for modifier
    ArrayList<String> qualifiedRPCCCList = new ArrayList<String>();
    ArrayList<String> qualifiedRPCMethodList = new ArrayList<String>();


    //rpc
    FindMethod rpcFinder = new FindMethod(config, allCC);
    ArrayList<IMethod> rpcMethods = null;
    if (config.rpcIdentWay.equals("CCMethod")) {
      rpcFinder.setCCMethod(config.rpcIdentWay, config.rpcCCKeyWord, config.rpcMethodKeyWord);
      rpcMethods = rpcFinder.getAllKeyMethod();
      PrepareUtil.writeToFile(config.rpcOutPath, rpcFinder.getAllKeyStr(), false);
      rpcFinder.getIdentifiedCCMethod(qualifiedRPCMethodList); //for modifier
    }
    else if (config.rpcIdentWay.equals("Para")) {
      rpcFinder.setPara(config.rpcIdentWay, config.rpcParaKeyWord);
      rpcMethods = rpcFinder.getAllKeyMethod();
      PrepareUtil.writeToFile(config.rpcOutPath, rpcFinder.getAllKeyStr(), false);
      rpcFinder.getIdentifiedCC(qualifiedRPCCCList); //for modifier
    }
    else if (config.rpcIdentWay.equals("CCMethod|Para")) {
      rpcFinder.setCCMethod("CCMethod", config.rpcCCKeyWord, config.rpcMethodKeyWord);
      rpcMethods = rpcFinder.getAllKeyMethod();
      PrepareUtil.writeToFile(config.rpcOutPath, rpcFinder.getAllKeyStr(), false);
      rpcFinder.getIdentifiedCCMethod(qualifiedRPCMethodList); //for modifier

      FindMethod rpcFinder2 = new FindMethod(config, allCC);
      rpcFinder2.setPara("Para", config.rpcParaKeyWord);
      rpcMethods.addAll(rpcFinder2.getAllKeyMethod());
      PrepareUtil.writeToFile(config.rpcOutPath, rpcFinder2.getAllKeyStr(), true);
      rpcFinder2.getIdentifiedCC(qualifiedRPCCCList); //for modifier

    }
    else if (config.rpcIdentWay.equals("SpecInst")) {
      rpcFinder.setSpecInst(config.rpcIdentWay, config.rpcSpecInstKeyWord);
      rpcMethods = rpcFinder.getAllKeyMethod();
      PrepareUtil.writeToFile(config.rpcOutPath, rpcFinder.getAllKeyStr(), false);
      rpcFinder.getIdentifiedCC(qualifiedRPCCCList); //for modifier
    }

    //if (config.bugID.equals("CA-1011")) {
    if (config.bugID.startsWith("CA-")) {
      for (IClass c : allCC) {
        for (IMethod m : c.getDeclaredMethods()) {
          //if (c.getName().toString().startsWith("Lorg/apache/cassandra/service/AntiEntropyService")) {
          /*if (c.getName().toString().contains("AntiEntropyService$RepairSession") ||
              c.getName().toString().endsWith("AntiEntropyService")) {
            if (c.getName().toString().contains("RepairJob")) {
              if (m.getName().toString().equals("sendTreeRequests")) {
                 rpcMethods.add(m);
                 continue;
              }
              if (m.getName().toString().equals("<init>") || m.getName().toString().equals("<cinit>")) {
                rpcMethods.add(m);
                continue;
              }
            }
            System.out.println("M: " + c.getName().toString() + "::" + m.getName().toString());
            rpcMethods.add(m);
            continue;
          }
          else if (c.getName().toString().endsWith("StorageService") || c.getName().toString().endsWith("NodeProbe")) {
            System.out.println("M: " + c.getName().toString() + "::" + m.getName().toString());
            rpcMethods.add(m);
            continue;
          }*/
          if (m.getName().toString().equals("initClient") && c.getName().toString().endsWith("StorageService"))
            rpcMethods.add(m);
        }
      }
    }
    else if (config.bugID.equals("ZK-1144")) {
      for (IClass c : allCC) {
        if (c.getName().toString().endsWith("QuorumPeer")) {
          for (IMethod m : c.getDeclaredMethods()) {
            if (m.getName().toString().equals("makeLeader") || m.getName().toString().equals("<init>")) {
              rpcMethods.add(m);
            }
          }
        }
      }
    }


    //event
    ArrayList<IMethod> eveMethods = null;
    FindMethod eveFinder = new FindMethod(config, allCC);
    if (config.eveIdentWay.equals("CCMethod")) {
      eveFinder.setCCMethod(config.eveIdentWay, config.eveCCKeyWord, config.eveMethodKeyWord);
      eveMethods = eveFinder.getAllKeyMethod();
      PrepareUtil.writeToFile(config.eveOutPath, eveFinder.getAllKeyStr(), false);
    }
    else if (config.eveIdentWay.equals("Para")) {
      eveFinder.setPara(config.eveIdentWay, config.eveParaKeyWord);
      eveMethods = eveFinder.getAllKeyMethod();
      PrepareUtil.writeToFile(config.eveOutPath, eveFinder.getAllKeyStr(), false);
    }
    else if (config.eveIdentWay.equals("SpecInst")) {
      eveFinder.setSpecInst(config.eveIdentWay, config.eveSpecInstKeyWord);
      eveMethods = eveFinder.getAllKeyMethod();
      PrepareUtil.writeToFile(config.eveOutPath, eveFinder.getAllKeyStr(), false);
    }
    if (config.bugID.equals("HB-4539")) {
      for (IClass c : allCC)
        for (IMethod m : c.getDeclaredMethods())
          if (m.getName().toString().equals("getZooKeeper")) eveMethods.add(m);
    }

    //callee
    eveMethods.addAll(rpcMethods);
    Callee callee = new Callee(scope, cha, config);
    callee.setEntryMethods(eveMethods);
    callee.setMethodFilter(eveMethods);
    //callee.updateEntry();
    PrepareUtil.writeToFile(config.calleeOutPath, callee.toStrArray(), false);

    if (config.bugID.startsWith("ZK")) {
      PrepareUtil.writeToFile(config.calleeOutPath, callee.entryMethodToStrArray(), true);
    }

    //find loop
    /* by JX
    FindLoop loopFinder = new FindLoop(config, allCC);
    PrepareUtil.writeToFile(config.calleeOutPath, loopFinder.searchToString(scope, cha), true);
	*/

    //rpc callee
    /*Callee hbaseRPCCallee = new Callee(scope, cha, config);
    hbaseRPCCallee.setEntryMethods(rpcMethods);
    hbaseRPCCallee.setMethodFilter(rpcMethods);
    PrepareUtil.writeToFile(config.calleeOutPath, hbaseRPCCallee.toStrArray(), true);*/

    //change source code only for mr, zk
    if (config.bugID.startsWith("CA") || config.bugID.startsWith("HB")) return;
    if (config.bugID.startsWith("ZK")) {
      qualifiedRPCCCList.clear();
      qualifiedRPCMethodList.clear();
      for (IClass c : PrepareUtil.getCCByName(allCC, config.rpcSpecInstKeyWord)) {
        qualifiedRPCCCList.add(PrepareUtil.typeToPack(c.getName().toString()));
        if (c.isInterface() == false && c.isAbstract() == false) {
          for (IMethod m : c.getDeclaredMethods()) {
            if (m.getName().toString().equals("serialize") || m.getName().toString().equals("deserialize")) {
              qualifiedRPCMethodList.add(PrepareUtil.typeToPack(c.getName().toString()) + "::" + m.getName());
            }
          }
        }
      }
    }

    //change source code
    String inputDir = inDirPath.toString();

    Filewalker walker = new Filewalker();
    ArrayList<String> files = new ArrayList<String>();
    walker.walk(inputDir, files);
    for (String str : files) {
      //if (str.endsWith("hadoop-yarn-server-resourcemanager/src/main/java") == false) continue;

        
    	
      Launcher launcher = new Launcher();

      final Factory factory = launcher.getFactory();

      final MRCCTemplate mrCCTemp = new MRCCTemplate(factory);
      //for (String i : qualifiedRPCCCList) { System.out.println("cc: " + i); }
      //qualifiedList.add("org.apache.hadoop.mapred.TaskAttemptListenerImpl");
      final CCChecker mrCCChecker = new CCChecker(qualifiedRPCCCList);
      final CCModifierWrap mrCCMod = new CCModifierWrap(mrCCTemp, mrCCChecker);
      final IfaceChecker mrIfaceChecker = new IfaceChecker(qualifiedRPCCCList);
      final IfaceModifierWrap mrIfaceMod = new IfaceModifierWrap(mrCCTemp, mrIfaceChecker);

      final MRMethodTemplate mrMethodTemp = new MRMethodTemplate(factory, false); //true: insert at beginning. false: at end
      final MethodChecker mrMethodChecker = new MethodChecker(qualifiedRPCMethodList);
      final MethodModifierWrap mrMethodMod = new MethodModifierWrap(mrMethodTemp, mrMethodChecker);

      final MRInvokeTemplate mrInvokeTemp = new MRInvokeTemplate(factory, InvokeTemplate.MODE.INSERT_BEFORE);
      final InvokeChecker mrInvokeChecker = new InvokeChecker(qualifiedRPCMethodList, qualifiedRPCCCList);
      final InvokeModifierWrap mrInvokeMod = new InvokeModifierWrap(mrInvokeTemp, mrInvokeChecker);

      final MRInvokeTemplate mrInvokeTempV1 = new MRInvokeTemplate(factory, InvokeTemplate.MODE.REPLACE);
      final InvokeModifierWrap mrInvokeModV1 = new InvokeModifierWrap(mrInvokeTempV1, mrInvokeChecker);

      final ZKCCTemplate zkCCTemp = new ZKCCTemplate(factory);
      final CCChecker zkCCChecker = new CCChecker(qualifiedRPCCCList);
      final CCModifierWrap zkCCMod = new CCModifierWrap(zkCCTemp, zkCCChecker);
      final IfaceChecker zkIfaceChecker = new IfaceChecker(qualifiedRPCCCList);
      final IfaceModifierWrap zkIfaceMod = new IfaceModifierWrap(zkCCTemp, zkIfaceChecker);

      final MethodChecker zkMethodChecker = new MethodChecker(qualifiedRPCMethodList);
      final ZKMethodModifierWrap zkMethodMod = new ZKMethodModifierWrap(zkMethodChecker, factory);

      //final ProcessingManager processingManager = new QueueProcessingManager(factory);
      //processingManager.addProcessor(ccMod);
      //processingManager.process(factory.Class().getAll());
      if (config.bugID.startsWith("MR")) {
        launcher.addProcessor(mrIfaceMod);
        launcher.addProcessor(mrCCMod);
        launcher.addProcessor(mrMethodMod);
        launcher.addProcessor(mrInvokeMod);
        launcher.addProcessor(mrInvokeModV1);
      }
      else if (config.bugID.startsWith("ZK")) {
        launcher.addProcessor(zkIfaceMod);
        launcher.addProcessor(zkCCMod);
        launcher.addProcessor(zkMethodMod);
      }


      System.out.println("Process file: " + str);
      
      String[] spoonArgs = new String[] {
      		"-i", str,
      		"--source-classpath", inClasspathStr,
      		"--output-type", "compilationunits",
      		"--level", "WARN",
      		"--no-copy-resources",
      };

      launcher.setArgs(spoonArgs);
      launcher.run();
      
      // jx: copyback
      copyback(str);
    }
  }
  
  public void copyback(String str) throws IOException {
	  Path srcpath = Paths.get(str);
	  Path relative = inDirPath.relativize(srcpath);
	  
	  final Path dstpath = outDirPath.resolve(relative);
	  System.out.println("srcpath: " + srcpath);
	  System.out.println("spoonedPath: " + spoonedPath);
	  System.out.println("dstpath: " + dstpath);
	  
	  // copy from "spooned" to "dstpath"
	  // traverse "spooned"
	  Files.walkFileTree( spoonedPath, new SimpleFileVisitor<Path>(){
          @Override 
          public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
        	  if ( !filepath.getFileName().toString().endsWith(".java") ) {
        		  System.out.println("JX - ERROR - filepath didn't end withs .java");
        		  return FileVisitResult.CONTINUE;
        	  }
        	  Files.copy(filepath, dstpath.resolve(spoonedPath.relativize(filepath)), StandardCopyOption.REPLACE_EXISTING);
              return FileVisitResult.CONTINUE;
          }
          
      });
	  
	  // delete "spooned" or all subdirs and subfiles of "spooned"
	  Files.walkFileTree( spoonedPath, new SimpleFileVisitor<Path>(){
          @Override 
          public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
        	  Files.delete(filepath);
              return FileVisitResult.CONTINUE;
          }
      }); //xx   x
	  
  }
    
}



class Filewalker {
  public void walk (String path, ArrayList<String> files) {
    File root = new File(path);
    File[] list = root.listFiles();
    if (list == null) return;

    for (File f : list) {
      if (f.isDirectory()) {
        if (f.getName().equals("test") ||
            f.getName().equals("Test") ||
            f.getName().contains("examples") ||
            f.getName().contains("generated") ||
            f.getName().equals("benchmarks") ||
            f.getName().equals("contrib") ||
            //f.getName().equals("hadoop-mapreduce-client-core") ||
            f.getName().equals("hadoop-yarn-common") ||
            f.getName().equals("hadoop-common") ||
            f.getName().equals("hadoop-common-project") ||
            f.getName().equals("hadoop-maven-plugins") ||
            f.getName().equals("hadoop-minikdc") ||
            f.getName().equals("hadoop-yarn-applications-distributedshell") ||
            f.getName().equals("hadoop-mapreduce-client-hs") ||
            f.getName().equals("webapp")) continue;
        if (f.getName().equals("java")) {
          files.add(f.getAbsolutePath());
        }
        else {
          walk(f.getAbsolutePath(), files);
        }
      }
    }
  }
}
