package sa.tc;

import java.util.*;
import java.io.*;
import java.util.jar.JarFile;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;

import sa.tc.Util.Config;

public class MapReduceSM {
  static AnalysisCache cache = new AnalysisCache();

  public static IR getIR(IMethod m) {
    AnalysisOptions options = new AnalysisOptions();
    options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
    return cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions());
  }

  public static ArrayList<SSAInvokeInstruction> targetInvoke (IR ir, String cc, String method) {
    ArrayList<SSAInvokeInstruction> rt = new ArrayList<SSAInvokeInstruction>();
    for (SSAInstruction si : ir.getInstructions()) {
      if (si instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction invokeI = (SSAInvokeInstruction)si;
        String invokeCC = invokeI.getDeclaredTarget().getDeclaringClass().getName().toString();
        String invokeM = invokeI.getDeclaredTarget().getName().toString();
        if (invokeCC.equals(cc) && invokeM.equals(method)) {
          rt.add(invokeI);
        }
      }
    }
    return rt;
  }

  public static void main(String[] args) throws Exception {
    Config config = new Config();
    config.config(args[0]);

    AnalysisScope scope = null;
    ClassHierarchy cha = null;
    for (String path : config.jarPaths) {
      if (scope == null) {
        scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(path, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
      }
      else {
        scope.addToScope(ClassLoaderReference.Application, new JarFile(path));
      }
    }
    cha = ClassHierarchy.make(scope);

    IClass targetCC = null;
    for (IClass c : cha) {
      if (c.getName().toString().endsWith("/TaskAttemptImpl")) {
        targetCC = c;
        System.out.println("cc: " + c.getName());
      }
    }


    ArrayList<IMethod> allMethods = new ArrayList<IMethod>();
    allMethods.addAll(targetCC.getAllMethods());
    allMethods.add(targetCC.getClassInitializer());
    ArrayList<String> smDefinePair = new ArrayList<String>(); //curState-eve
    for (IMethod m : allMethods) {
      IR ir = getIR(m);
      if (ir == null) { continue; }
      DefUse defUse = cache.getDefUse(ir);
      for (SSAInvokeInstruction invokeI : targetInvoke(ir, "Lorg/apache/hadoop/yarn/state/StateMachineFactory", "addTransition")) {
        assert(invokeI.getNumberOfParameters() >= 4);

        SSAInstruction curState = defUse.getDef(invokeI.getUse(1));
        //System.out.println("curS: " + getValue(curState));
        ArrayList<String> eveList = new ArrayList<String>();

        SSAInstruction eve = defUse.getDef(invokeI.getUse(3));
        if (eve instanceof SSAGetInstruction) { //signal event
          //System.out.println("eve: " + getValue(eve));
          eveList.add(getValue(eve));
        }
        else { //event set
          assert (eve instanceof SSAInvokeInstruction);
          SSAInvokeInstruction eveInvoke = (SSAInvokeInstruction)eve;
          for (int i=0; i < eveInvoke.getNumberOfParameters(); i++) {
            SSAInstruction ei = defUse.getDef(eveInvoke.getUse(i));
            if (ei instanceof SSAGetInstruction) {
              //System.out.println("eve: " + getValue(ei));
              eveList.add(getValue(ei));
            }
           else { //event is a array. Set = [e1, array]
             assert(ei instanceof SSANewInstruction);
             SSANewInstruction eiNew = (SSANewInstruction)ei;
             Iterator<SSAInstruction> it  = defUse.getUses(eiNew.getDef());
             while (it.hasNext()) {
               SSAInstruction useI = it.next();
               if (useI instanceof SSAArrayStoreInstruction) {
                 SSAInstruction addedEi = defUse.getDef(useI.getUse(2));
                 //System.out.println("eve: " + getValue(addedEi));
                 eveList.add(getValue(addedEi));
               }
             }
           }
          }
        }

        //System.out.println("Combine: ");
        for (String e : eveList) {
          smDefinePair.add(getValue(curState) + "@" + e);
        }
      }
    }

    BufferedReader read = new BufferedReader(new FileReader(args[1]));
    String idline = "";
    String eveline = "";
    int cnt = 0, benignCnt = 0;;
    while ((idline = read.readLine()) != null &&
            (eveline = read.readLine()) != null) {
      String[] op = eveline.split(" ");
      String e1 = op[0].split("-")[0];
      String s1 = op[0].split("-")[1];
      String e2 = op[1].split("-")[0];
      String s2 = op[1].split("-")[1];
      String case1 = s1 + "@" + e2;
      String case2 = s2 + "@" + e1;
      cnt++;
      if (smDefinePair.contains(case1) && smDefinePair.contains(case2)) { 
        //System.out.println("Benign: " + idline);
        System.out.println("Benign: " + eveline);
        benignCnt++;
      }
    }
    System.out.println("Overall #.Races = " + cnt + "; #.Benign = " + benignCnt);
  }

  public static String getValue(SSAInstruction i) {
    String rt = "";
    if (i instanceof SSAGetInstruction) {
      rt = ((SSAGetInstruction)i).getDeclaredField().getName().toString();
    }
    return rt;
  }
}

