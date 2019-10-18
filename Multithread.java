import java.net.*;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Multithread{
/*
class Multithread:
    A class that sends out grep query and print out remote grep result on screen
*/
    static final int port = 24685;
    public class client implements Runnable{
    int vm_no;
    String term;
    boolean test;
    public client(int vm_no, String term, boolean test){
        this.vm_no = vm_no + 1;
        this.term = term;
        this.test = test;
        // System.out.println("Connect to VM" + vm_no);
    }
    public int whichVm(){
        return this.vm_no - 1;
    }
    public String whichTerm(){
        return this.term;
    }
    public void run(){
        // System.out.println("We have the term " + this.term);
        String[] servers = new String[11];
        servers[1] = "172.22.152.2"; //IPs for all VMs
        servers[2] = "172.22.154.2";
        servers[3] = "172.22.156.2";
        servers[4] = "172.22.152.3";
        servers[5] = "172.22.154.3";
        servers[6] = "172.22.156.3";
        servers[7] = "172.22.152.4";
        servers[8] = "172.22.154.4";
        servers[9] = "172.22.156.4";
        servers[10] = "172.22.152.5";
        try{
            Socket vm = new Socket(servers[this.vm_no], port);
            OutputStream outToServer = vm.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            StringBuilder total = new StringBuilder();
            out.writeUTF(this.term); //Sending term to VMs
            InputStream inFromServer = vm.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            int i = Integer.valueOf(in.readUTF());
            while(i>0){ //Receiving output from VMs
                if(in.available() == 0) continue;
                String currline = in.readUTF();
                total.append(currline);
                // System.out.println(in.readUTF());
                i -= currline.length();
            }
            System.out.print(total.toString());
            vm.close();

            File file = new File("Unittest.txt"); //Writing output to text files
            FileWriter fr = new FileWriter(file, false);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(total.toString());
            br.close();
            fr.close();

            // System.out.println(java.time.LocalTime.now());
        }
        catch (Exception e) {
            // e.printStackTrace();
            if(test) {throw new RuntimeException(e);}
      }
    }
}
    public static void main(String[] args){
        // System.out.println(java.time.LocalTime.now());
        String term = "";
        if (args.length > 1)
            term = args[0] + " '" + args[1] + "'";
        else
            term = args[0];
        Multithread exec = new Multithread(); //Initiating
        exec.startGrep(term);
    }

    public void startGrep(String term){
        ExecutorService clients = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) { //Launching threads to connect to all VMs
            clients.execute(new client(i, term, false));
        }
        clients.shutdown();
    }
}
