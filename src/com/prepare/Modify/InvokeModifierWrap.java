package com.prepare.Modify;

import spoon.processing.AbstractProcessor;
import spoon.template.Substitution;
import spoon.reflect.code.CtInvocation;
import spoon.template.ExtensionTemplate;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;

import java.util.*;

import com.prepare.Modify.template.InvokeTemplate;
import com.prepare.Modify.checker.InvokeChecker;
import com.comm.Util;

/* mode1: add statement before
 * mode2: replace statement
 */

public class InvokeModifierWrap extends AbstractProcessor<CtInvocation> {
  InvokeTemplate temp;
  InvokeChecker checker = null;

  public InvokeModifierWrap(InvokeTemplate temp_, InvokeChecker che) {
    temp = temp_;
    checker = che;
  }

  public void process (CtInvocation ctInvoke) {

    int paraPos = checker.getTargetParaIndex(ctInvoke);

    if (temp.getMode() == InvokeTemplate.MODE.INSERT_BEFORE) {
      //if (checker.isTargetPara(ctInvoke) == false) return;
      if (paraPos == -1) return;
    }
    else {
      if (checker.isTargetMethod(ctInvoke) == false) return;
    }
    //if (checker.isTarget(ctInvoke) == false) return;
    String ccFile = ctInvoke.getPosition().getFile().getName();
    //if (ccFile.equals("ResourceMgrDelegate.java")) return;

    if (temp.getMode() == InvokeTemplate.MODE.INSERT_BEFORE) {
      // for MR-v2, 1st para is request obj.
      System.out.println("Mod inv: " + ctInvoke.toString());
      CtExpression argu = (CtExpression)(ctInvoke.getArguments().get(paraPos));

      CtStatement invokeState = temp.getInvokeToInsert(argu);

      String pos = ctInvoke.getPosition().toString();
      if (pos.contains("PBClientImpl") || pos.contains("PBServiceImpl") || pos.contains("PBImpl") ||
          pos.contains("ClientRMService")) return;
      /*if (pos.contains("ClientServiceDelegate.java:333")) {
        System.out.println("jobclient, is ctstatement? " + (ctInvoke instanceof CtStatement));
        System.out.println("jobclient, para type: " + ctInvoke.getParent().getClass().toString());
        System.out.println("jobclient, para2 type: " + ctInvoke.getParent().getParent().getClass().toString());
        System.out.println("jobclient, para3 type: " + ctInvoke.getParent().getParent().getParent().getClass().toString());
        System.out.println("jobclient, par is assign? " + (ctInvoke.getParent() instanceof CtAssignment));
        ((CtStatement)ctInvoke.getParent().getParent()).insertBefore(invokeState);
        return;
      }*/

      if (ctInvoke instanceof CtStatement) {
        CtStatement tmp = (CtStatement) ctInvoke;
        while (!(tmp.getParent() instanceof CtBlock)) { tmp = (CtStatement)(tmp.getParent()); }
        //System.out.println("jobclient, type: " + tmp.getClass().toString());
        //System.out.println("jobclient, para type: " + tmp.getParent().getClass().toString());
        tmp.insertBefore(invokeState);
        /*System.out.println("jobclient, type: " + ctInvoke.getClass().toString());
        System.out.println("jobclient, para type: " + ctInvoke.getParent().getClass().toString());
        ctInvoke.insertBefore(invokeState);*/
      }
      else if (ctInvoke instanceof CtExpression &&
          ctInvoke.getParent() instanceof CtStatement) {
        System.out.println("exp type: " + ctInvoke.getType().toString());
        CtStatement parent = (CtStatement)(ctInvoke.getParent());
        parent.insertBefore(invokeState);
      }
      else { Util.exitWithMsg("bad ctInvoke. Exit"); }
    }
    else
      ctInvoke.addArgument(temp.getArguToInsert());
  }
}

