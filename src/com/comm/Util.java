package com.comm;

import java.io.*;
import java.util.*;

public class Util {

  public static String readLine(BufferedReader in) {
    String str = "";
    do {
      try {
        str = in.readLine();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } while(str != null && str.startsWith("//"));
    return str;
  }

  public static void exitWithMsg (String str) {
    System.out.println("Exit with msg: " + str);
    System.exit(-1);
  }

  public static boolean strEqual (String str1, String str2) {
    if (str1.equals("*") || str2.equals("*")) return true;
    if (str1.equals("#") || str2.equals("#")) return false;

    if (str1.startsWith("^") && str2.startsWith("^")) {
      exitWithMsg("Compare two strs begin with '^'.");
      return false;
    }
    else if (str1.startsWith("^")) return str2.startsWith(str1.substring(1));
    else if (str2.startsWith("^")) return str1.startsWith(str2.substring(1));
    else return str1.equals(str2);
  }

}
