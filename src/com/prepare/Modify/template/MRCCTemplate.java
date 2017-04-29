package com.prepare.Modify.template;

import spoon.reflect.factory.Factory;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtField;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtBlock;
import spoon.reflect.declaration.CtParameter;

import java.util.*;


public class MRCCTemplate implements CCTemplate {
  Factory factory = null;;
  //for proto class

  CtMethod getDMIDInIface = null;
  CtMethod setDMIDInIface = null;
  CtField<Integer> DMID = null;
  CtMethod getDMIDInCC = null;
  CtMethod setDMIDInCC = null;
  CtMethod setDMIDInCCWithBuilderInit = null;

  public static CtMethod getEmptyMethod(Factory f) {
    CtParameter<List<Integer>> parameter = f.Core().<List<Integer>>createParameter();
    CtMethod rt = f.Core().createMethod();
    rt.addModifier(ModifierKind.PUBLIC);
    rt.setParameters(Collections.<CtParameter<?>>singletonList(parameter));
    return rt;
  }

  public static CtMethod getEmptyMethodWithBody(Factory f) {
    CtBlock<?> ctBlock = f.Core().createBlock();
    CtParameter<List<Integer>> parameter = f.Core().<List<Integer>>createParameter();
    CtMethod rt = f.Core().createMethod();
    rt.addModifier(ModifierKind.PUBLIC);
    rt.setParameters(Collections.<CtParameter<?>>singletonList(parameter));
    rt.setBody(ctBlock);
    return rt;
  }

  public MRCCTemplate(Factory f) {
    factory = f;

    //init DMID field
    CtTypeReference <Integer> intTy = factory.Type().integerPrimitiveType();
    DMID = factory.Core().<Integer>createField();
    DMID.<CtField>setType(intTy);
    DMID.<CtField>addModifier(ModifierKind.PRIVATE);
    DMID.setSimpleName("DMID");
    CtExpression<Integer> zero = factory.Code().createCodeSnippetExpression("0");
    DMID.setDefaultExpression(zero);

    //init getDMID in iface
    getDMIDInIface = MRCCTemplate.getEmptyMethod(factory);
    getDMIDInIface.setSimpleName("getDMID");
    getDMIDInIface.addModifier(ModifierKind.ABSTRACT);
    getDMIDInIface.setType(factory.Type().stringType());

    //init setDMID in iface
    setDMIDInIface = MRCCTemplate.getEmptyMethod(factory);
    setDMIDInIface.setSimpleName("setDMID");
    setDMIDInIface.addModifier(ModifierKind.ABSTRACT);
    setDMIDInIface.setType(factory.Type().voidPrimitiveType());

    //init getDMID in cc
    getDMIDInCC = MRCCTemplate.getEmptyMethodWithBody(factory);
    getDMIDInCC.setSimpleName("getDMID");
    getDMIDInCC.setType(factory.Type().stringType());

    String[] getStr = {"if (this.DMID != 0 ) return Integer.toString(DMID)",
                       "return Integer.toString(proto.getDMID())"};
    for (String i : getStr) {
      CtCodeSnippetStatement state = factory.Code().createCodeSnippetStatement(i);
      getDMIDInCC.getBody().insertEnd(state);
    }

    //init setDMID in cc
    setDMIDInCC = MRCCTemplate.getEmptyMethodWithBody(factory);
    setDMIDInCC.setSimpleName("setDMID");
    setDMIDInCC.setType(factory.Type().voidPrimitiveType());
    String[] setStr = {"DMID = java.util.concurrent.ThreadLocalRandom.current().nextInt();",
                       "builder.setDMID(DMID);"};
    for (String i : setStr) {
      CtCodeSnippetStatement state = factory.Code().createCodeSnippetStatement(i);
      setDMIDInCC.getBody().insertEnd(state);
    }


    //init setDMIDWithInit in cc
    setDMIDInCCWithBuilderInit = MRCCTemplate.getEmptyMethodWithBody(factory);
    setDMIDInCCWithBuilderInit.setSimpleName("setDMID");
    setDMIDInCCWithBuilderInit.setType(factory.Type().voidPrimitiveType());
    String[] setStrWithInit = {"DMID = java.util.concurrent.ThreadLocalRandom.current().nextInt();",
                       "maybeInitBuilder();",
                       "builder.setDMID(DMID);"};
    for (String i : setStrWithInit) {
      CtCodeSnippetStatement state = factory.Code().createCodeSnippetStatement(i);
      setDMIDInCCWithBuilderInit.getBody().insertEnd(state);
    }
  }


  public ArrayList<CtMethod> getMethodToAddIface() {
    ArrayList<CtMethod> rt = new ArrayList<CtMethod>();
    rt.add(getDMIDInIface);
    rt.add(setDMIDInIface);
    return rt;
  }

  public ArrayList<CtMethod> getMethodToAddCC(boolean withInit) {
    ArrayList<CtMethod> rt = new ArrayList<CtMethod>();
    rt.add(getDMIDInCC);
    if (withInit) rt.add(setDMIDInCCWithBuilderInit);
    else rt.add(setDMIDInCC);
    return rt;
  }

  public ArrayList<CtField> getFieldToAdd() {
    ArrayList<CtField> rt = new ArrayList<CtField>();
    rt.add(DMID);
    return rt;
  }
}


/*class AddInProtoIface extends ExtensionTemplate {
  public String getDMID();
  public void setDMID();
}

class AddInProtoCC extends ExtensionTemplate {
  private int DMID = 0;
  public String getDMID() {
    if (this.DMID != 0 ) return Integer.toString(DMID);
    //return Integer.toString(proto.getDMID());
  }
  public void setDMID() {
    DMID = java.util.concurrent.ThreadLocalRandom.current().nextInt();
    //builder.setDMID(DMID);
  }
}*/
