package com.prepare.Modify.checker;

import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;

import java.util.*;

public class IfaceChecker {
  ArrayList<String> qualifiedList = new ArrayList<String>();

  public IfaceChecker (ArrayList<String> list) {
    qualifiedList.addAll(list);
  }

  public boolean isTarget(CtInterface ctClass) {
    return qualifiedList.contains(ctClass.getQualifiedName());
    /*if (ctClass.getSimpleName().equals("TaskAttemptListenerImpl")) {
      System.out.println("Pack: " + ctClass.getQualifiedName());
      return true;
    }
    return false;*/
  }
}
