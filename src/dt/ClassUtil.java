package dt;

import javassist.*;
import javassist.bytecode.*;

import java.util.ArrayList;

public class ClassUtil {
  String searchScope;
  ClassPool classPool;

  public void setSearchScope(String str) { searchScope = str; }

  public void setClassPool(CtBehavior method) {
    classPool = method.getDeclaringClass().getClassPool();
  }

  public ClassPool getClassPool() { return classPool; }

  public void updateClassPool() {
    if (searchScope.equals("") == false) {
      try {
        if (searchScope.endsWith("/*"))
          classPool.appendClassPath(searchScope);
        else if (searchScope.endsWith("/"))
          classPool.appendClassPath(searchScope + "*");
        else
          classPool.appendClassPath(searchScope + "/*");
      } catch (NotFoundException e) {
        System.out.println("Cannot add path " + searchScope + " into search scope...");
        e.printStackTrace();
      }
    }
  }

  public CtClass getClass(String className) {
    CtClass cc = null;
    try {
      cc = classPool.get(className);
    } catch (NotFoundException e) {
      e.printStackTrace();
    }
    return cc;
  }

  public CtClass getSuperclass(String className) {
    CtClass cc = getClass(className);
    CtClass superClass = null;
    if (cc == null) return superClass;

    try {
      superClass = cc.getSuperclass();
    } catch (NotFoundException e) {
      e.printStackTrace();
    }
    return superClass;
  }

  public ArrayList<CtClass> getAllSuperclass(String className) {
    CtClass cc = getClass(className);
    CtClass superClass = null;
    ArrayList<CtClass> superCCList = new ArrayList<CtClass>();
    if (cc == null) return superCCList;

    try {
      superClass = cc.getSuperclass();
      while(superClass != null) {
        superCCList.add(superClass);
        superClass = superClass.getSuperclass();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return superCCList;
  }


  public CtClass[] getInterfaces(String className) {
    CtClass cc = getClass(className);
    CtClass[] iface = null;
    if (cc == null) return iface;

    try {
      iface = cc.getInterfaces();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return iface;
  }

  public boolean isTargetClass(String className, String targetName) {
    CtClass cc = getClass(className);
    //CtClass superCC = getSuperclass(className);
    //if (cc != null && cc.getName().equals(targetName)) {
    if (cc.getName().equals(targetName)) {
      return true;
    }
    /*else if (superCC != null && superCC.getName().equals(targetName)) {
      return true;
    }*/
    else {
      for (CtClass ci : getAllSuperclass(className)) {
        if (ci.getName().equals(targetName)) {
          return true;
        }
      }
      for (CtClass ci : getInterfaces(className)) {
        if (ci.getName().equals(targetName)) {
          return true;
        }
      }
      for (CtClass ci : getAllSuperclass(className)) {
        for (CtClass sci : getInterfaces(ci.getName().toString())) {
          if (sci.getName().toString().equals(targetName)) {
            return true;
          }
        }
      }
      //it's weird that an iface inherits from another iface.
      return false;
    }
  }

  public boolean isThreadClass(String className) {
    return isTargetClass(className, "java.lang.Thread");
  }

  public boolean isRunnableClass(String className) {
    return isTargetClass(className, "java.lang.Runnable");
  }

}
