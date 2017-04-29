package com.prepare.Modify.template;

import spoon.reflect.declaration.CtParameter;

import java.util.*;

public interface MethodTemplate {

  public ArrayList<CtParameter> getParaToAdd();
  public boolean insertBegin();
}
