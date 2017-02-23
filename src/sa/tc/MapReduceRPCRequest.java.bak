package sa.tc;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;


public class MapReduceRPCRequest { 
  public static String format(String str) {
    String rt = str;
    if (rt.startsWith("L")) {
      rt = rt.substring(1);
    }
    rt = rt.replaceAll("/", ".");
    return rt;
  }

  public static boolean containMethod(IClass c, String method) {
    boolean rt = false;
    for (IMethod im : c.getDeclaredMethods()) {
      if (im.getName().toString().equals(method)) {
        rt = true;
      }
    }
    return rt;
  }

  public static void main(String[] args) {
    //String path = "/mnt/storage/haopeng/workstation/java-ws/DC-Detector/mr_test/hadoop-0.23.1-install/share/hadoop/";
    String path = "src/sa/res/MapReduce/hadoop-0.23.3/";
    ArrayList<String> pathList = new ArrayList<String>();
    //pathList.add(path + "common");
    //pathList.add(path + "mapreduce");
    pathList.add(path);

    AnalysisScope scope = null;;
    ClassHierarchy cha = null;
    ArrayList<IClass> classList = new ArrayList<IClass>();
    ArrayList<IClass> requestClassList = new ArrayList<IClass>();
    ArrayList<String> requestNameList = new ArrayList<String>();
    ArrayList<IClass> responseClassList = new ArrayList<IClass>();
    ArrayList<String> responseNameList = new ArrayList<String>();
    ArrayList<IClass> otherClassList = new ArrayList<IClass>();
    ArrayList<String> otherNameList = new ArrayList<String>();
    try {
      for (String pathi : pathList) {
        File jarDIR = new File(pathi);
        System.out.println( jarDIR.toString() );
        for (File f : jarDIR.listFiles()) {
          if (f.isFile()) {
            if (f.getName().contains("test")) { continue; }
            if (scope == null) {
              scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(f.getAbsolutePath(), (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
            }
            else {
            	System.out.println( "JX-" + f.getAbsolutePath() );
              scope.addToScope(ClassLoaderReference.Application, new JarFile(f.getAbsolutePath()));
            }
            System.out.println(f.getName());
          }
        }
      }
      cha = ClassHierarchy.make(scope);
      for (IClass c : cha) {
        if (c.getName().toString().startsWith("Lorg/apache/") == false) { continue; }
        classList.add(c);
        /* if the class is a subclass of ProtoBase.*/
        if (c.getSuperclass().getName().toString().endsWith("ProtoBase")) {
          if (c.getName().toString().contains("Request")) {
            requestClassList.add(c);
            requestNameList.add(c.getName().toString());
          }
          else if (c.getName().toString().contains("Response")) {
            responseClassList.add(c);
            responseNameList.add(c.getName().toString());
          }
          else { //this class is not a direct rpc related class.
            if (c.getName().toString().endsWith("LocalizerStatusPBImpl")) {
              otherClassList.add(c);
              otherNameList.add(c.getName().toString());
            }
          }
        }
      }
    } catch (WalaException|IOException e) {
      e.printStackTrace();
    }

    //prune request and response
    Iterator<IClass> iter = requestClassList.iterator();
    while (iter.hasNext()) {
      IClass c = iter.next();
      String name = c.getName().toString();
      String responseName = name.replaceAll("Request", "Response");
      if (responseNameList.contains(responseName) == false) {
        iter.remove();
        requestNameList.remove(name);
      }
    }
    iter = responseClassList.iterator();
    while (iter.hasNext()) {
      IClass c = iter.next();
      String name = c.getName().toString();
      String requestName = name.replaceAll("Response", "Request");
      if (requestNameList.contains(requestName) == false) {
        iter.remove();
        responseNameList.remove(name);
      }
    }

    // get interface
    ArrayList<String> requestIface = new ArrayList<String>();
    for (IClass c : requestClassList) {
      for (IClass iface : c.getAllImplementedInterfaces()) {
        requestIface.add(iface.getName().toString());
      }
    }

    ArrayList<String> otherIface = new ArrayList<String>();
    for (IClass c : otherClassList) {
      for (IClass iface : c.getAllImplementedInterfaces()) {
        otherIface.add(iface.getName().toString());
        System.out.println(" debug iface: " + iface.getName());
      }
    }

    /* need manually add register & getTask & unregister for mr-3274?
    for (IClass c : cha) {
      if (c.getName().toString().contains("TaskAttemptListenerImpl")) {
        for (IMethod m : c.getDeclaredMethods()) {
          System.out.println(c.getName().toString() +" " + m.getName() + "" + m.getDescriptor().toString());
        }
      }
    }
    System.exit(-1);*/
        

    // get each rpc function and its class.
    ArrayList<String> out = new ArrayList<String>();


    for (IClass c : classList) {
      if (c.getName().toString().startsWith("Lorg/apache/") == false) { continue; }
      if (c.getName().toString().contains("$")) { continue; } // private class
      if (c.getName().toString().contains("ClientImpl")) { continue; } // client impl

      for (IMethod m : c.getDeclaredMethods()) {
        if (m.isAbstract() == true) { continue; } // abstrct method
        if (m.getNumberOfParameters() != 2) { continue; }

        String paraTy = m.getParameterType(1).toString(); // format: <Application, Lorg/.../Class>
        paraTy = paraTy.substring(paraTy.lastIndexOf(",")+1, paraTy.length()-1);
        if (requestIface.contains(paraTy)){
          String str = c.getName().toString();
          String outStr;
          str = MapReduceRPCRequest.format(str);
          System.out.println(str + " ");
          outStr = str + " ";
          for (IClass iface : c.getAllImplementedInterfaces()) {
            if (MapReduceRPCRequest.containMethod(iface, m.getName().toString()) == true) {
              str = iface.getName().toString();
              str = MapReduceRPCRequest.format(str);
              System.out.println(str + " ");
              outStr += str + " ";
            }
          }
          str = m.getName().toString();
          str = MapReduceRPCRequest.format(str);
          System.out.println(str);
          outStr += str + " ";
          outStr += "1 Ljava/lang/Object";
          out.add(outStr);
        }
        else if (otherIface.contains(paraTy)) {
          System.out.println("Method: " + m.getName() + " in cc: " + c.getName());
        }

      }
    }
    System.exit(-1);

    String filePath = "../../src/com/mr_rpc.txt_tmp";
    try {
      PrintWriter outFile = new PrintWriter(filePath, "UTF-8");
      outFile.println("//format: class iface method");
      for (String str : out) {
        outFile.println(str);
      }
      outFile.close();
    } catch (Exception e) {
      e.printStackTrace();
    }


    System.out.println("MRV1 RPC");
    //check mrv1 rpc
    ArrayList<IClass> mrv1Iface = new ArrayList<IClass>();
    ArrayList<IClass> mrv1Class = new ArrayList<IClass>();
    for (IClass c : cha) {
      String className = c.getName().toString();
      if (className.startsWith("Lorg/apache/hadoop/mapred/") == false &&
          className.startsWith("Lorg/apache/hadoop/mapreduce/") == false &&
          className.startsWith("Lorg/apache/hadoop/yarn/") == false) {
        continue;
      }
      if (className.contains("Local") ||
          className.contains("Avro")) {
        continue;
      }
      
      for (IClass cc : c.getAllImplementedInterfaces()) {
        if (cc.getName().toString().endsWith("VersionedProtocol")) {
          if (c.isInterface()) {
            mrv1Iface.add(c);
          }
          else {
            mrv1Class.add(c);
          }
          break;
        }
      }
    }


    ArrayList<String> mrv1Out = new ArrayList<String>();
    for (IClass c : mrv1Class) {
      String str;
      System.out.println(MapReduceRPCRequest.format(c.getName().toString()));
      for (IClass cc : c.getAllImplementedInterfaces()) {
        if (mrv1Iface.contains(cc)) {
          System.out.println(MapReduceRPCRequest.format(cc.getName().toString()));

          for (IMethod m : cc.getDeclaredMethods()) {
            str = MapReduceRPCRequest.format(c.getName().toString()) + " ";
            str += MapReduceRPCRequest.format(cc.getName().toString()) + " ";
            str += m.getName().toString() + " ";
            str += (m.getNumberOfParameters()-1) + " ";
            for (int i=1; i < m.getNumberOfParameters(); i++) {
              if (m.getParameterType(i).isReferenceType()) {
                str += m.getParameterType(i).getName() + " ";
                //str += "Ljava/lang/Object ";
              }
              else {
                str += m.getParameterType(i).getName() + " ";
              }
            }
            mrv1Out.add(str);
            System.out.println(m.getName());
          }
        }
      }
    }

    filePath = "../../src/com/mr_rpc_v1.txt_tmp";
    try {
      PrintWriter outFile = new PrintWriter(filePath, "UTF-8");
      outFile.println("//format: class iface method");
      for (String str : mrv1Out) {
        outFile.println(str);
      }
      outFile.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    /*System.out.println("#.Request = " + requestClassList.size());
    System.out.println("#.Response = " + responseClassList.size());
    String dir = "../../src/com/";
    String requestCFile = dir + "mr_rpc_request_class.txt";
    String requestIFile = dir + "mr_rpc_request_iface.txt";
    String responseCFile = dir + "mr_rpc_response_class.txt";
    String responseIFile = dir + "mr_rpc_response_iface.txt";
    try {
      PrintWriter reqCWriter = new PrintWriter(requestCFile, "UTF-8");
      PrintWriter reqIWriter = new PrintWriter(requestIFile, "UTF-8");
      PrintWriter resCWriter = new PrintWriter(responseCFile, "UTF-8");
      PrintWriter resIWriter = new PrintWriter(responseIFile, "UTF-8");
      for (IClass c : requestClassList) {
        String cName = MapReduceRPCRequest.format(c.getName().toString());
        reqCWriter.println(cName);
        for (IClass ifaceCC : c.getAllImplementedInterfaces()) {
          String ifaceName = MapReduceRPCRequest.format(ifaceCC.getName().toString());
          reqIWriter.println(ifaceName);
        }
      }
      for (IClass c : responseClassList) {
        String cName = MapReduceRPCRequest.format(c.getName().toString());
        resCWriter.println(cName);
        for (IClass ifaceCC : c.getAllImplementedInterfaces()) {
          String ifaceName = MapReduceRPCRequest.format(ifaceCC.getName().toString());
          resIWriter.println(ifaceName);
        }
      }
      reqCWriter.close();
      reqIWriter.close();
      resCWriter.close();
      resIWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }*/
  }
}
