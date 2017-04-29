package com.prepare.Modify.template;

import spoon.reflect.factory.Factory;
import spoon.reflect.declaration.CtParameter;

import java.util.*;

public class MRMethodTemplate implements MethodTemplate {
  Factory factory = null;

  CtParameter para = null;
  boolean atBegin = false; //insert pos

  public MRMethodTemplate(Factory f, boolean begin) {
    factory = f;
    atBegin = begin;

    para = factory.createParameter();
    para.setSimpleName("DMID");
    para.setType(factory.Type().stringType());
  }

  public ArrayList<CtParameter> getParaToAdd() {
    ArrayList<CtParameter> rt = new ArrayList<CtParameter>();
    rt.add(para);
    return rt;
  }

  public boolean insertBegin() { return atBegin;}
}
