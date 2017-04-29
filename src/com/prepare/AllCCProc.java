package com.prepare;

import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtClass;

import java.util.*;

public class AllCCProc extends AbstractProcessor<CtClass> {
  ArrayList<CtClass> classList = new ArrayList<CtClass>();

  public void process(CtClass ctClass) {
    System.out.println("Hello allcc112");
    String ccName = ctClass.getQualifiedName();
    System.out.println("CC: " + ccName);
  }
}
