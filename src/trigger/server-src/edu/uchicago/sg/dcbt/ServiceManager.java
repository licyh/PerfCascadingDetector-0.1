package edu.uchicago.sg.dcbt;

/**
 * Created by hadoop on 6/3/16.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;


public class ServiceManager {

    final static Logger LOG = LoggerFactory.getLogger(ServiceManager.class);

    ArrayList<TService> services;

    //ThreadPoolExecutor threadPoolExecutor;

    public static void main (String[] argv){
        // only one parameter accepted which is the config file
        if (argv.length < 1){
            System.out.println("Please specify the configure file!");
            return ;
        }
        if (argv.length > 2){
            System.out.println("Only accept one configure file. There are too many");
            return;
        }

        ServiceManager servicemanager = new ServiceManager();
        servicemanager.services = new ArrayList<TService>();
        //servicemanager.threadPoolExecutor = new ThreadPoolExecutor();
        if (servicemanager.loadservices(argv[0]) == false){
            System.out.println("Configure file error");
            return;
        }
        servicemanager.startservices();
        servicemanager.waitforfinish();
    }

    public boolean loadservices(String config){
        Properties configprop = new Properties();
        String systemlauncher;
        String sysworktime;
        String workloadlauncher;

        String cleanwork;
        String listenport;
        String message1;
        String message2;
        String message3;
        String message4;
        String location1;
        String location2;
        try {
            FileInputStream configinputstream = new FileInputStream(config);
            configprop.load(configinputstream);
            configinputstream.close();
            systemlauncher = configprop.getProperty("systemlauncher"); // script for running the framework of D-Software like NM,RM in Hadoop
            sysworktime = configprop.getProperty("sysworktime"); //waiting time between system and workload
            workloadlauncher = configprop.getProperty("workloadlauncher"); // like script for wordcount in Hadoop

            cleanwork = configprop.getProperty("cleanwork"); // Run after the testing to clean
            listenport = configprop.getProperty("listenport"); // which port is used for receive the message
            message1 = configprop.getProperty("messageID1"); //the file containing ID for message 1
            message2 = configprop.getProperty("messageID2"); //the file containing ID for message 2
            message3 = configprop.getProperty("messageID3"); //the file containing ID for message 3
            message4 = configprop.getProperty("messageID4"); //the file containing ID for message 4
            location1 = configprop.getProperty("location1"); //first location to insert the blocking function
            location2 = configprop.getProperty("location2"); //second location to insert the blocking function

        }catch (Exception e){
            return false;
        }
        TargetsystemService targetsystemService = new TargetsystemService(systemlauncher,workloadlauncher,sysworktime);
        services.add(targetsystemService);

        MessageListener messageListener = new MessageListener(listenport,message1,message2,message3,message4,targetsystemService);
        services.add(messageListener);
        (new Thread(messageListener)).start();
        try{
            Thread.sleep(2000);
        }catch (Exception e){
            e.printStackTrace();
        }
        (new Thread(targetsystemService)).start();
        return true;
    }

    public void startservices(){
        //for (TService ts : services)
          //  (new Thread(ts)).start();
    }

    public void waitforfinish(){
        boolean finish = false;
        while (finish == false){
            try {
                finish = true;
                for (TService ts : services)
                    finish = finish && ts.isFinish();
                Thread.sleep(500);
            }catch (Exception e){}
        }
    }

}
