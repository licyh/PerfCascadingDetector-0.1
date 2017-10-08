package trigger.server;

/**
 * Created by hadoop on 6/4/16.
 */
//import edu.uchicago.sg.dcbt.MessageProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class MessageListener extends TService{

    final static Logger LOG = LoggerFactory.getLogger(MessageListener.class);

    List<String> ls1;
    List<String> ls2;
    List<String> ls3;
    List<String> ls4;
    String content1;
    String content2;
    String content3;
    String content4;
    TService ts;
    Thread listener;
    Thread listener2;

    AtomicInteger flag12;
    AtomicInteger flag23;
    AtomicInteger flag31;

    public MessageListener(String addr, String f1, String f2,String f3, String f4,TService t){
        try {
            ls1 = Files.readAllLines(Paths.get(f1), Charset.defaultCharset());
            ls2 = Files.readAllLines(Paths.get(f2), Charset.defaultCharset());
            ls3 = Files.readAllLines(Paths.get(f3), Charset.defaultCharset());
            ls4 = Files.readAllLines(Paths.get(f4), Charset.defaultCharset());
        }catch (Exception e){
            LOG.error("Cannot open four messageID file");
        }
        ts = t;
        content1 = "";
        for (String st : ls1)
            content1 = content1 + st;

        content2 = "";
        for (String st : ls2)
            content2 = content2 + st;

        content3 = "";
        for (String st : ls3)
            content3 = content3 + st;

        content4 = "";
        for (String st : ls4)
            content4 = content4 + st;

        flag12 = new AtomicInteger();
        flag12.set(0);
        flag23 = new AtomicInteger();
        flag23.set(0);
        flag31 = new AtomicInteger();
        flag31.set(1);

        LOG.info("Listener targets: \n<1> " +content1+" \n<2> "+content2+" \n<3> "+content3+" \n<4> "+content4);
        listener = new Thread(new socketlistener(addr,ls1,ls2));
        //listener2 = new Thread(new socketlistener("128.135.11.22:12001",ls1,ls2));

        LOG.info("Message listener is created");
    }

    public void run(){
        listener.start();
        //listener2.start();
        LOG.info("Message listener begins to run");
        while (isfinish == false){
            try {
                Thread.sleep(1000);
                if (ts.isFinish())
                    this.isfinish = true;
            }catch (Exception e){
                LOG.error("Conmunication meets some problems between ");
            }
        }
        //listener.interrupt();
        listener.stop();
        //listener2.stop();
        LOG.info("Message listener comes to end");
    }

    class socketlistener implements Runnable{
        // Logger LOG = LoggerFactory.getLogger(socketlistener.class);

        //SocketAddress socketAddress;
        ServerSocket serverSocket;
        public socketlistener(String addr, List<String> l1, List<String>l2){ //l1 l2 are not useful
            String [] addrs = addr.split(":");
            try {
                LOG.info("Listener is on "+addrs[0]+":"+addrs[1]);
                serverSocket = new ServerSocket(Integer.parseInt(addrs[1]));
                serverSocket.setSoTimeout(10000);
                //System.out.println(ls1);

            } catch (Exception e){
                e.printStackTrace();
                System.out.println("Listener cannot monitor the port");
            }

        }

        public void run(){
            while(true)
            {
                try
                {
                    Socket socket = serverSocket.accept();
                    /*
                    DataInputStream in =
                            new DataInputStream(socket.getInputStream());
                    String inputinfo = in.readUTF();
                    System.out.println(inputinfo+"~~~~~~~");
                    LOG.info("Server receives "+inputinfo+ "~~");

                    DataOutputStream out =
                            new DataOutputStream(socket.getOutputStream());
                    out.writeUTF("GO! Pikachu");

                    */
                    new EchoSocket(socket).start();
                    Thread.sleep(100);
                    //socket.close();
                }catch(SocketTimeoutException s)
                {
                    System.out.println("Socket keeps listened!");
                    continue;
                }catch(Exception e)
                {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    class EchoSocket extends Thread{
        protected Socket socket;
        public EchoSocket(Socket clientsocket){
            socket = clientsocket;
        }

        public void run(){
            try{
                DataInputStream in =
                        new DataInputStream(socket.getInputStream());
                //System.out.println("Begin to deserialize the coming message");
                //MessageProto.Message message = MessageProto.Message.parseDelimitedFrom(in);
                //System.out.println("finish deserialize the coming message");
                String inputinfo = in.readUTF();
                //if ((inputinfo.equals(content1))||(inputinfo.equals(content2))||(inputinfo.equals(content3)))
                    System.out.println(inputinfo + " come here!!!");
                //System.out.println(inputinfo.substring(0, Math.min(150, inputinfo.length())) + " come here!!!!");
                int limitt = 15;
                LOG.info("Server receives "+inputinfo+ " ~~");
                if (inputinfo.substring(0, inputinfo.indexOf('@')).equals(content1)) {
                    System.out.println(inputinfo + " arrived");
                    flag12.addAndGet(1);
                    System.out.println(" as content1");
                    int t= 1;
                    while (flag31.get() <1){
                        Thread.sleep(500*t);
                        t++;
                        if (t>limitt) break;
                    }
                    if (flag31.get() >0)
                        flag31.addAndGet(-1);
                    else
                        System.out.print("<TIMEOUT> ");
                    System.out.println("Reply to it Content 1 ");// + inputinfo);
                }
                if (inputinfo.substring(0, inputinfo.indexOf('@')).equals(content2)){
                    System.out.println(inputinfo + " arrived");
                    flag23.addAndGet(1);
                    System.out.println(" as content2");
                    int t= 1;
                    while (flag12.get() <1){
                        Thread.sleep(500*t);
                        t++;
                        if (t>limitt) break;
                    }
                    if (flag12.get() >0)
                        flag12.addAndGet(-1);
                    else
                    //if (flag12.get() < 1)
                        System.out.print("<TIMEOUT> ");
                    System.out.println("Reply to it Content 2 ");// + inputinfo);
                }

                if (inputinfo.substring(0, inputinfo.indexOf('@')).equals(content3)){
                    System.out.println(inputinfo + " arrived");
                    flag31.addAndGet(1);
                    System.out.println(" as content3");
                    int t= 1;
                    while (flag23.get() <1){
                        Thread.sleep(500*t);
                        t++;
                        if (t>limitt) break;
                    }
                    if (flag23.get() >0)
                        flag23.addAndGet(-1);
                    else
                        System.out.print("<TIMEOUT> ");
                    System.out.println("Reply to it Content 3 "); // + inputinfo);
                }
                //MessageProto.Message reply = MessageProto.Message.newBuilder()
                //        .setContent(inputinfo+", GO! Pikachu").build();
                DataOutputStream out =
                        new DataOutputStream(socket.getOutputStream());
                out.writeUTF("Go! PiKachu"); //inputinfo+" GO! Pikachu");
                //reply.writeTo(out);
                //if ((inputinfo.equals(content1))||(inputinfo.equals(content2))||(inputinfo.equals(content3)))
                //    System.out.println("Reply to it " + inputinfo);
                //System.out.println("Reply to it ");
                out.flush();
                out.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }
}
