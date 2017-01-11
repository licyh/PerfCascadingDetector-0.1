package dm.Util;


import java.io.*;
import java.util.*;

import javassist.*;
import javassist.bytecode.*;

import dm.Util.Bytecode.*;

import com.RPCInfo;

public class MethodUtil {
  CtBehavior method;
  MethodInfo methodInfo;
  ConstPool constPool;
  CodeAttribute codeAttr;
  CodeIterator codeIter;

  int origMaxLocals;
  int origMaxStack;

  SignatureAttribute.MethodSignature methodSig;
  boolean injectFlag = true; //if false, only analysis not injection

  public void setMethod(CtBehavior m) {
    method = m;
    methodInfo = method.getMethodInfo();
    constPool = methodInfo.getConstPool();
    codeAttr = methodInfo.getCodeAttribute();
    codeIter = codeAttr.iterator();
    origMaxLocals = codeAttr.getMaxLocals();
    origMaxStack = codeAttr.getMaxStack();
    try {
      methodSig = SignatureAttribute.toMethodSignature(method.getSignature());
    } catch (BadBytecode e) {
      e.printStackTrace();
    }
  }

  public int paraNum() {
    return methodSig.getParameterTypes().length;
  }
  public String paraI(int index) {
    return methodSig.getParameterTypes()[index].toString();
  }


  /* allocLocal: allocate local variable at the end of original method
   * return: the start index.
   */
  public int allocLocal(int n) {
    int curMaxLocals = codeAttr.getMaxLocals();
    if (origMaxLocals + n > curMaxLocals) {
      codeAttr.setMaxLocals(origMaxLocals + n);
    }
    return origMaxLocals;
  }

  public void allocStack(int n) {
    int curMaxStack = codeAttr.getMaxStack();
    if (origMaxStack + n > curMaxStack) {
      codeAttr.setMaxStack(origMaxStack + n); //+1 for temp stack var. Eg: thd.getId() --> store long to stack.
    }
  }

  public ArrayList<Integer> storePara(Bytecode code, int paraNum, ArrayList<String> paras, int reqStack) {
    int allocLen = paraNum + 1; // +1 for the object instance
    for (int j = 0; j < paraNum; j++) {
      String type = paras.get(j);
      if (type.equals("long") || type.equals("double")) {
        allocLen += 1;
      }
    }
    int localBegin = allocLocal(allocLen);
    allocStack(reqStack);

    ArrayList<Integer> paraLocs = new ArrayList<Integer>();
    int localPos = localBegin;
    for (int j = paraNum - 1; j >= 0; j--) {
      String type = paras.get(j);
      if (type.equals("int") || type.equals("byte") ||
          type.equals("short") || type.equals("char") || type.equals("boolean")) {
        code.addIstore(localPos);
        paraLocs.add(0, localPos);
        localPos++;
      }
      else if (type.equals("float")) {
        code.addFstore(localPos);
        paraLocs.add(0, localPos);
        localPos++;
      }
      else if (type.equals("long")) {
        code.addLstore(localPos);
        paraLocs.add(0, localPos);
        localPos += 2;
      }
      else if (type.equals("double")) {
        code.addDstore(localPos);
        paraLocs.add(0, localPos);
        localPos += 2;
      }
      else {
        code.addAstore(localPos);
        paraLocs.add(0, localPos);
        localPos++;
      }
    }
    code.addAstore(localPos); //the object instance.
    paraLocs.add(0, localPos);
    code.addAload(paraLocs.get(0));

    for (int j=0; j <= paraNum - 1; j++) {
      String type = paras.get(j);
      if (type.equals("int") || type.equals("byte") ||
          type.equals("short") || type.equals("char") || type.equals("boolean")) {
        code.addIload(paraLocs.get(j+1));
      }
      else if (type.equals("float")) {
        code.addFload(paraLocs.get(j+1));
      }
      else if (type.equals("long")) {
        code.addLload(paraLocs.get(j+1));
      }
      else if (type.equals("double")) {
        code.addDload(paraLocs.get(j+1));
      }
      else {
        code.addAload(paraLocs.get(j+1));
      }
    }
    return paraLocs;
  }

  public void insertSyncMethod(String logClassEnter, String logMethodEnter, String logClassExit, String logMethodExit) {
	if (Modifier.toString(method.getModifiers()).contains("static")) {
      //TODO -
	  return;
	}
	if (Modifier.toString(method.getModifiers()).contains("synchronized")) {
      String str = "String opValue = Integer.toString(System.identityHashCode($0)) + \"_2\";";
      str += logClassEnter + "." + logMethodEnter + "(opValue);";
      try { method.insertBefore(str); } catch (Exception e) { e.printStackTrace(); }

      str = "String opValue = Integer.toString(System.identityHashCode($0)) + \"_2\";";
      str += logClassExit + "." + logMethodExit + "(opValue);";
      try { method.insertAfter(str); } catch (Exception e) { e.printStackTrace(); }
    }
  }

