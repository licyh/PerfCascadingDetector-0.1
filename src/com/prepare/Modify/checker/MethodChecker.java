package com.prepare.Modify.checker;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeInformation;

import java.util.*;

public class MethodChecker {
  ArrayList<String> qualifiedList = new ArrayList<String>();

  public MethodChecker(ArrayList<String> list) {
    qualifiedList.addAll(list);
  }

  public boolean isTarget(CtMethod ctMethod) {
    CtTypeInformation ctClass = (CtTypeInformation)(ctMethod.getParent());
    String str = ctClass.getQualifiedName() + "::" + ctMethod.getSimpleName();
    return qualifiedList.contains(str);
    /*if (ctMethod.getSimpleName().equals("unregister"))
      return true;
    return false;*/
  }
}
