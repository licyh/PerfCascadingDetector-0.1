package com.prepare.Modify.checker;
import spoon.reflect.code.CtInvocation;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.code.CtExpression;
import spoon.reflect.reference.CtExecutableReference;

import java.util.*;

public class InvokeChecker {
  ArrayList<String> qualifiedCCMethodList = new ArrayList<String>();
  ArrayList<String> qualifiedParaList = new ArrayList<String>();

  public InvokeChecker (ArrayList<String> mList, ArrayList<String> paraList) {
    qualifiedCCMethodList.addAll(mList);
    qualifiedParaList.addAll(paraList);
  }

  public boolean isTargetMethod(CtInvocation ctInvoke) {
    CtExecutableReference ctMethod = (ctInvoke.getExecutable());
    //format: org.apache.hadoop.fs.FileSystem#mkdirs(org.apache.hadoop.fs.Path)
    //System.out.println("Qua Name: " + ctMethod.getSignature());
    String str = ctMethod.getSignature().split("\\(")[0].replace("#", "::");
    //System.out.println("Qua Name: " + str1);
    return qualifiedCCMethodList.contains(str);
    /*if (ctMethod.getParent() instanceof CtTypeInformation) {
      CtTypeInformation ctClass = (CtTypeInformation)(ctMethod.getParent());
      String str = ctClass.getQualifiedName() + "::" + ctMethod.getSimpleName();
    if (ctMethod.getSimpleName().equals("fatalError")) {
      System.out.println("find fatalerror call: " + str);
      System.out.println("contains? " + qualifiedCCMethodList.contains(str));
    }
      return qualifiedCCMethodList.contains(str);
    }
    else return false;*/

    /*if (ctInvoke.getExecutable().getSimpleName().equals("getProtocolSignature"))
      return true;
    return false;*/
  }

  public boolean isTargetPara (CtInvocation ctInvoke) {
    String pos = ctInvoke.getPosition().toString();
    if (pos.contains("ClientServiceDelegate")) {
      System.out.println("HP pos: " + pos);
      System.out.println("HP pos inv: " + ctInvoke.toString());
    }
    List<CtExpression> arguList = ctInvoke.getArguments();
    if (arguList.size() != 1) return false;
    String arguType = arguList.get(0).getType().getQualifiedName();
    return qualifiedParaList.contains(arguType);
  }

  public int getTargetParaIndex (CtInvocation ctInvoke) {
    int rt = -1;
    List<CtExpression> arguList = ctInvoke.getArguments();
    for (int i=0; i < arguList.size(); i++) {
      if (arguList.get(i).getType() == null) continue;
      String arguType = arguList.get(i).getType().getQualifiedName();
      if (qualifiedParaList.contains(arguType)) {
        if (rt != -1) {
          System.out.println("Find multiple matched paras in one invocation. Exit...");
          System.exit(-1);
        }
        rt = i;
      }
    }
    /*String pos = ctInvoke.getPosition().toString();
    if (pos.contains("ClientServiceDelegate")) {
      System.out.println("HP pos: " + pos);
      System.out.println("HP pos inv: " + ctInvoke.toString());
      System.out.println("HP pos return: " + rt);
    }*/
    return rt;
  }

}
