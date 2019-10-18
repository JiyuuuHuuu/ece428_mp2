package mypack;

import java.io.*;
import java.lang.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Server implements Runnable
{
  // private static Set<String> clientsName = new HashSet<>();
  // private static Set<PrintWriter> writers = new HashSet<>();
  public static String tp(String arg1, int arg2) {
      Process process=null;
      String tCommand = "grep " + arg1 + " vm" + String.valueOf(arg2) + ".log";
      // System.out.println(tCommand);
      StringBuilder total = new StringBuilder();
      String[] finalCommand = new String[] {"/bin/sh", "-c", tCommand};
      try {
          final ProcessBuilder processBuilder = new ProcessBuilder(finalCommand);
          processBuilder.redirectErrorStream(true);
          process = processBuilder.start();
          // stdout+stderr
          InputStreamReader isr = new InputStreamReader( process.getInputStream() );
          BufferedReader br = new BufferedReader(isr);
          String line;
          while ((line = br.readLine()) != null) {
            // System.out.println(line);
            total.append("vm" + String.valueOf(arg2) + ".log:" + line + "\n");
            // "vm" + String.valueOf(arg2) + ".log:" +
          }
          // FileOutputStream temp = new FileOutputStream("test.txt");
          // byte[] strtobyte = total.toString().getBytes();
          // temp.write(strtobyte);
          // temp.close();
          // System.out.println(total.toString());
          // outStream.writeUTF(total.toString());
          // System.out.println("Program terminated!");
          process.destroy();
          br.close();
          isr.close();
      }
      catch (Exception e) {
          e.printStackTrace();
      }
      return total.toString();
  }
  public void run()
  {
    ExecutorService clients = Executors.newFixedThreadPool(15); //threads
    ServerSocket reception = null;
    // System.out.println(">> Server launched");
    try{
        reception = new ServerSocket(24685);
    } catch (Exception e) {
        // System.out.println(e);
    }
    while(true)
    {
      try{
          clients.execute(new RequestHD(reception.accept()));
      } catch (Exception e) {
          // System.out.println(e);
      }
    }
  }

  private static class RequestHD implements Runnable
  {
    private Socket client;
    private int macNo;

    public RequestHD(Socket socket)
    {
      this.client = socket;
      String IP = "";
      try
      {
        IP = InetAddress.getLocalHost().getHostAddress();
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
      String[] servers = new String[10];
      servers[0] = "172.22.152.2";
      servers[1] = "172.22.154.2";
      servers[2] = "172.22.156.2";
      servers[3] = "172.22.152.3";
      servers[4] = "172.22.154.3";
      servers[5] = "172.22.156.3";
      servers[6] = "172.22.152.4";
      servers[7] = "172.22.154.4";
      servers[8] = "172.22.156.4";
      servers[9] = "172.22.152.5";
      for(int i = 0; i < 10; i++)
      {
        // System.out.print(i);
        // System.out.print(":" + servers[i] + " " + IP + "\n");
        if(IP.compareTo(servers[i]) == 0)
        {
          this.macNo = i + 1;
          break;
        }
      }
    }

    public void run()
    {
      // System.out.println(">> " + client.getInetAddress().toString() + " has connected.");
      int StreamMax = 32*1024;
      try
      {
        InputStream inFromServer = client.getInputStream();
        DataInputStream inStream = new DataInputStream(inFromServer);
        OutputStream outToServer = client.getOutputStream();
        DataOutputStream outStream = new DataOutputStream(outToServer);
        String input = inStream.readUTF();
        String output = tp(input, this.macNo);
        outStream.writeUTF(String.valueOf(output.length()));
        // System.out.println(output.length());
        while(output.isEmpty() != true)
        {
          if(output.length() >= StreamMax)
          {
            outStream.writeUTF(output.substring(0, StreamMax));
            output = output.substring(StreamMax);
          }
          else
          {
            outStream.writeUTF(output);
            break;
          }
        }
        // while(true)
        // {
        //   String input = inStream.readUTF();
        //   // boolean isFound = input.indexOf("/q") !=-1? true: false;
        //   // if(isFound) break;
        //   tp(input, outStream);
        //   // System.out.println(input);
        // }
        // boolean isFound = input.indexOf("/q") !=-1? true: false;
        // if(isFound) break;
        // outStream.writeUTF("sent");
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      finally
      {
        try
        {
          // System.out.println(">> " + client.getInetAddress() + " left.\n");
          client.close();
        }
        catch(Exception e)
        {
          e.printStackTrace();
        }
      }
    }
  }
}
