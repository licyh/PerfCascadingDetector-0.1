package com.prepare.Modify;

import spoon.processing.AbstractProcessor;
import spoon.template.Substitution;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtField;

import java.util.*;

import com.prepare.Modify.template.CCTemplate;
import com.prepare.Modify.checker.CCChecker;

public class CCModifierWrap extends AbstractProcessor<CtClass> {
  CCTemplate temp;
  CCChecker checker = null;

  public CCModifierWrap(CCTemplate temp_, CCChecker che) {
    temp = temp_;
    checker = che;
  }

  public void process (CtClass ctClass) {
    if (checker.isTarget(ctClass) == false) return;

    System.out.println("Mod CC: " + ctClass.getQualifiedName());
    if (ctClass.isInterface()) {
      for (CtMethod m : temp.getMethodToAddIface())
        ctClass.addMethod(m);
    }
    else {
      boolean hasInitMethod = ctClass.getMethodsByName("maybeInitBuilder").size() > 0;
      for (CtField<List<Date>> f : temp.getFieldToAdd())
        if (f != null) ctClass.addField(f);
      for (CtMethod m : temp.getMethodToAddCC(hasInitMethod))
        if (m != null) ctClass.addMethod(m);
    }
  }
}
