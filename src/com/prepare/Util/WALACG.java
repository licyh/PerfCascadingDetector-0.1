package com.prepare.Util;

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CGNode;

import com.ibm.wala.classLoader.CallSiteReference;

import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.util.collections.HashSetFactory;


import java.io.*;
import java.util.*;


public class WALACG {
  public AnalysisScope scope;
  public IClassHierarchy cha;
  public ArrayList<IMethod> entryMethods = new ArrayList<IMethod>();
  Iterable<Entrypoint> entryPoints;
  CallGraph cg;

  public WALACG(AnalysisScope scope_, ClassHierarchy cha_) {
    scope = scope_; cha = cha_;
  }

  private Iterable<Entrypoint> makeEntrypoints() {
    final HashSet<Entrypoint> result = HashSetFactory.make();
    for (IMethod m : entryMethods) result.add(new DefaultEntrypoint(m, cha));
    return new Iterable<Entrypoint>() {
      public Iterator<Entrypoint> iterator() {
        return result.iterator();
      }
    };
  }
  public void setEntryMethods(ArrayList<IMethod> methodList) {
    entryMethods.addAll(methodList);
    entryPoints = makeEntrypoints();
    System.out.println("entry size: " + entryMethods.size());
  }

  public void buildCG() {
    AnalysisOptions options = new AnalysisOptions(scope, entryPoints);
    CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope);
    //CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
    //CallGraphBuilder builder = Util.makeRTABuilder(options, new AnalysisCache(), cha, scope);
    try {
      cg = builder.makeCallGraph(options, null);
    } catch (Exception e) { e.printStackTrace(); }
  }

  /*unclear method: in CG, a callsite is InvokeVirtual, but it cannot be solved.
   * And the potential target is only one.
   * Add such methods into entry.
   */
  public ArrayList<IMethod> unClearMethod(ArrayList<String> packageFilter, ArrayList<IMethod> methodFilter) {
    ArrayList<IMethod> rt = new ArrayList<IMethod>();
    for (CGNode node : cg.getEntrypointNodes()) {
      Queue<CGNode> queue = new LinkedList<CGNode>();
      Set<CGNode> visited = new HashSet<CGNode>();
      queue.add(node); visited.add(node);
      while (!queue.isEmpty()) {
        CGNode head = queue.remove();
        for (Iterator<CallSiteReference> iter = head.iterateCallSites(); iter.hasNext(); ) {
          CallSiteReference isite = iter.next();

          Set<CGNode> targets = cg.getPossibleTargets(head, isite);
          if (isite.isVirtual() && targets.size() == 0) {
            Set<IMethod> mTargets = cha.getPossibleTargets(isite.getDeclaredTarget());
            if (mTargets.size() == 1) {
              IMethod tmp = mTargets.iterator().next();
              if (rt.contains(tmp) == false &&
                  tmp.getDeclaringClass().getName().toString().startsWith("Ljava") == false) rt.add(tmp);
            }
          }

          for (CGNode target : targets) {
            IMethod targetM = target.getMethod();
            IClass targetC = targetM.getDeclaringClass();
            String targetPackage = PrepareUtil.typeToPack(targetC.getName().getPackage().toString());
            String targetClassName = targetC.getName().toString();
            //check class
            boolean usefulClass = false;
            for (String pi : packageFilter) {
              if (targetPackage.startsWith(pi)) usefulClass = true;
            }
            if (usefulClass == false) { continue; }
            if (targetC.isAbstract() || targetC.isInterface()) { continue; }
            //check method
            if (targetM.isClinit()) { continue; }
            if (targetM.isInit()) { continue; }
            if (methodFilter.contains(targetM) == true) { continue; }

            if (visited.add(target) == true) {
              queue.add(target);
            }
          }
        }
      }
    }
    return rt;
  }

  public ArrayList<IMethod> getAllCallee(ArrayList<String> packageFilter, ArrayList<IMethod> methodFilter) {
    ArrayList<IMethod> rt = new ArrayList<IMethod>();
    int cnt = 0;
    for (CGNode node : cg.getEntrypointNodes()) { //BFS traverse over all child-nodes.
      Queue<CGNode> queue = new LinkedList<CGNode>();
      Set<CGNode> visited = new HashSet<CGNode>();
      queue.add(node); visited.add(node);

      while (!queue.isEmpty()) {
        CGNode head = queue.remove();
        for (Iterator<CallSiteReference> iter = head.iterateCallSites(); iter.hasNext(); ) {
          CallSiteReference isite = iter.next();

          Set<CGNode> targets = cg.getPossibleTargets(head, isite);
          if (isite.isVirtual() && targets.size() == 0) {
            Set<IMethod> mTargets = cha.getPossibleTargets(isite.getDeclaredTarget());
            if (mTargets.size() == 1) {
              IMethod tmp = mTargets.iterator().next();
              if (rt.contains(tmp) == false) rt.add(tmp);
            }
            continue;
          }

          for (CGNode target : targets) {
            IMethod targetM = target.getMethod();
            IClass targetC = targetM.getDeclaringClass();
            String targetPackage = PrepareUtil.typeToPack(targetC.getName().getPackage().toString());
            String targetClassName = targetC.getName().toString();
            //check class
            boolean usefulClass = false;
            for (String pi : packageFilter) {
              if (targetPackage.startsWith(pi)) usefulClass = true;
            }
            if (usefulClass == false) { continue; }
            cnt++;
            if (targetC.isAbstract() || targetC.isInterface()) { continue; }
            //check method
            if (targetM.isClinit()) { continue; }
            if (targetM.isInit()) { continue; }
            if (methodFilter.contains(targetM) == true) { continue; }

            if (visited.add(target) == true) {
              queue.add(target);
              if (rt.contains(targetM) == false) rt.add(targetM);
            }
          }
        }
      }
    }
    System.out.println("Callee size: " + rt.size());
    return rt;
  }
}
