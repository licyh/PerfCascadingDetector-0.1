package com.prepare.Modify;

import spoon.processing.AbstractProcessor;
import spoon.template.Substitution;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtField;

import java.util.*;

import com.prepare.Modify.template.CCTemplate;
import com.prepare.Modify.checker.IfaceChecker;

public class IfaceModifierWrap extends AbstractProcessor<CtInterface> {
  CCTemplate temp;
  IfaceChecker checker = null;

  public IfaceModifierWrap(CCTemplate temp_, IfaceChecker che) {
    temp = temp_;
    checker = che;
  }

  public void process (CtInterface ctIface) {
    if (checker.isTarget(ctIface) == false) return;

    System.out.println("Mod Iface: " + ctIface.getQualifiedName());
    for (CtMethod m : temp.getMethodToAddIface())
      ctIface.addMethod(m);
  }
}
