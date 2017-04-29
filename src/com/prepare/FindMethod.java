package com.prepare;


import java.util.*;
import com.comm.Util;
import com.prepare.Util.PrepareUtil;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;

public class FindMethod {

  Config config;
  ArrayList<IClass> allCC;

  String identFlag;
  //for case1: CCMethod
  String ccKeyWord;
  String methodKeyWord;

  //for case2: Para
  String paraKeyWord;

  //for case3: SpecInst
  String specInstKeyWord;

  //case1: class/method based: given a name of iface/method. HB, CA, MR-v1
    //for HB: ccKey = VersionedProtocol; methodKey = "*"
    //for CA: ccKey = "IVerbHandler"; methodKey = "doVerb"
  //case2: para-type based: given a name of parameter type. MR-v2
  //case3: special instruction based: ZK
  //
  ArrayList<Tuple> identifiedTuple = new ArrayList<Tuple>();
  ArrayList<String> identifiedList = new ArrayList<String>();

  public FindMethod (Config config_, ArrayList<IClass> allCC_) {
    config = config_; allCC = allCC_;
  }

  public void setCCMethod(String identFlag_, String ccKeyWord_, String methodKeyWord_) {
    if (identFlag_.equals("CCMethod") == false) {
      Util.exitWithMsg("Set id flag incorrectly. Exit...");
    }
    identFlag = identFlag_;
    ccKeyWord = ccKeyWord_;
    methodKeyWord = methodKeyWord_;
    getBaseCCMethod();
  }
  public void setPara(String identFlag_, String paraKeyWord_) {
    if (identFlag_.equals("Para") == false) {
      Util.exitWithMsg("Set id flag incorrectly. Exit...");
    }
    identFlag = identFlag_;
    paraKeyWord = paraKeyWord_;
    getBasePara();
  }
  public void setSpecInst(String identFlag_, String specInstKeyWord_) {
    if (identFlag_.equals("SpecInst") == false) {
      Util.exitWithMsg("Set id flag incorrectly. Exit...");
    }
    identFlag = identFlag_;
    specInstKeyWord = specInstKeyWord_;
    getBaseSpecInst();
  }

  public ArrayList<IMethod> getAllKeyMethod() {
    ArrayList<IMethod> rt = new ArrayList<IMethod>();
    for (Tuple t : identifiedTuple) rt.add(t.method);
    return rt;
  }

  public ArrayList<String> getAllKeyStr() {
    ArrayList<String> rt = new ArrayList<String>();
    rt.add("//format: class iface method codeFlag version paraNum para");
    for (Tuple t : identifiedTuple) rt.add(t.toString());
    return rt;
  }

  /*for BaseProto case*/
  public void getIdentifiedCC (ArrayList<String> list) { //for modifier
    for (String i : identifiedList) {
      String[] str = i.split("::");
      if (str[0].equals("*")) continue;
      if (list.contains(str[0]) == false) list.add(str[0]);
    }
  }
  //for BaseCCMethod case
  public void getIdentifiedCCMethod (ArrayList<String> list) {
    for (String i : identifiedList) {
      if (i.startsWith("*") || i.endsWith("*")) continue;
      if (list.contains(i) == false) list.add(i);
    }
  }

  public void getBaseCCMethod(){
    ArrayList<IClass> keyCCIface = PrepareUtil.getCCByName(allCC, ccKeyWord);
    ArrayList<IClass> keyCC = PrepareUtil.getCCInList(keyCCIface);
    ArrayList<IClass> keyIface = PrepareUtil.getIfaceInList(keyCCIface);

    for (IClass c : keyCC) {
      for (IClass cc : PrepareUtil.getCCFamily(c)) {
        if (keyIface.contains(cc) == false) continue;
        if (cc.getName().toString().endsWith("VersionedProtocol")) continue;
        if (cc.getName().toString().endsWith("ClientProtocol")) continue;
        for (IMethod m : c.getDeclaredMethods()) {
          if (m.isPublic() == false && m.isProtected() == false) continue;
          if (m.isInit() || m.isAbstract() || m.isClinit()) continue;

          if (methodKeyWord.equals("*")) {
            if (PrepareUtil.methodInCC(cc, m.getName().toString())) {
              identifiedTuple.add(new Tuple(c, cc, m, 3, 1));
              //add to identified list for source code modifier
              String str = PrepareUtil.typeToPack(c.getName().toString()) + "::" + m.getName();
              if (!identifiedList.contains(str)) identifiedList.add(str);
              str = PrepareUtil.typeToPack(cc.getName().toString()) + "::" + m.getName();
              if (!identifiedList.contains(str)) identifiedList.add(str);
            }
          }
          else {
            if (m.getName().toString().equals(methodKeyWord)) {
              identifiedTuple.add(new Tuple(c, cc, m, 3, 1));
              //add to identified list for source code modifier
              String str = PrepareUtil.typeToPack(c.getName().toString()) + "::" + m.getName();
              if (!identifiedList.contains(str)) identifiedList.add(str);
              str = PrepareUtil.typeToPack(cc.getName().toString()) + "::" + m.getName();
              if (!identifiedList.contains(str)) identifiedList.add(str);
            }
          }
        }
      }
    }
  }

