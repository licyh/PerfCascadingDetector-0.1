package dt;

import java.io.*;
import java.util.*;
import org.apache.commons.cli.*;

public class DMOption {
  String argStr;
  String delimiter;
  String[] args;

  CommandLineParser parser;
  CommandLine cmd;
  Options options;

  public DMOption(String str) {
    argStr = str;
    parser = new BasicParser();
    options = new Options();
  }

  public void setDelimiter(String str) {
    delimiter = str;
    args = argStr.split(delimiter);
  }

  public void addOption(String c) {
    addOption(c, "xxx");
  }
  public void addOption(String c, String str) {
    addOption(c, str, "xxx");
  }
  public void addOption(String c, String str, String des) {
    options.addOption(c, str, true, des);
  }

  public void parse() {
    try{
      cmd = parser.parse(options, args);
    } catch(ParseException e) {
      e.printStackTrace();
    }
  }

  public String getValue(String option) {
    return cmd.getOptionValue(option);
  }

  public String readOptionFile(String option) {
    String file = getValue(option);
    String funcList = null; //seperate by space
    try {
      String line = null;
      FileReader fileReader = new FileReader(file);
      BufferedReader buffer = new BufferedReader(fileReader);
      while ((line = buffer.readLine()) != null) {
        if (line.contains(" ") == true) {
          System.out.println("Error in the function list file. Exit!");
          System.exit(-1);
        }
        funcList += line + " ";
      }
      buffer.close();
    } catch(Exception e) { e.printStackTrace(); }
    return funcList;
  }

}

