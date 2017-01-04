package dm.Util.Bytecode;

import java.io.*;
import java.util.*;

import javassist.*;
import javassist.bytecode.*;

public class InvokeInst extends Instruction {

  /* invoke instructions:
   * - invokedynamic
   * - invokeinterface
   * - invokespecial: call constructor function. Exclude this case.
   * - invokestatic
   * - invokevirtual: for thread.start(). it's a invokevirtual.
   */

  SignatureAttribute.MethodSignature invokeMethodSig = null;

  public InvokeInst(Instruction i) {
    this.setMethod(i.getMethod());
    this.setPos(i.getPos());
  }

  public int calledIndex() {
    return (int) codeIter.u16bitAt(pos + 1); //invoke index arg1
  }

  public String calledClass() {
    int index = calledIndex();
    if (isInvokeinterface())
      return constPool.getInterfaceMethodrefClassName(index);
    else
      return constPool.getMethodrefClassName(index);
  }

  public String calledMethod() {
    int index = calledIndex();
    if (isInvokeinterface())
      return constPool.getInterfaceMethodrefName(index);
    else
      return constPool.getMethodrefName(index);
  }

  public String calledMethodType() {
    int index = calledIndex();
    if (isInvokeinterface())
      return constPool.getInterfaceMethodrefType(index);
    else
      return constPool.getMethodrefType(index);
  }

  public void setMethodSig() {
    if (invokeMethodSig == null) {
      try {
        invokeMethodSig = SignatureAttribute.toMethodSignature(calledMethodType());
      } catch (BadBytecode e) {
        e.printStackTrace();
      }
    }
  }

  public int paraNum() {
    setMethodSig();
    return invokeMethodSig.getParameterTypes().length;
  }

  public String paraI(int index) {
    setMethodSig();
    return invokeMethodSig.getParameterTypes()[index].toString();
  }

  public ArrayList<String> paraArray() {
    setMethodSig();
    ArrayList<String> rt = new ArrayList<String>();
    for (SignatureAttribute.Type i : invokeMethodSig.getParameterTypes()) {
      rt.add(i.toString());
    }
    return rt;
  }

  /* move to ClassUtil. */
  /*public boolean isCalledThreadClass(String searchScope) {
    String calledClassStr = calledClass();
    ClassPool classPool = method.getDeclaringClass().getClassPool();

    //add search scope into class pool
    if (searchScope.equals("") == false) {
      try {
        if (searchScope.endsWith("/*")) {
          classPool.appendClassPath(searchScope);
        }
        else if (searchScope.endsWith("/")) {
          classPool.appendClassPath(searchScope + "*");
        }
        else {
          classPool.appendClassPath(searchScope + "/*");
        }
      } catch (NotFoundException e) {
        System.out.println("Cannot add path " + searchScope + " into search scope...");
        e.printStackTrace();
      }
    }

    CtClass calledClass = null;
    try {
      calledClass = classPool.get(calledClassStr);
      if (calledClass.getSuperclass().getName().equals("java.lang.Thread") == true) {
        return true;
      }
      else {
        return false;
      }
    } catch (NotFoundException e) {
      return false;
    }
  }*/


  public String toString() {
	// Modified by JX - this may be still not formal
    return opcode() + " " + "#" + calledIndex() + " = (..)Method " +
            calledClass() + "." +
            calledMethod() + ":" +
            calledMethodType();
    // end-Modified
  }

}
