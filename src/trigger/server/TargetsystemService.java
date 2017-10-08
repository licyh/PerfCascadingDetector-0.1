package trigger.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;

/**
 * Created by hadoop on 6/3/16.
 */
public class TargetsystemService extends TService {

    final static Logger LOG = LoggerFactory.getLogger(TargetsystemService.class);

    String syst;
    String workload;
    int time; // waiting time between two
    Process sp; //process for system of D-Software
    Process wp; //process for workload of D-Software
    public TargetsystemService(String Sys, String Workload,String t){
        this.syst = Sys;
        this.workload = Workload;
        this.isfinish = false;
        if (t.equals(""))
            this.time = 5000;
        else
            this.time = Integer.parseInt(t);
        LOG.info("Target system service is created");
    }

    public void run(){
        LOG.info("Target system service begins to run");
        try {
            LOG.info("Launch "+this.syst);
            sp = Runtime.getRuntime().exec("/bin/bash "+this.syst);
            Thread.sleep(this.time);
            //BufferedReader reader = new BufferedReader(new InputStreamReader(sp.getInputStream()));

            LOG.info("Launch "+this.workload);
            wp = Runtime.getRuntime().exec("/bin/bash "+this.workload);
            System.out.println(new Date());
            sp.waitFor();
            System.out.println(new Date());
            wp.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(sp.getInputStream()));
            String s;
            while ((s = reader.readLine()) != null) {
                System.out.println("System output: " + s);
            }
            reader = new BufferedReader(new InputStreamReader(wp.getInputStream()));
            while ((s = reader.readLine()) != null) {
                System.out.println("Workload output: " + s);
            }
        }catch (Exception e){
            LOG.info("Target System or workload cannot start");
        }
        isfinish = true;
    }

}
