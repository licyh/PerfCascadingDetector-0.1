package com.prepare.Modify.checker;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;

public class CCChecker {
  ArrayList<String> qualifiedList = new ArrayList<String>();

  public CCChecker (ArrayList<String> list) {
    qualifiedList.addAll(list);
  }

  public boolean isTarget(CtClass ctClass) {
    if (qualifiedList.contains(ctClass.getQualifiedName())) return true;
    for (CtTypeReference<?> iface : ctClass.getSuperInterfaces()) {
      if (qualifiedList.contains(iface.getQualifiedName())) return true;
    }
    return false;

    //return qualifiedList.contains(ctClass.getQualifiedName());
    /*if (ctClass.getSimpleName().equals("TaskAttemptListenerImpl")) {
      System.out.println("Pack: " + ctClass.getQualifiedName());
      return true;
    }
    return false;*/
  }
}
