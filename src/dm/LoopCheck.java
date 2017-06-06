package dm;

import java.io.*;
import java.util.*;
import java.security.*;
import java.lang.instrument.*;

import javassist.*;
import javassist.bytecode.*;

import dm.util.Bytecode.*;
import dm.util.Bytecode.Instruction;
import dm.util.Bytecode.InvokeInst;
import dm.util.ClassUtil;
import dm.util.MethodUtil;
import com.APIInfo;
import com.API;
import com.RPCInfo;
import com.CalleeInfo;


public class LoopCheck {

  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("Agent arguments: " + agentArgs);
    inst.addTransformer(new LoopCheckTransformer(agentArgs));

  }
}

class LoopCheckTransformer extends Transformer {
  ClassUtil classUtil;
  APIInfo apiInfo = new APIInfo();
  ArrayList<API> apiRead = new ArrayList<API>();
  ArrayList<API> apiWrite = new ArrayList<API>();

  public LoopCheckTransformer(String str) {
    super(str);
    option.setDelimiter("%");
    option.addOption("s", "searchScope", "search path");
    option.parse();

    //-s parameter
    classUtil = new ClassUtil();
    classUtil.setSearchScope(option.getValue("s"));

    //api file
    apiInfo.setInfoFilePath("resource/api.txt");
    apiInfo.readFile();
    apiRead = apiInfo.allReadAPI();
    apiWrite = apiInfo.allWriteAPI();
  }

  public void transformMethod(CtClass cl, CtBehavior method) {

    /*check class*/
    String op1 = "";
    ArrayList<Operation> ops = new ArrayList<Operation>();
    Operation tmp = null;

    /* zk-1270*/
    //1225 & 2357
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "waitForEpochAck", 812);
    ops.add(tmp);
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "waitForEpochAck", 818);
    ops.add(tmp);

    //1199 & 2406
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "lead", 331);
    ops.add(tmp);
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "processAck", 477);
    ops.add(tmp);

    //1181 & 2320
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "getEpochToPropose", 785);
    ops.add(tmp);
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "getEpochToPropose", 779);
    ops.add(tmp);

    //2420 & 3332
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "processAck", 511);
    ops.add(tmp);
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "processAck", 477);
    ops.add(tmp);

    /* zk-1144 */
    //2518 & 2992
    tmp = new Operation("org.apache.zookeeper.server.quorum.flexible.QuorumMaj", "containsQuorum", 54);
    ops.add(tmp);
    tmp = new Operation("org.apache.zookeeper.server.quorum.Leader", "processAck", 499);
    ops.add(tmp);

    /* CA-1011 */
    //1019 & 3235
    //254 & 3235
    tmp = new Operation("org.apache.cassandra.gms.Gossiper", "getStateForVersionBiggerThan", 443);
    ops.add(tmp);
    tmp = new Operation("org.apache.cassandra.gms.Gossiper", "handleMajorStateChange", 614);
    ops.add(tmp);

    //52 & 5666
    //14059 & 16129
    //8138 & 9946
    tmp = new Operation("org.apache.cassandra.locator.TokenMetadata", "updateNormalToken", 109);
    ops.add(tmp);
    tmp = new Operation("org.apache.cassandra.locator.TokenMetadata", "sortedTokens", 316);
    ops.add(tmp);

    /* MR-4637 */
    //403 & 2827
    tmp = new Operation("org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler", "addApplication", 300);
    ops.add(tmp);
    tmp = new Operation("org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler", "getApplication", 280);
    ops.add(tmp);

    tmp = new Operation("org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler", "doneApplication", 340);
    ops.add(tmp);
    tmp = new Operation("org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler", "getApplication", 280);
    ops.add(tmp);

    /* MR- 3274 */
    // 1760 & 2658 and the same group races
    tmp = new Operation("org.apache.hadoop.yarn.server.resourcemanager.scheduler.AppSchedulingInfo", "getResourceRequest", 180);
    ops.add(tmp);
    tmp = new Operation("org.apache.hadoop.yarn.server.resourcemanager.scheduler.AppSchedulingInfo", "allocateRackLocal", 271);
    ops.add(tmp);

    //20765 & 23357
    tmp = new Operation("org.apache.hadoop.mapred.TaskAttemptListenerImpl", "register", 437);
    ops.add(tmp);
    tmp = new Operation("org.apache.hadoop.mapred.TaskAttemptListenerImpl", "getTask", 419);
    ops.add(tmp);
    
    //21165 & 23357
    tmp = new Operation("org.apache.hadoop.mapred.TaskAttemptListenerImpl", "unregister", 452);
    ops.add(tmp);
    tmp = new Operation("org.apache.hadoop.mapred.TaskAttemptListenerImpl", "getTask", 419);
    ops.add(tmp);

    /* MR-4744, same as in MR-3274, line num doesn't matter. */

    /* HB-4539 */
    tmp = new Operation("org.apache.hadoop.hbase.zookeeper.RecoverableZooKeeper", "delete", 107);
    ops.add(tmp);
    tmp = new Operation("org.apache.hadoop.hbase.zookeeper.RecoverableZooKeeper", "getData", 305);
    ops.add(tmp);

    /* HB-4729 */
    tmp = new Operation("org.apache.hadoop.hbase.zookeeper.RecoverableZooKeeper", "delete", 107);
    ops.add(tmp);
    tmp = new Operation("org.apache.hadoop.hbase.zookeeper.RecoverableZooKeeper", "setData", 372);
    ops.add(tmp);

    //260 & 1774
    tmp = new Operation("org.apache.hadoop.hbase.util.FSTableDescriptors", "get", 166);
    ops.add(tmp);
    tmp = new Operation("org.apache.hadoop.hbase.util.FSTableDescriptors", "get", 151);
    ops.add(tmp);


    boolean flag = false;
    String methodName = method.getName().toString();
    String className = cl.getName().toString();
    for (Operation op : ops) {
      if (op.cc.equals(className) && op.method.equals(methodName)) {
        flag = true;
        break;
      }
    }
    if (flag == false) { return; }


    String logClass = "_DM_Log";
    String logFuncPre = "log";

    String heapReadLog        = logFuncPre + "_" + "HeapRead";
    String heapWriteLog       = logFuncPre + "_" + "HeapWrite";

    classUtil.setClassPool(method);
    classUtil.updateClassPool();
    MethodUtil methodUtil = new MethodUtil();
    methodUtil.setMethod(method);


      /* heap read */
    for (API apiI : apiRead) {
      methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapReadLog, classUtil);
    }

    /* heap write */
    for (API apiI : apiWrite) {
      methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapWriteLog, classUtil);
    }

    /* primitive type static/field read/write. */
    methodUtil.insertGetPutInst(logClass, heapReadLog, logClass, heapWriteLog);
  }
}

class Operation {
  public String cc;
  public String method;
  public int line;
  Operation (String cc_, String m_, int line_) {
    cc = cc_;
    method = m_;
    line = line_;
  }
}
