package com.sa;

import java.io.*;
import java.util.*;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.CallSiteReference;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.types.MethodReference;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;

import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;

public class CalleeCG {
  AnalysisScope scope;
  IClassHierarchy cha;
  ArrayList<IMethod> entryMethods;
  Iterable<Entrypoint> entryPoints;
  CallGraph cg;

  ArrayList<IClass> classList; //used for empty callsite to find iface and its method.
  //This list should set seperately. Currently this is not used.

  public CalleeCG(AnalysisScope scope_, ClassHierarchy cha_) {
    scope = scope_;
    cha = cha_;
    classList = null;
  }
  public void setEntryMethods(ArrayList<IMethod> mList) {
    entryMethods = mList;
    entryPoints = makeEntrypoints();
  }

  public Iterable<Entrypoint> makeEntrypoints() {
    final HashSet<Entrypoint> result = HashSetFactory.make();
    for (IMethod m : entryMethods) {
      result.add(new DefaultEntrypoint(m, cha));
    }
    return new Iterable<Entrypoint>() {
      public Iterator<Entrypoint> iterator() {
        return result.iterator();
      }
    };
  }

  public void setClassList(ArrayList<IClass> list) { classList = list; }

  public void buildCG() {
    AnalysisOptions options = new AnalysisOptions(scope, entryPoints);
    com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope);
    try {
      cg = builder.makeCallGraph(options, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ArrayList<IMethod> getTargets(IMethod m, SSAInvokeInstruction invoke) {
    Set<CGNode> nodes = cg.getNodes(m.getReference());
    if (nodes.size() == 0) {
      System.out.println("Cannot method's cgnode. Exit...");
      System.exit(-1);
    }

    if (nodes.size() > 1) {
      System.out.println("Find more than one cgnodes. Exit...");
      System.exit(-1);
    }


    ArrayList<IMethod> rt = new ArrayList<IMethod>();
    for (CGNode called : cg.getPossibleTargets(nodes.iterator().next(), invoke.getCallSite())) {
      if (rt.contains(called.getMethod()) == false) {
        rt.add(called.getMethod());
      }
    }
    return rt;
  }

  public ArrayList<IMethod> getAllCallee(ArrayList<String> packageFilter, ArrayList<IMethod> methodFilter) {
    ArrayList<IMethod> rt = new ArrayList<IMethod>();
    for (CGNode node : cg.getEntrypointNodes()) {
      //BFS traverse over all child-nodes.
      Queue<CGNode> queue = new LinkedList<CGNode>();
      Set<CGNode> visited = new HashSet<CGNode>();
      queue.add(node);
      visited.add(node);
      while (!queue.isEmpty()) {
        CGNode head = queue.remove();
        for (Iterator<CallSiteReference> iter = head.iterateCallSites(); iter.hasNext(); ) {
          CallSiteReference isite = iter.next();
          
          /* not used.
          //if the callsite is empty, find the corresponding iface and method
          if (cg.getPossibleTargets(head, isite).size() == 0 && classList != null) {
            MethodReference methodRef = isite.getDeclaredTarget();
            //filter out unexpected
            String pack = methodRef.getDeclaringClass().getName().getPackage().toString().replaceAll("/", ".");
            for (String pi : packageFilter) {
              if (pack.startsWith(pi)) {
                String callSig = methodRef.getSignature().toString();
                int lastDot = callSig.lastIndexOf(".");
                int firstParenthesis = callSig.indexOf("(");
                String ccName = callSig.substring(0, lastDot);
                String methodName = callSig.substring(lastDot+1, firstParenthesis);
                String methodSig = callSig.substring(firstParenthesis);

                boolean flag = false;
                String ifaceName = methodRef.getDeclaringClass().getName().toString();
                for (IClass ci : classList) {
                  if (ci.getName().toString().equals(ifaceName)) {
                    for (IMethod m : ci.getDeclaredMethods()) {
                      if (m.getSignature().equals(callSig)) {
                        if (rt.contains(m) == false) { rt.add(m); }
                        flag = true;
                      }
                    }
                  }
                }
                if (flag == false) {
                  //System.out.println("Cannot find empty callsite: " + isite.getDeclaredTarget().toString());
                  System.out.println("Cannot find empty cs: " + ccName + " " + methodName +  " " + methodSig);
                }
              }
            }
          }*/

          //else, iterate all possible target
          for (CGNode target : cg.getPossibleTargets(head, isite)) {
            IMethod targetM = target.getMethod();
            IClass targetC = targetM.getDeclaringClass();
            String targetPackage = targetC.getName().getPackage().toString().replaceAll("/", ".");
            String targetClassName = targetC.getName().toString();
            //check class
            boolean usefulClass = false;
            for (String pi : packageFilter) {
              if (targetPackage.startsWith(pi)) {
                 usefulClass = true;
              }
            }
            if (usefulClass == false) { continue; }

            if (targetC.isAbstract() || targetC.isInterface()) { continue; }
            if (targetM.isClinit()) { continue; }
            if (targetM.isInit()) { continue; }
            if (methodFilter.contains(targetM) == true) { continue; }

            if (visited.add(target) == true) {
              queue.add(target);
              if (rt.contains(targetM) == false) {
                rt.add(targetM);
              }
            }
          }
        }
      }
    }
    return rt;
  }

  public void writeToFile(ArrayList<IMethod>methods, String path, boolean append) {
    try {
      PrintWriter out = new PrintWriter(new FileOutputStream(new File(path), append));
      out.println("//format: class method sign");
      for (IMethod m : methods) {
        String cName = format(m.getDeclaringClass().getName().toString());
        String mName = m.getName().toString();
        out.println(cName + " " + mName + " " + m.getDescriptor().toString());
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String format(String str) {
    String rt = str;
    if (rt.startsWith("L")) {
      rt = rt.substring(1);
    }
    rt = rt.replaceAll("/", ".");
    return rt;
  }

  public void cntSignalWait (ArrayList<IMethod> allMethods) {
    int notifyNum = 0, waitNoParaNum = 0, waitParaNum = 0;
    boolean notifyCnted = false, waitNoParaCnted = false, waitParaCnted = false;
    for (IMethod mi : allMethods) {
      notifyCnted = false;
      waitNoParaCnted = false;
      waitParaCnted = false;
      for (SSAInstruction i : getSSAInst(mi)) {
        if (i instanceof SSAInvokeInstruction) {
          SSAInvokeInstruction callI = (SSAInvokeInstruction)i;
          int paraNum = callI.getNumberOfParameters();
          String callStr = callI.getDeclaredTarget().getName().toString();
          if ((callStr.equals("notify") || callStr.equals("notifyAll") ||
               callStr.equals("signal") || callStr.equals("signalAll"))
              && notifyCnted == false) {
            notifyNum++;
            notifyCnted = true;
            System.out.println("notify in: " + mi.getDeclaringClass().getName() + " " + mi.getName());
          }
          if (callStr.equals("wait") || callStr.equals("await")) {
            System.out.println("wait in: " + mi.getDeclaringClass().getName() + " " + mi.getName());
            if (paraNum == 1 && waitNoParaCnted == false) {
              waitNoParaNum++;
              waitNoParaCnted = true;
            }
            else if (paraNum == 2 && waitParaCnted == false) {
              waitParaNum++;
              waitParaCnted = true;
            }
          }
        }
      }
    }
    System.out.println("#.All Methods: " + allMethods.size());
    System.out.println("#. notify: " + notifyNum);
    System.out.println("#. wait no para: " + waitNoParaNum);
    System.out.println("#. wait para: " + waitNoParaNum);
  }

  public ArrayList<SSAInstruction> getSSAInst(IMethod m) {
    AnalysisOptions options = new AnalysisOptions();
    options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
    AnalysisCache cache = new AnalysisCache();
    IR mIR = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions());
    ArrayList<SSAInstruction> inst = new ArrayList<SSAInstruction>();
    if (mIR != null) {
      Iterator ii = mIR.iterateAllInstructions();
      while(ii.hasNext()) {
        inst.add((SSAInstruction)ii.next());
      }
    }
    return inst;
  }

}
