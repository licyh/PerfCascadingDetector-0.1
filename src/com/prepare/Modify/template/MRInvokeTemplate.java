package com.prepare.Modify.template;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtStatement;
import spoon.reflect.factory.Factory;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.code.CtCodeSnippetExpression;

import java.util.*;

import com.comm.Util;

public class MRInvokeTemplate implements InvokeTemplate {
  Factory factory = null;

  MODE mode = MODE.REPLACE;
  CtExpression<String> arguToInsert = null;
  //CtInvocation<Void> invokeToInsert = null;
  CtStatement invokeToInsert = null;

  public MRInvokeTemplate(Factory f, MODE mode_) {
    factory = f;
    mode = mode_;

    //init argument
    arguToInsert = factory.Code().createCodeSnippetExpression("Integer.toString(java.util.concurrent.ThreadLocalRandom.current().nextInt())");
  }

  public CtStatement getInvokeToInsert(CtExpression argu) {
    CtTypeReference voidTy = factory.Type().voidPrimitiveType();
    CtMethod setIDMethod = argu.getType().getTypeDeclaration().getMethod(voidTy, "unregister");

    List<CtMethod> mList = argu.getType().getTypeDeclaration().getMethodsByName("setDMID");
    if (mList.size() == 1) setIDMethod = mList.get(0);
    if (setIDMethod == null) {
      System.out.println("name: " + argu.toString());
      invokeToInsert = (CtStatement)(factory.Code().createCodeSnippetStatement(argu.toString() + ".setDMID()"));
      return invokeToInsert;
      //Util.exitWithMsg("Cannot find or find multiple setDMID. Exit...");
    }

    CtCodeSnippetExpression invokeArgu = factory.Code().createCodeSnippetExpression("");
    invokeToInsert = factory.Code().createInvocation(argu, setIDMethod.getReference(), invokeArgu);
    if (invokeToInsert == null) Util.exitWithMsg("Cannot create invoke. Exit...");
    return invokeToInsert;
  }

  public CtExpression getArguToInsert() { return arguToInsert; }
  public MODE getMode() { return mode; }
}

