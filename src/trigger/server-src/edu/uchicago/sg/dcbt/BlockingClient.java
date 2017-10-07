package edu.uchicago.sg.dcbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import edu.uchicago.sg.dcbt.MessageProto;

/**
 * Created by hadoop on 6/4/16.
 */
public class BlockingClient {

    String content;

    public BlockingClient(String c){
        content = c;
    }

    public void send(String addr){
        String[] addrs = addr.split(":");
        int time=1;
        while(true){
        try {
                System.out.println("Sending to server " + content);
                // System.out.println("Sending to server " + content.substring(0, Math.min(200, content.length())));
                Socket client = new Socket(addrs[0], Integer.parseInt(addrs[1]));
                OutputStream outToServer = client.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);
                out.writeUTF(content);
                //MessageProto.Message message = MessageProto.Message.newBuilder()
                                                //.setContent(content).build();
                //message.writeDelimitedTo(out);
                out.flush();
                //out.close();

                InputStream inFromServer = client.getInputStream();
                DataInputStream in =
                        new DataInputStream(inFromServer);
                //MessageProto.Message reply = MessageProto.Message.parseDelimitedFrom(in);
                //System.out.println("Server says " + reply.getContent());
                System.out.println("Server says " + in.readUTF());
                client.close();
                break;
            }catch (Exception e){
                e.printStackTrace();
                try {
                    Thread.sleep(time*100);
                }catch (Exception et){
                    //e.printStackTrace();
                    break;
                }
                time = time*2;
                if (time >256) break;
                continue;
            }
        }
    }

    public static String processstack(StackTraceElement[] cause){
        String _str = "";
        for (StackTraceElement sye : cause){
            _str = _str+ sye.getClassName()+"-"+sye.getMethodName()+"-"+sye.getLineNumber()+";";
        }
        return _str;
    }
}