  public void getBasePara() { //for MR-v2
    //Only 2 cases:
    //case1: MR-v2 rpc. compare parameter and return type
    //case2: zk request. only compare parameter.
    ArrayList<IClass> keyCCIface = PrepareUtil.getCCByName(allCC, paraKeyWord);
    boolean isMR = config.bugID.startsWith("MR");
    boolean isZK = config.bugID.startsWith("ZK");
    boolean isHB = config.bugID.startsWith("HB");
    System.out.println("here....");
    for (IClass c : keyCCIface) System.out.println("key iface: " + c.getName());
    for (IClass c: allCC){
      if (c.getName().toString().endsWith("Request"))
      System.out.println("c: " + c.getName());
      if (c.getName().toString().endsWith("FlushRegionRequest")) {
        System.out.println("find c: " + c.getName());
      }
    }

    for (IClass c : allCC) {
      if (c.getName().toString().contains("ClientImpl")) { continue; } // client impl

      for (IMethod m : c.getDeclaredMethods()) {
        if (m.isAbstract() || m.isInit() || m.isClinit()) { continue; } // abstrct method
        if (isMR || isZK) {
          if (m.getNumberOfParameters() != 2) { continue; }
        }
        else if (isHB) {
          if (m.getNumberOfParameters() != 3) { continue; }
          if (c.getName().toString().endsWith("BlockingStub") ||
              c.getName().toString().endsWith("Builder")) { continue; }
          if (c.getName().toString().contains("security")) { continue; }
        }

        String paraTy = m.getParameterType(m.getNumberOfParameters()-1).toString(); // format: <Application, Lorg/.../Class>
        paraTy = paraTy.substring(paraTy.lastIndexOf(",")+1, paraTy.length()-1);
        String rtType = m.getReturnType().toString();
        rtType = rtType.substring(rtType.lastIndexOf(",")+1, rtType.length()-1);
        if (isMR) {
          if (PrepareUtil.cfHasType(keyCCIface, paraTy) && PrepareUtil.cfHasType(keyCCIface, rtType)) {
            for (IClass iface : c.getAllImplementedInterfaces()) {
              if (PrepareUtil.methodInCC(iface, m.getName().toString())) {
                identifiedTuple.add(new Tuple(c, iface, m, 2, 2));
                //add to identified list for source code modifier
                String str = PrepareUtil.typeToPack(paraTy) + "::*";
                if (!identifiedList.contains(str) && 
                    (paraTy.endsWith("Request") || paraTy.endsWith("RequestImpl"))) identifiedList.add(str);
              }
            }
          }
        }
        else if (isZK) { //ZK
          if (PrepareUtil.cfHasType(keyCCIface, paraTy)) {
            identifiedTuple.add(new Tuple(c, null, m));
            //add to identified list for source code modifier
            String str = PrepareUtil.typeToPack(paraTy) + "::*";
            if (!identifiedList.contains(str)) identifiedList.add(str);
          }
        }
        else if (isHB) {
          if (PrepareUtil.cfHasType(keyCCIface, paraTy) && PrepareUtil.cfHasType(keyCCIface, rtType)) {
            for (IClass iface : c.getAllImplementedInterfaces()) {
              if (PrepareUtil.methodInCC(iface, m.getName().toString())) {
                identifiedTuple.add(new Tuple(c, iface, m));
                //add to identified list for source code modifier
                String str = PrepareUtil.typeToPack(paraTy) + "::*";
                if (!identifiedList.contains(str)) identifiedList.add(str);
              }
            }
          }
        }
      }
    }
  }


  private ArrayList<SSANewInstruction> getSSANew(IMethod m) {
    ArrayList<SSANewInstruction> rt = new ArrayList<SSANewInstruction>();

    AnalysisOptions options = new AnalysisOptions();
    options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
    AnalysisCache cache = new AnalysisCache();
    IR ir = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions());
    if (ir == null) return rt;

    Iterator ii = ir.iterateAllInstructions();
    while(ii.hasNext()) {
      SSAInstruction i = (SSAInstruction)ii.next();
      if (i instanceof SSANewInstruction) {
        rt.add((SSANewInstruction)i);
      }
    }
    return rt;
  }

  public void getBaseSpecInst() { //for ZK record
    ArrayList<IClass> keyCCIface = PrepareUtil.getCCByName(allCC, specInstKeyWord);

    for (IClass c : allCC) {
      for (IMethod m : c.getDeclaredMethods()) {
        if (m.isNative()) continue;
        for (SSANewInstruction si : getSSANew(m)) {
          String type = si.getConcreteType().getName().toString();
          if (PrepareUtil.hasType(keyCCIface, type)) {
            identifiedTuple.add(new Tuple(c, null, m));
            //add to identified list for source code modifier
            String str = PrepareUtil.typeToPack(c.getName().toString()) + "::" + m.getName();
            if (!identifiedList.contains(str)) identifiedList.add(str);
            continue;
          }
        }
      }
    }
  }
}

class Tuple {
  public IClass cc;
  public IClass iface;
  public IMethod method;
  public int codeFlag = -1, version = -1;
  public Tuple (IClass cc_, IClass iface_, IMethod method_) {
    cc = cc_;
    iface = iface_;
    method = method_;
  }
  public Tuple (IClass cc_, IClass iface_, IMethod method_, int flag, int v) {
    cc = cc_;
    iface = iface_;
    method = method_;
    codeFlag = flag;
    version = v;
  }
  public String toString() {
    String str = PrepareUtil.typeToPack(cc.getName().toString()) + " ";
    if (iface != null)
      str += PrepareUtil.typeToPack(iface.getName().toString()) + " ";
    else
      str += "null ";
    str += method.getName().toString() + " ";
    str += codeFlag + " " + version + " ";
    str += (method.getNumberOfParameters()-1) + " ";
    for (int i=1; i < method.getNumberOfParameters(); i++) {
      if (method.getParameterType(i).isReferenceType()) str += "Ljava/lang/Object ";
      else str += method.getParameterType(i).getName() + " ";
    }
    return str;
  }
}
