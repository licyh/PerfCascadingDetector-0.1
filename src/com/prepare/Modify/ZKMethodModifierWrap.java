package com.prepare.Modify;

import spoon.processing.AbstractProcessor;
import spoon.template.Substitution;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.code.CtStatement;
import spoon.reflect.factory.Factory;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtExecutableReference;
import spoon.template.ExtensionTemplate;

import java.util.*;

import com.prepare.Modify.checker.MethodChecker;


public class ZKMethodModifierWrap extends AbstractProcessor<CtMethod> {
  MethodChecker checker = null;
  Factory factory = null;

  public ZKMethodModifierWrap(MethodChecker che, Factory f) {
    checker = che;
    factory = f;
  }

  public void process (CtMethod ctMethod) {
    if (checker.isTarget(ctMethod) == false) return;
    System.out.println("Mod m: " + ctMethod.getSignature());
    CtStatement invokeState = null;

    if (ctMethod.getSimpleName().equals("serialize")) {
      Iterator<CtStatement> it = ctMethod.getBody().iterator();
      while(it.hasNext()) {
        CtStatement cur = it.next();
        if (cur instanceof CtInvocation) {
          CtInvocation inv = (CtInvocation) cur;
          CtExecutableReference target = inv.getExecutable();
          if (target.getSimpleName().toString().equals("endRecord")) {

            //System.out.println(((CtInvocation)cur).getTarget());
            //System.out.println(((CtInvocation)cur).getAnnotations());
            //System.out.println(((CtInvocation)cur).getExecutable());

            invokeState = (CtStatement)(factory.Code().createCodeSnippetStatement("this.set_DM_ID()"));
            cur.insertBefore(invokeState);

            invokeState = (CtStatement)(factory.Code().createCodeSnippetStatement(inv.getTarget().toString() + ".writeInt(_DM_ID, \"msgID\")"));

            //System.out.println(invokeState);
            cur.insertBefore(invokeState);
          }
        }
      }
    }
    else if (ctMethod.getSimpleName().equals("deserialize")) {
      Iterator<CtStatement> it = ctMethod.getBody().iterator();
      while(it.hasNext()) {
        CtStatement cur = it.next();
        if (cur instanceof CtInvocation) {
          CtInvocation inv = (CtInvocation) cur;
          CtExecutableReference target = inv.getExecutable();
          if (target.getSimpleName().toString().equals("endRecord")) {

            String str = "_DM_ID = " + inv.getTarget().toString() + ".readInt(\"msgID\")";
            invokeState = (CtStatement)(factory.Code().createCodeSnippetStatement(str));

            //System.out.println(invokeState);
            cur.insertBefore(invokeState);
          }
        }
      }
    }
  }
}
