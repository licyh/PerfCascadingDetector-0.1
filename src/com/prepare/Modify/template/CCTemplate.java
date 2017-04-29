package com.prepare.Modify.template;

import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtField;

import java.util.*;

public interface CCTemplate {

  public ArrayList<CtMethod> getMethodToAddIface();
  public ArrayList<CtMethod> getMethodToAddCC(boolean withInit);
  public ArrayList<CtField> getFieldToAdd();
}