  public void insertMonitorInst(String logClassEnter, String logMethodEnter, String logClassExit, String logMethodExit) {
    codeIter = codeAttr.iterator();
    Instruction i = new Instruction();
    i.setMethod(method);
    int cur;
    String logClass, logMethod;
    try {
      while (codeIter.hasNext()) {
        cur = codeIter.next();
        i.setPos(cur);

        boolean isLock = false;
        boolean isUnlock = false;
        if (i.isInvokeinterface()) {
          InvokeInst invokeI = new InvokeInst(i);
          String calledCC = invokeI.calledClass();
          String calledM = invokeI.calledMethod();
          if (calledCC.equals("java.util.concurrent.locks.Lock")) {
            if (calledM.equals("lock") || calledM.equals("tryLock")) { isLock = true; }
            else if (calledM.equals("unlock")) { isUnlock = true; }
          }
        }

        if (i.isMonitor() || isLock || isUnlock) {
          if (i.isMonitorEnter() || isLock) {
            logClass = logClassEnter;
            logMethod = logMethodEnter;
          }
          else { //isMonitorExit() or unlock
            logClass = logClassExit;
            logMethod = logMethodExit;
          }
          int mlFlag = 51; //flag: 1(49):monitor,ie,sync(obj);  2:sync method;  3(51):lock;
          if (i.isMonitor()) { mlFlag = 49; }

          Bytecode code = new Bytecode(constPool);
          allocStack(3);
          /* cannot use storePara at here because monitorenter/exit doesn't include obj.
           */
          int objIndex = allocLocal(1);
          code.addAstore(objIndex);
          code.addAload(objIndex);
          
          code.addNew("java/lang/StringBuilder");
          code.addOpcode(Opcode.DUP);
          code.addInvokespecial("java/lang/StringBuilder", "<init>", "()V");

          code.addAload(objIndex);
          code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
          code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
          code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

          // "_"
          code.addIconst(95);
          code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");
          // "1"
          code.addIconst(mlFlag);
          code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");

          code.addInvokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
          code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
          try {
            int loc = codeIter.insertExAt(cur, code.get());
            codeIter.insert(code.getExceptionTable(), loc);
            methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                            method.getDeclaringClass().getClassFile2());
          } catch (BadBytecode e) {
            System.out.print("Badcode when insert monitor instruction.");
            e.printStackTrace();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  
  //Added by JX
  public void insertRWLock(String logClass, String logMethod) {
    codeIter = codeAttr.iterator();
    Instruction i = new Instruction();
    i.setMethod(method);
    int cur;
    try {
      // get 1st instruction
      if (codeIter.hasNext()) { 
        cur = codeIter.next();
        i.setPos(cur);
      }
      else {
    	System.out.println("JX - method's first bytecode instruction doesn't exist!!");
    	throw new Exception("JX - method's first bytecode instruction doesn't exist!!");
      }

      int numOfRWLocksInOneMethod = 0;    //basically, it is only 1
      
      // scan original bytecode
      while (codeIter.hasNext()) {
        cur = codeIter.next();

        boolean isRWLock = false;
        
        if (i.isInvokespecial()) {
          InvokeInst invokeI = new InvokeInst(i);
          String calledCC = invokeI.calledClass();
          String calledM = invokeI.calledMethod();
          if (calledCC.equals("java.util.concurrent.locks.ReentrantReadWriteLock") && calledM.equals("<init>"))
        	  isRWLock = true;
        }

        if ( isRWLock ) {
          if (++numOfRWLocksInOneMethod > 1) {
            System.out.println( "JX - WARNING - Now the program perhaps cannot deal with multipule RWLokcs in a method!!!" );
            throw new Exception("JX - WARNING - Now the program perhaps cannot deal with multipule RWLokcs in a method!!!");
          }
      /* 
      // JX - testcode
      CodeIterator tmpiter = codeAttr.iterator();
      Instruction j = new Instruction();
      Instruction k;
      j.setMethod(method);
      while (tmpiter.hasNext()) {
    	  int pos = tmpiter.next();
    	  //int opindex = tmpiter.byteAt(pos);
    	  //System.out.println("\t" + Mnemonic.OPCODE[opindex]);
    	  j.setPos( pos );
    	  if (j.isInvoke()) 
    		  k = new InvokeInst( j );
    	  else if (j.isField())
    		  k = new FieldInst( j );
    	  else if (j.isLoad())
    		  k = new LoadInst( j );
    	  else 
    		  k = j;
    	  System.out.println("\t" + k.toString());
      }
      */
          Bytecode code = new Bytecode(constPool);
          // prepare stack and local variables
          allocStack(10);
      /*
      LocalVariableAttribute table = (LocalVariableAttribute)codeAttr.getAttribute(LocalVariableAttribute.tag);
      System.out.println( "JX - numberOfLocalVariables - " + table.tableLength() );
      for (int ii = 0; ii < table.tableLength(); ii++) {
    	  System.out.println( "JX - " + ii + " : " + table.variableName(ii) + " : " + table.descriptor(ii) 
    	  	+ " index=" + table.index(ii) + " nameIndex=" + table.nameIndex(ii) + " descriptorIndex=" + table.descriptorIndex(ii) );
      }
      */
      //System.out.println("JX - maxLocals - " + codeAttr.getMaxLocals());
      	  method.addLocalVariable("rwlock", ClassPool.getDefault().get("java.util.concurrent.locks.ReadWriteLock")); // added: jx: the variable name "rwlock" is just for inserting 'source code'; Here, this is useless.
      	  int rwlockindex = codeAttr.getMaxLocals() - 1;    //jx: if one method has the second RWLock, this will be wrong, because 'MaxLocals' has been changed
      //System.out.println("JX - rwlockindex - " + rwlockindex);
          allocLocal(5);      //jx - will change codeAttr.getMaxLocals(), then affect 'rwlockindex', so cannot move up.
          int objIndex = rwlockindex + 1;
      //System.out.println("JX - objindex - " + objIndex);
      //System.out.println("JX - maxLocals - " + codeAttr.getMaxLocals());
          code.addAstore(objIndex);
          code.addAload(objIndex);
          
          // add ReadWriteLock rwlock = $topOfStack;
          code.addAload(objIndex);
          code.addAstore( rwlockindex );

          // prepare StringBuilder
          code.addNew("java/lang/StringBuilder");
          code.addOpcode(Opcode.DUP);
          code.addInvokespecial("java/lang/StringBuilder", "<init>", "()V");
          // add System.identityHashCode( rwlock )
          code.addAload(rwlockindex);
          code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
          code.addInvokevirtual("java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
          // add '|'
          code.addIconst( 124 );
          code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");
          // add System.identityHashCode( rwlock.readLock() )
          code.addAload(rwlockindex);
          code.addInvokeinterface("java/util/concurrent/locks/ReadWriteLock", "readLock", "()Ljava/util/concurrent/locks/Lock;", 1);          
          code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
          code.addInvokevirtual("java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
          // add '|'
          code.addIconst( 124 );
          code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");
          // add System.identityHashCode( rwlock.writeLock() )
          code.addAload(rwlockindex);
          code.addInvokeinterface("java/util/concurrent/locks/ReadWriteLock", "writeLock", "()Ljava/util/concurrent/locks/Lock;", 1);
          code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
          code.addInvokevirtual("java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
          // log "System.identityHashCode(rwlock) +'|'+ System.identityHashCode(rwlock.readLock()) +'|'+ System.identityHashCode(rwlock.writeLock())"
          code.addInvokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
          code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
          
          try {
            int loc = codeIter.insertExAt(cur, code.get());
            codeIter.insert(code.getExceptionTable(), loc);
            methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                            method.getDeclaringClass().getClassFile2());
          } catch (BadBytecode e) {
            System.out.print("Badcode when insert monitor instruction.");
            e.printStackTrace();
          }
          
        }
        // JX - then set instruction
        i.setPos(cur);
      }//end-while

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  //End-Added
  

  public void insertGetPutInst(String logClassRead, String logMethodRead, String logClassWrite, String logMethodWrite) {
    codeIter = codeAttr.iterator();
    Instruction i = new Instruction();
    i.setMethod(method);
    int cur;
    String logClass, logMethod;
    try {
      while (codeIter.hasNext()) {
        cur = codeIter.next();
        i.setPos(cur);
        if (i.isField()) {
          FieldInst fieldI = new FieldInst(i);
          if (fieldI.isGet()) {
            logClass = logClassRead;
            logMethod = logMethodRead;
          }
          else if (fieldI.isPut()) {
            logClass = logClassWrite;
            logMethod = logMethodWrite;
          }
          else {
            logClass = "";
            logMethod = "";
            System.out.println("Get or Put error, and exit.");
            System.exit(-1);
          }

          Bytecode code = new Bytecode(constPool);
          allocStack(3);

          /* getstatic does not have class information:
           * getstatic     #3  // Field aHep:Ljava/util/HashMap;
           * OPValue: fieldrefClass + "_" + fieldrefName + index
           */
          if (fieldI.isStatic()) {
            String opValue = fieldI.fieldRefClass() + "_" + fieldI.fieldIndex();
            int strIndex = constPool.addStringInfo(opValue);
            code.addLdc(strIndex);
            code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
          }

          /* getfield has object information.
           */
          else {
            int objIndex = -1;
            if (fieldI.isGet()) {
              int tmpLocal = allocLocal(0 + 1); //no para, 1 local for obj
              code.addAstore(tmpLocal);
              code.addAload(tmpLocal);
              objIndex = tmpLocal;
            }
            else if (fieldI.isPut()) {
              ArrayList<String> fieldType = new ArrayList<String>();
              fieldType.add(SignatureAttribute.toTypeSignature(fieldI.fieldRefType()).toString());
              ArrayList<Integer> paraLocs = storePara(code, 1, fieldType, 0); //stack already allocated.
              objIndex = paraLocs.get(0);
            }

            //object id
            code.addNew("java/lang/StringBuilder");
            code.addOpcode(Opcode.DUP);
            code.addInvokespecial("java/lang/StringBuilder", "<init>", "()V");
            code.addAload(objIndex);
            code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
            code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
            code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

            // "_"
            code.addIconst(95);
            code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");

            // offset
            code.addIconst(fieldI.fieldIndex());
            code.addInvokevirtual("java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
            code.addInvokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
            code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
          }

          if (injectFlag) {
            try {
              int loc = codeIter.insertExAt(cur, code.get());
              codeIter.insert(code.getExceptionTable(), loc);
              methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                              method.getDeclaringClass().getClassFile2());
            } catch (BadBytecode e) {
              System.out.print("Field: " + fieldI.fieldRefClass() + "; ");
              System.out.print(fieldI.fieldRefName() + ";");
              System.out.print(fieldI.fieldRefType() + " Static? " + fieldI.isStatic());
              System.out.println(" Put? " + fieldI.isPut() + " IndeX: " + fieldI.fieldIndex());
              e.printStackTrace();
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /* flag:
   * 0: thd enter/exit. opValue = "-"
   * 1: MapReduce event enter/exit. opValue = "$1.hashcode"
   * 2: MapReduce rpc (V2) enter/exit. opValue = $1.toString().hashcode"
   * 3: MapReduce rpc (V1) enter/exit. opValue = the last parameter"
   * 4: MapReduce run/call. opValue = "$0.hashcode"
   *
   * 5: HBase event process enter/exit. opValue = $0.hashcode
   * 6. HBase rpc process enter/exit. opValue = the first parameter."
   * 7. HBase: watchedevent process function enter/exit. opValue = "ZK + event.type() + event.path()
   *    Id conflict.
   *
   * 8. ZK serialize function. send msg. value = $0.get_DM_ID()
   * 9. ZK request process function: value = $0.hashcode
   *
   * 10. MR launch container (start process.)
   */
  public String callStrInstBA (String logClass, String logFunc, int flag) {
    String str = "";
    if (flag == 0) {
      str = logClass + "." + logFunc + "(\"-\");";
    }
    else if (flag == 1) {
      String ccName = method.getDeclaringClass().getName().toString();
      if (ccName.endsWith(".TaskImpl") || ccName.endsWith(".TaskAttemptImpl") ||
          ccName.endsWith(".JobImpl") || ccName.endsWith(".RMNodeImpl") ||
          ccName.endsWith(".ApplicationImpl") || ccName.endsWith(".RMContainerImpl") ||
          ccName.endsWith(".ContainerImpl") || ccName.endsWith(".LocalizedResource") ||
          ccName.endsWith(".RMAppAttemptImpl")) {
        str = "String opValue = Integer.toString(System.identityHashCode($1)) + $1.getType() + \"!\" + stateMachine.getCurrentState() +\"!\" + $0.getClass().getSimpleName();";
      }
      else {
        str = "String opValue = Integer.toString(System.identityHashCode($1)) + $1.getType() + $0.getClass().getSimpleName();";
      }
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 2) {
      str = "String opValue = $1.getDMID();";
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 3) {
      str = "String opValue = $args[$args.length-1].toString();";
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 4) {
      str = "String opValue = Long.toString(Thread.currentThread().getId()) + \"/\" + Integer.toString(System.identityHashCode($0));";
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 5) {
      str = "String opValue = Integer.toString(System.identityHashCode($0));";
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 6) {
      str = "String opValue = $1.toString();";
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 7) {
      str = "String opValue = \"ZK\" + Long.toString(new java.sql.Timestamp(new java.util.Date().getTime()).getTime()) + $1.getType() + $1.getPath();";
      str += logClass + "." + logFunc + "(opValue);";
    }

    else if (flag == 8) {
      str = "String opValue = $0.get_DM_ID();";
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 9) {
      //str = "String opValue = Integer.toString(System.identityHashCode($1)) + \"|\" + Long.toString(new java.sql.Timestamp(new java.util.Date().getTime()).getTime());";
      str = "String opValue = Integer.toString(System.identityHashCode($1)) + \"|\" + Long.toString(System.nanoTime());";
      str += logClass + "." + logFunc + "(opValue);";
    }

    else if (flag == 10) {
      str = "String opValue = Integer.toString(_pid2333);";
      str += logClass + "." + logFunc + "(opValue);";
    }

    else if (flag == 41){
      //str = "String opValue = Integer.toString(System.identityHashCode($1)) + \"_\"+ $2;";
      //CA-Handler-doVerb
      str = "String opValue = $1.getMessageSpecId();";
      //str = "String opValue = $2;";
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 42){
      str = "String opValue = Integer.toString(System.identityHashCode($1));";
      str += logClass + "." + logFunc + "(opValue);";
    }
    else if (flag == 43){
      str = "String opValue = Integer.toString(System.identityHashCode(this.event));";
      str += logClass + "." + logFunc + "(opValue);";
    }
    return str;
  }
  
  public void insertCallInstBefore(String logClass, String logFunc, int flag) {
    try {
      method.insertBefore(callStrInstBA(logClass, logFunc, flag));
    } catch (Exception e) {
      System.out.println("Debug: insert before in method: " + method.getName().toString() +
                      " in cc: " + method.getDeclaringClass().getName().toString() + " flag: " + flag);
      System.out.println("Debug: methodType: " + method.getMethodInfo().getDescriptor());
      System.out.println("Debug: Insert Instruction: " + callStrInstBA(logClass, logFunc, flag));
      e.printStackTrace();
    }
  }

  public void insertCallInstAfter(String logClass, String logFunc, int flag) {
    try {
      if (flag != 10)
          method.insertAfter(callStrInstBA(logClass, logFunc, flag));
      else
          method.insertAt(222 , true,callStrInstBA(logClass, logFunc, flag));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void insertCallInst(String apiClass, String apiMethod, int paraNum,
                  String logClass, String logMethod, ClassUtil classUtil) {
    codeIter = codeAttr.iterator();
    Instruction i = new Instruction();
    i.setMethod(method);
    String curClass = method.getDeclaringClass().getName();
    int cur;
    try {
      while (codeIter.hasNext()) {
        cur = codeIter.next();
        i.setPos(cur);
        if (i.isInvokevirtual() == true || i.isInvokeinterface() == true) {
          InvokeInst invokeI = new InvokeInst(i);
          if (invokeI.calledMethod().equals(apiMethod)) {
            String calledClass = invokeI.calledClass();
            if (classUtil.isTargetClass(calledClass, apiClass)) {
              //allocate local variable space.
              Bytecode code = new Bytecode(constPool);
              ArrayList<Integer> paraLocs = storePara(code, invokeI.paraNum(), invokeI.paraArray(), 5);
              //currentMap requires 2 stack spaces.
              //zk requires 5 stack spaces.

              if (apiClass.equals("java.lang.Thread") &&
                  (apiMethod.equals("start") || apiMethod.equals("join"))
                 ) {
                if (apiMethod.equals("join")) {
                  if (invokeI.paraNum() > 0) {
                    continue; //the case that a.join(100) instead of a.join(). It's not a hard happend-before.
                  }
                }
                int objIndex = paraLocs.get(0);
                code.addAload(objIndex); //for getId()
                code.addInvokevirtual(calledClass, "getId","()J");
                code.addInvokestatic("java/lang/Long", "toString", "(J)Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }
              else if ((apiClass.equals("java.util.concurrent.ThreadPoolExecutor") ||
                        apiClass.equals("java.util.concurrent.ExecutorService") ||
                        apiClass.equals("java.util.concurrent.CompletionService")) &&
                       (apiMethod.equals("execute") || apiMethod.equals("submit"))) {
                int firstIndex = paraLocs.get(1);
                code.addAload(firstIndex);
                code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
                code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }
              else if ((apiClass.equals("java.util.concurrent.ScheduledThreadPoolExecutor")||apiClass.equals("java.util.Timer") ) &&
                       apiMethod.equals("schedule")) {
                int firstIndex = paraLocs.get(1);
                code.addAload(firstIndex);
                code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
                code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }

              else if (apiClass.equals("java.lang.Runtime") &&
                       apiMethod.equals("addShutdownHook")) {
                int firstIndex = paraLocs.get(1);
                code.addAload(firstIndex);
                code.addInvokevirtual("java/lang/Thread", "getId", "()J");
                code.addInvokestatic("java/lang/Long", "toString", "(J)Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }

              /*else if (apiClass.equals("java.util.HashMap") &&
                       (apiMethod.equals("put") || apiMethod.equals("get"))) {*/
              else if (apiClass.equals("java.util.HashMap") ||
                       apiClass.equals("java.util.HashSet") ||
                       apiClass.equals("java.util.Set") ||
                       (apiClass.equals("java.util.Map") &&(!curClass.endsWith("TaskAttemptListenerImpl"))) ||
                       apiClass.equals("com.google.common.collect.BiMap")) {
                int objIndex = paraLocs.get(0);
                code.addNew("java/lang/StringBuilder");
                code.addOpcode(Opcode.DUP);
                code.addInvokespecial("java/lang/StringBuilder", "<init>", "()V");

                code.addAload(objIndex); // for identityHashCode()
                code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
                code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

                if (curClass.endsWith("TaskAttemptListenerImpl")) {
                  if (apiClass.equals("java.util.Set") &&
                      (invokeI.calledMethod().equals("contains") || invokeI.calledMethod().equals("remove") ||
                       invokeI.calledMethod().equals("add"))) {
                    int firstIndex = paraLocs.get(1);
                    code.addAload(firstIndex);
                    code.addInvokevirtual("org/apache/hadoop/mapred/WrappedJvmID", "toString", "()Ljava/lang/String;");
                    code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                  }
                }


                code.addInvokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }

              else if (apiClass.equals("java.util.concurrent.ConcurrentMap") ||
                              (apiClass.equals("java.util.Map") && curClass.endsWith("TaskAttemptListenerImpl"))) { //obj is already protected.
                int objIndex = paraLocs.get(0);
                code.addNew("java/lang/StringBuilder");
                code.addOpcode(Opcode.DUP);
                code.addInvokespecial("java/lang/StringBuilder", "<init>", "()V");
                code.addAload(objIndex);
                code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
                code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

                //record key for getTask in MR.
                if (curClass.endsWith("TaskAttemptListenerImpl")) {
                  if (invokeI.calledMethod().equals("containsKey") || invokeI.calledMethod().equals("remove") ||  invokeI.calledMethod().equals("get") ||
                      invokeI.calledMethod().equals("put")) {
                    int firstIndex = paraLocs.get(1);
                    code.addAload(firstIndex);
                    code.addInvokevirtual("org/apache/hadoop/mapred/WrappedJvmID", "toString", "()Ljava/lang/String;");
                    code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                  }
                }

                // append "c"
                code.addIconst(99);
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");
                code.addInvokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }

              else if (apiClass.equals("org.apache.hadoop.yarn.state.StateMachine") &&
                       apiMethod.equals("doTransition")) {
                int objIndex = paraLocs.get(0);
                code.addAload(objIndex); // for identityHashCode()
                code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
                code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }

              else if (apiClass.equals("org.apache.hadoop.hbase.executor.ExecutorService") &&
                       apiMethod.equals("submit")) {
                int firstIndex = paraLocs.get(1);
                code.addAload(firstIndex); // for identityHashCode()
                code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
                code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }

              //zk for heapread/heapwrite
              else if (apiClass.equals("org.apache.zookeeper.ZooKeeper") &&
                       (logMethod.endsWith("HeapRead") || logMethod.endsWith("HeapWrite"))) {
                int objIndex = paraLocs.get(0);
                code.addNew("java/lang/StringBuilder");
                code.addOpcode(Opcode.DUP);
                code.addInvokespecial("java/lang/StringBuilder", "<init>", "()V");
                code.addAload(objIndex);
                code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
                code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");


                String mName = invokeI.calledMethod();
                if (mName.equals("create") || mName.equals("delete") || mName.equals("setData") || mName.equals("exists") ||
                    mName.equals("getData") || mName.equals("getChildren")) {
                  int firstIndex = paraLocs.get(1);
                  code.addAload(firstIndex);
                  code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                }

                // append "z"
                code.addIconst(122);
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");
                code.addInvokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }

              //zk for watched-event creating.
              else if (apiClass.equals("org.apache.zookeeper.ZooKeeper") &&
                       logMethod.endsWith("MsgSending")) {
                int firstIndex = paraLocs.get(1);
                code.addNew("java/lang/StringBuilder");
                code.addOpcode(Opcode.DUP);
                code.addInvokespecial("java/lang/StringBuilder", "<init>", "()V");

                // append "ZK"
                code.addIconst(90);
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");
                code.addIconst(75);
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");

                //append localtime
                code.addNew("java/sql/Timestamp");
                code.addOpcode(Opcode.DUP);
                code.addNew("java/util/Date");
                code.addOpcode(Opcode.DUP);
                code.addInvokespecial("java/util/Date", "<init>", "()V");
                code.addInvokevirtual("java/util/Date", "getTime", "()J");
                code.addInvokespecial("java/sql/Timestamp", "<init>", "(J)V");
                code.addInvokevirtual("java/sql/Timestamp", "getTime", "()J");
                code.addInvokestatic("java/lang/Long", "toString", "(J)Ljava/lang/String;");
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

                //append method
                int strIndex = constPool.addStringInfo(apiMethod);
                code.addLdc(strIndex);
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

                code.addAload(firstIndex); // for identityHashCode()
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                code.addInvokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }

              //For ZK
              /*else if (apiClass.equals("org.apache.zookeeper.server.RequestProcessor") &&
                       apiMethod.equals("processRequest")) {
                //create event
                int firstIndex = paraLocs.get(1);
                code.addNew("java/lang/StringBuilder");
                code.addOpcode(Opcode.DUP);
                code.addInvokespecial("java/lang/StringBuilder", "<init>", "()V");

                code.addAload(firstIndex); // for identityHashCode()
                code.addInvokestatic("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
                code.addInvokestatic("java/lang/Integer", "toString", "(I)Ljava/lang/String;");
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");

                code.addIconst(124);
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;");

                //append localtime
                code.addNew("java/sql/Timestamp");
                code.addOpcode(Opcode.DUP);
                code.addNew("java/util/Date");
                code.addOpcode(Opcode.DUP);
                code.addInvokespecial("java/util/Date", "<init>", "()V");
                code.addInvokevirtual("java/util/Date", "getTime", "()J");
                code.addInvokespecial("java/sql/Timestamp", "<init>", "(J)V");
                code.addInvokevirtual("java/sql/Timestamp", "getTime", "()J");
                code.addInvokestatic("java/lang/Long", "toString", "(J)Ljava/lang/String;");
                code.addInvokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
                code.addInvokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

                code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              }*/

              if (injectFlag) {
                try {
                  int loc = codeIter.insertExAt(cur, code.get());
                  codeIter.insert(code.getExceptionTable(), loc);
                  methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                                  method.getDeclaringClass().getClassFile2());
                } catch (BadBytecode e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void insertRPCInvoke(String logClass, String logMethod) {
    codeIter = codeAttr.iterator();
    Instruction i = new Instruction();
    i.setMethod(method);
    String curClass = method.getDeclaringClass().getName();
    
    //other classes are IRC or un-wanted classes
    if (curClass.contains("ClientServiceDelegate") == false) {
      return;
    }

    /* only based the invoke callinst, we cannot get the class.
     * record the last ldc_w index as the class.
     * see the pattern in ClientServiceDelegate
     */
    int cur, classIndex = -1;
    try {
      while (codeIter.hasNext()) {
        cur = codeIter.next();
        i.setPos(cur);
        if (i.isLDCW()) {
          classIndex = codeIter.u16bitAt(cur + 1);
        }

        if (i.isInvokevirtual() == true || i.isInvokeinterface() == true ||
            i.isInvokespecial() == true) {
          InvokeInst invokeI = new InvokeInst(i);
          String calledClass = invokeI.calledClass();
          if (calledClass.equals("java.lang.reflect.Method") == false) {
            if (invokeI.calledMethod().equals("invoke")) {
              //System.out.println("EEEE invoke: " + invokeI.calledMethodType());
              //System.out.println("EEEE invoke: " + calledClass);
              //System.out.println("EEEE invoke: " + method.getName().toString() + " in cc: " + method.getDeclaringClass().getName().toString());
              Bytecode code = new Bytecode(constPool);
              allocLocal(3);
              allocStack(1);

              if (classIndex == -1) {
                System.out.println("Cannot find invoke rpc class type. Exit.");
                System.exit(-1);
              }
              code.addOpcode(Opcode.DUP);
              code.addInvokeinterface(classIndex, "getDMID", "()Ljava/lang/String;", 1);
              code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
              classIndex = -1;

              if (injectFlag) {
                try {
                  int loc = codeIter.insertExAt(cur, code.get());
                  codeIter.insert(code.getExceptionTable(), loc);
                  methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                                  method.getDeclaringClass().getClassFile2());
                } catch (BadBytecode e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void insertRPCCallInst(String logClass, String logMethod, RPCInfo rpcInfo) {
    codeIter = codeAttr.iterator();
    Instruction i = new Instruction();
    i.setMethod(method);
    String curClass = method.getDeclaringClass().getName();

    int cur;
    try {
      while (codeIter.hasNext()) {
        cur = codeIter.next();
        i.setPos(cur);
        if (i.isInvokeinterface() == true) {
          InvokeInst invokeI = new InvokeInst(i);
          String calledClass = invokeI.calledClass();
          if (rpcInfo.isRPCCall(calledClass, invokeI.calledMethod())) {
            //allocate local variable space.
            Bytecode code = new Bytecode(constPool);
            //int paraNum = rpcInfo.getNumPara(calledClass, invokeI.calledMethod());
            int paraNum = invokeI.paraNum();
            //paraNum >0; filter when read rpc.txt in RPCInfo class.
            ArrayList<Integer> paraLocs = storePara(code, paraNum, invokeI.paraArray(), 1);

            if (rpcInfo.getVersion(calledClass, invokeI.calledMethod()) == 2 &&
                rpcInfo.isTargetAPP(calledClass, invokeI.calledMethod(), "MR")) {
              //paraNum = 1
              int firstParaIndex = paraLocs.get(1); //0: the obj
              code.addAload(firstParaIndex); // for identityHashCode()
              code.addInvokeinterface(invokeI.paraI(0), "getDMID", "()Ljava/lang/String;", 1);
              code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
            }
            else if (rpcInfo.getVersion(calledClass, invokeI.calledMethod()) == 1 &&
                     rpcInfo.isTargetAPP(calledClass, invokeI.calledMethod(), "MR")) { //mrv1
              int lastParaIndex = paraLocs.get(paraNum);
              code.addAload(lastParaIndex); //the last parameter. getDMID() in MR.
              code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
            }
            else if (rpcInfo.isTargetAPP(calledClass, invokeI.calledMethod(), "HB")) {
              int firstParaIndex = paraLocs.get(1);
              code.addAload(firstParaIndex); //the first parameter. getDMID() in MR.
              code.addInvokestatic(logClass, logMethod, "(Ljava/lang/String;)V");
            }

            if (injectFlag) {
              try {
                int loc = codeIter.insertExAt(cur, code.get());
                codeIter.insert(code.getExceptionTable(), loc);
                methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                                method.getDeclaringClass().getClassFile2());
              } catch (BadBytecode e) {
                System.out.println("Debug: m: " + method.getName() + " in cc: " + method.getDeclaringClass().getName());
                e.printStackTrace();
              }
            }
          }
        }
      }
   } catch (Exception e) {
      e.printStackTrace();
    }
  }
  /*public void insertThdCrtCallInstAt(String methodName, ClassUtil classUtil) {
    // insert thread create logFunc call inst:
    // 1. copy aload to get the object of Thread
    // 2. insert invokevirtual instruction.
    //
    codeIter = codeAttr.iterator();
    int pred = 0, cur = 0;

    Instruction i = new Instruction();
    i.setMethod(method);
    try {
      while (codeIter.hasNext()) {
        pred = cur;
        cur = codeIter.next();
        i.setPos(cur);
        if (i.isInvokevirtual() == true) {
          InvokeInst invokeI = new InvokeInst(i);
          if (invokeI.calledMethod().equals("start")) {
            String calledClass = invokeI.calledClass();
            if (classUtil.isThreadClass(calledClass)) {
              i.setPos(pred);
              if (i.isLoad() == false) {
                System.out.println("Not an aload before thd.start(). Exit the execution. Check it.");
                System.exit(-1);
              }
              LoadInst loadI = new LoadInst(i);
              int thdObjIndex = loadI.getIndex();

              Bytecode code = new Bytecode(constPool);
              code.addAload(thdObjIndex); //for call logFunc()
              code.addAload(thdObjIndex); //for getId()
              code.addInvokevirtual(calledClass, "getId","()J");
              code.addInvokestatic("java/lang/Long", "toString", "(J)Ljava/lang/String;");
              code.addInvokevirtual(calledClass, methodName, "(Ljava/lang/String;)V");
              try {
                int loc = codeIter.insertExAt(pred, code.get());
                codeIter.insert(code.getExceptionTable(), loc);
                methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                                method.getDeclaringClass().getClassFile2());
              } catch (BadBytecode e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/



  /*public void insertHeapCallInst(ClassUtil classUtil, int paraNum) {
    codeIter = codeAttr.iterator();
    Instruction i = new Instruction();
    i.setMethod(method);

    int cur = 0, pred = 0, pred_pred = 0, pred_pred_p = 0;;
    try {
      while (codeIter.hasNext()) {
        pred_pred_p = pred_pred;
        pred_pred = pred;
        pred = cur;
        cur = codeIter.next();
        i.setPos(cur);
        if (i.isInvokevirtual() == true) {
          InvokeInst invokeI = new InvokeInst(i);
          if (invokeI.calledMethod().equals("put")) {
            System.out.println("Put: " + cur);
            System.out.println("Put pred: " + pred_pred_p);
            System.out.println("SrcLine: " + methodInfo.getLineNumber(cur));

            Bytecode code = new Bytecode(constPool);
            int tmpLocal = allocLocal(paraNum + 1); //+1 for the obj.
            for (int j=0; j < paraNum + 1; j++) {
              code.addAstore(tmpLocal + j);
            }
            for (int j=paraNum; j >= 0; j--) {
              code.addAload(tmpLocal + j);
            }

            CtClass stream = classUtil.getClassPool().get("java.lang.System");
            code.addGetstatic(stream, "out", "Ljava/io/PrintStream;");
            code.addAload(tmpLocal + paraNum);

            code.addInvokestatic("java.lang.System", "identityHashCode", "(Ljava/lang/Object;)I");
            code.addInvokevirtual("java/io/PrintStream", "println", "(I)V");


            try {
              int loc = codeIter.insertExAt(cur, code.get());
              codeIter.insert(code.getExceptionTable(), loc);
              methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                              method.getDeclaringClass().getClassFile2());
            } catch (BadBytecode e) {
              e.printStackTrace();
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/


  public void print() {
    InstructionPrinter printer = new InstructionPrinter(System.out);
    printer.print((CtMethod)method);
  }

}

