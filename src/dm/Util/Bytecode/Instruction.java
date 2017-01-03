package dm.Util.Bytecode;

import java.io.*;
import java.util.*;

import javassist.*;
import javassist.bytecode.*;

public class Instruction {
  CtBehavior method;
  MethodInfo methodInfo;
  ConstPool constPool;
  CodeAttribute codeAttr;
  CodeIterator codeIter;

  int pos;

  public void setMethod(CtBehavior m) {
    method = m;
    methodInfo = method.getMethodInfo();
    constPool = methodInfo.getConstPool();
    codeAttr = methodInfo.getCodeAttribute();
    codeIter = codeAttr.iterator();
  }
  public CtBehavior getMethod() { return method; }

  public void setPos(int pos_) { pos = pos_; }
  public int getPos() { return pos; }

  public String opcode() {
    codeIter = codeAttr.iterator();
    int opcode = codeIter.byteAt(pos);
    return Mnemonic.OPCODE[opcode];
  }

  public boolean isInvoke() {
    return opcode().startsWith("invoke");
  }

  public boolean isInvokevirtual() {
    return opcode().startsWith("invokevirtual");
  }

  public boolean isInvokeinterface() {
    return opcode().startsWith("invokeinterface");
  }

  public boolean isInvokespecial() {
    return opcode().startsWith("invokespecial");
  }

  public boolean isMonitor() {
    return opcode().startsWith("monitor");
  }

  public boolean isMonitorEnter() {
    return opcode().startsWith("monitorenter");
  }

  public boolean isMonitorExit() {
    return opcode().startsWith("monitorexit");
  }

  public boolean isLDCW() {
    return opcode().startsWith("ldc_w");
  }

  public boolean isLoad() {
    return opcode().matches("[a-zA-Z]+load($|_[\\d+])");
  }

  public boolean isField() {
    return opcode().matches("(get|put)(field|static)($|_[\\d+])");
  }

  //Added by JX
  public String toString() {
	return opcode();
  }
  //end-Added
  
}

