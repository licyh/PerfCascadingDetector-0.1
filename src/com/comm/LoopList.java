package com.comm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Created by wangxuact on 3/20/17.
 */
public class LoopList {

    String filePath;
    ArrayList<Loop> loopList = new ArrayList<Loop>();
    ArrayList<Loop> exclusiveLoop = new ArrayList<Loop>();
    public LoopList(String path) { filePath = path; }
    public void init() {
        try {
            BufferedReader buf = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = Util.readLine(buf)) != null) {

                if(line.startsWith("^"))
                {
                    Loop tmpe = new Loop(line);
                    exclusiveLoop.add(tmpe);
                }
                else {
                    Loop tmp = new Loop(line);
                    boolean isExclusive = false;

                    for(Loop tlp:exclusiveLoop)
                    {
                        if(tlp.equal(tmp.className,tmp.methodName,tmp.sig))
                        {
                            isExclusive = true;
                        }
                    }
                    if(!isExclusive) {
                        loopList.add(tmp);
                    }
                }
            }
            buf.close();
        } catch (Exception e) {e.printStackTrace();}
    }

    public boolean isLoop (String cName, String mName, String sig) {
        for (Loop i : loopList) {
            if (i.equal(cName, mName, sig)) return true;
        }
        return false;
    }

    public boolean isLoopMethod(String cName, String mName)
    {
        for (Loop i : loopList) {
            if (i.equal(cName, mName, "*")) return true;
        }
        return false;
    }
}
