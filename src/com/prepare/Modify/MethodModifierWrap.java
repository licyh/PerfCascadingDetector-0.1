package com.prepare.Modify;

import spoon.processing.AbstractProcessor;
import spoon.template.Substitution;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.template.ExtensionTemplate;

import java.util.*;

import com.prepare.Modify.template.MethodTemplate;
import com.prepare.Modify.checker.MethodChecker;

/*add newPara to the method parameter list.
 */

public class MethodModifierWrap extends AbstractProcessor<CtMethod> {
  MethodTemplate temp;
  MethodChecker checker = null;

  public MethodModifierWrap(MethodTemplate temp_, MethodChecker che) {
    temp = temp_;
    checker = che;
  }

  public void process (CtMethod ctMethod) {
    if (checker.isTarget(ctMethod) == false) return;
    System.out.println("Mod m: " + ctMethod.getSignature());
    int index = temp.insertBegin() ? 0 : ctMethod.getParameters().size();
    ctMethod.getParameters().addAll(index, temp.getParaToAdd());
  }
}
