import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.*;
import java.time.*;
import mypack.*;
import java.util.Arrays;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.Random;


public class Node{
/*
Class Node:
DESCRIPTION:
    the main body of the program, maining membership
*/
    ExecutorService clients;
    // int stats = 0;
    // int stats1 = 0;
    static final int NORMAL = 0;
    static final int JOIN = 1;
    static final int LEAVE = 2;
    static final int DEAD = 3;
    static final int INTR_PORT = 5225;
    static final int INTERVAL = 10;
    static final int MSG_INTVAL = 500;
    static final int INTR_COM_PORT = 5345;
    static final int RECVR_PORT_NUM = 5111;
    static final int SDR_PORT_NUM = 5242;
    static final int MSG_BUFFER_SIZE = 8192;
    static final int CHECK_PERIOD = 200;
    InetAddress introducer;
    Vector<ListEntry> membership = new Vector<ListEntry>();
    ListEntry[] sendBeats = new ListEntry[3];
    ListEntry[] receiveBeats = new ListEntry[3];
    InetAddress ip;
    final Control control = new Control();

    public static void main(String[] args) {
      /*
      main(): takes one argument: int: 0, indicates this machine is the first node in the system
                                       1, indicates which vm to connect to
                                       VM CANNOT CONNECT TO ITSELF
      */
      ExecutorService server = Executors.newFixedThreadPool(1);
      int i = Integer.parseInt(args[0]);
      if (i < 0 || i > 10){
          System.out.println("args out of range");
          System.exit(0);
      }
      server.execute(new Server());
        try{
            Node node = new Node();
            node.node(i);
        } catch(Exception e){
            System.out.println(e);
        }
    }

    public void printinglist(){
        /*
        printinglist(): print out the membership list
        */
        for(int i = 0; i < membership.size(); i++){
          System.out.println(membership.get(i).getId());
        }
        return;
    }

    public String getlist(){
        /*
        getlist(): return the membership list in a string
        */
        String ret = "";
        for(int i = 0; i < membership.size(); i++){
          ret += "||";
          ret += membership.get(i).getId();
        }
        return ret.substring(2);
    }

    private void node(int specMac){
        /*
        node(specMac): function that does the main mission. Set up connection, maintain membership
        VARAIBLE: specMac: the machine number u want to connect to
        */
        ExecutorService interFace = Executors.newFixedThreadPool(1);
        interFace.execute(new IOthread());
        try{
            ip = InetAddress.getLocalHost();
            ip = InetAddress.getByName(ip.getHostAddress());
        }catch(Exception e){
            System.out.println(e);
        }


        ExecutorService intro;
        for(int i = 0; i < 3; i++){
          sendBeats[i] = null;
          receiveBeats[i] = null;
        }
        membership.clear();
        intro = Executors.newFixedThreadPool(1);
        while(true){
            // halt the program until join in typed by user
            control.cmdLock.readLock().lock();
            String temp = control.cmd;
            control.cmdLock.readLock().unlock();
            if(temp.equals("join"))
                break;
        }

        if(specMac == 0){
            // create new system
            String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS").format(new Date());
            membership.add(new ListEntry(ip, timestamp));

            Collections.sort(membership, (ListEntry p1, ListEntry p2) -> p1.getTime().compareTo(p2.getTime()));
            // printinglist();
        }
        else{
            // join existing system by getting introduced
            String listmem = getIntrodced(specMac);
            String[] lines = listmem.split("[\\r\\n]+");
            InetAddress tempIP = null;
            for(int i = 0; i < lines.length; i++){
                String[] temp = lines[i].substring(1).split("&");
                try{
                    tempIP = InetAddress.getByName(temp[0]);
                }catch(Exception e){
                    e.getStackTrace()[0].getLineNumber();
                    System.out.println(e);
                }
                membership.add(new ListEntry(tempIP, temp[1]));

            }
            restructure();
        }
        // if (findIndex(ip) == -1) System.out.println("Line 154");
        intro.execute(new Introducer(membership.get(findIndex(ip))));
        // start new introducer thread (all machines are introducer)

        // writing to log
        String toLog = ">>>>> New logging process started at ";
        toLog += new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS").format(new Date());
        String fileName = "vm" + Integer.toString(getMachineNo()) + ".log";
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(toLog);
            writer.newLine();   //Add new line
            writer.close();
        } catch(Exception e) {
            // System.out.println("Oops, log cannot be written.");
        }


        // printinglist();
        while(true){
            clients = Executors.newFixedThreadPool(4);
            int k = findIndex(ip);
            // if(k==-1) System.out.println("Line 174");
            ListEntry myentry = membership.get(k);
            ListEntry sendingone = sendBeats[0];
            ListEntry sendingtwo = sendBeats[1];
            ListEntry sendingthree = sendBeats[2];

            clients.execute(new heartbeatSdr(SDR_PORT_NUM, myentry, sendingone, sendingtwo, sendingthree, RECVR_PORT_NUM));
            // sending to RECVR_PORT_NUM from SDR_PORT_NUM

            // start three heartbeatRecvr thread to listen to three predecessors
            for(int i = 0; i < receiveBeats.length; i++){
                if(receiveBeats[i] == null) continue;
                ListEntry receive = receiveBeats[i];
                int l = findIndex(ip);
                if(l==-1) System.out.println("Line 174");
                ListEntry myself = membership.get(l);
                clients.execute(new heartbeatRecvr(RECVR_PORT_NUM + i + 1, receive, myself));
            }

            // halt main thread till all other threads done execution
            clients.shutdown();
            try{
                clients.awaitTermination(1, TimeUnit.HOURS);
            }catch(Exception e){
                // System.out.println("Jiyu caused this exception");
            }

            // read the process the message
            control.lock.readLock().lock();
            String temp_msg = control.shared_msg;
            control.lock.readLock().unlock();

            Message toDo = new Message(temp_msg);
            switch(toDo.getType()){
                case JOIN:
                    addNode(toDo.getTarget().getAddress(), toDo.getTarget().getTime());
                    break;
                case DEAD:
                    toLog = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS").format(new Date());
                    toLog += ": FAILURE: ";
                    toLog += toDo.getTarget().getId();
                    toLog += " reported by: ";
                    toLog += toDo.getSender().getId();
                    fileName = "vm" + Integer.toString(getMachineNo()) + ".log";
                    try{
                        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
                        writer.write(toLog);
                        writer.newLine();   //Add new line
                        writer.close();
                    } catch(Exception e) {
                        // System.out.println("Oops, log cannot br written.");
                    }
                    delNode(toDo.getTarget().getAddress());
                    break;
                case LEAVE:
                    k = findIndex(ip);
                    if( k != -1 ) {
                      ListEntry tempmyentry = membership.get(k);
                      if (tempmyentry.equalsTo(toDo.getOrigin())) break;
                    }
                    toLog = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS").format(new Date());
                    toLog += ": LEAVE: ";
                    toLog += toDo.getTarget().getId();
                    toLog += " reported by: ";
                    toLog += toDo.getSender().getId();
                    fileName = "vm" + Integer.toString(getMachineNo()) + ".log";
                    try{
                        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
                        writer.write(toLog);
                        writer.newLine();   //Add new line
                        writer.close();
                    } catch(Exception e) {
                        // System.out.println("Oops, log cannot br written.");
                    }
                    delNode(toDo.getTarget().getAddress());
                    break;
                default:
                    break;
            }

            // update message cache
            control.cacheLock.writeLock().lock();
            control.shared_cache.add(toDo);
            if(control.shared_cache.size() > 10){
                control.shared_cache.remove(0);
            }
            control.cacheLock.writeLock().unlock();

            // Collections.sort(membership, (ListEntry p1, ListEntry p2) -> p1.getTime().compareTo(p2.getTime()));
            restructure();
            // check the user input command, exit program if command is "leave"
            control.shared_msg = "";
            control.cmdLock.readLock().lock();
            String temp_cmd = control.cmd;
            control.cmdLock.readLock().unlock();
            if (temp_cmd.equals("leave"))
                break;
        }
        System.exit(0);
    }

    public String getIntrodced(int specMac){
        /*
            getIntrodced(specMac): called when try to connect to other vm
        */
        String[] servers = new String[11];
        servers[1] = "172.22.152.2";
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
            introducer = InetAddress.getByName(servers[specMac]);
        }catch(Exception e){}
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(INTR_COM_PORT);
        }catch(Exception e){
            // System.out.println(e);
        }

        byte[] buf = new byte[MSG_BUFFER_SIZE];
        DatagramPacket packet;
        packet = new DatagramPacket(buf, buf.length, introducer, INTR_PORT);

        try {
            socket.send(packet);
        }catch(Exception e){
            // System.out.println(e);
        }


        try {
            packet = new DatagramPacket(buf, buf.length);
        }catch(Exception e){
            // System.out.println(e);
        }


        try {
            socket.receive(packet);
        }catch(Exception e){
            // System.out.println(e);
        }

        String received = new String(packet.getData(), 0, packet.getLength());
        if(received.length() == 0){
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            }catch(Exception e){
                // System.out.println(e);
            }
        }
        packet = new DatagramPacket(buf, buf.length, introducer, INTR_COM_PORT);
        // send acknowledgement
        try {
            socket.send(packet);
        }catch(Exception e){
              // System.out.println(e);
        }
        socket.close();
        return received;
    }

    public String introducer(InetAddress target){
        /*
            introducer(target): add on InetAddress to the membership
        */

        String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS").format(new Date());
        ListEntry toAdd = new ListEntry(target, timestamp);
        membership.add(toAdd);
        Collections.sort(membership, (ListEntry p1, ListEntry p2) -> p1.getTime().compareTo(p2.getTime()));

        // keep logs
        String toLog = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS").format(new Date());
        toLog += ": JOIN: ";
        toLog += toAdd.getId();
        toLog += ": updated membership List: ";
        toLog += getlist();
        String fileName = "vm" + Integer.toString(getMachineNo()) + ".log";
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(toLog);
            writer.newLine();   //Add new line
            writer.close();
        } catch(Exception e) {
            // System.out.println("Oops, log cannot br written.");
        }

        String listToSend = "";
        // construct the message to send to new comer
        for(int i = 0; i < membership.size(); i++){
            listToSend += membership.get(i).getId() + "\n";
        }
        restructure();
        return listToSend;
    }

    public boolean delNode(InetAddress target){
        /*
            delNode(): delete the specified node
            RETURN VALUE: true if the node is successfully deleted
                          false if the node doesnot exist or deletion failed
        */
        int i = findIndex(target);
        if(i == -1) return false;
        ListEntry temp_entry = membership.get(i);
        membership.remove(i);
        Collections.sort(membership, (ListEntry p1, ListEntry p2) -> p1.getTime().compareTo(p2.getTime()));

        // update the log
        String toLog = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS").format(new Date());
        toLog += ": REMOVE: ";
        toLog += temp_entry.getId();
        toLog += ": updated membership List: ";
        toLog += getlist();
        String fileName = "vm" + Integer.toString(getMachineNo()) + ".log";
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(toLog);
            writer.newLine();   //Add new line
            writer.close();
        }catch(Exception e){
            // System.out.println("Oops, log cannot br written.");
        }
        return true;
        // restructure();
        // tell heartbeats to send everyone the msg
    }

    public boolean addNode(InetAddress target, String timestamp){
        /*
            addNode(): add a new node to membership
            RETURN VALUE: true if the node is successfully added
                          false if the node doesnot exist or addition failed
        */
        if(findIndex(target) != -1) return false;
        ListEntry toAdd = new ListEntry(target, timestamp);
        membership.add(toAdd);
        Collections.sort(membership, (ListEntry p1, ListEntry p2) -> p1.getTime().compareTo(p2.getTime()));
        // restructure();

        // update the log
        String toLog = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS").format(new Date());
        toLog += ": ADD: ";
        toLog += toAdd.getId();
        toLog += ": updated membership List: ";
        toLog += getlist();
        String fileName = "vm" + Integer.toString(getMachineNo()) + ".log";
        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(toLog);
            writer.newLine();   //Add new line
            writer.close();
        } catch(Exception e) {
            // System.out.println("Oops, log cannot be written.");
        }
        return true;
    }

    public int findIndex(InetAddress target){
        /*
        findIndex(): find the index in membership list accorsing to InetAddress
        return -1 if not found
        */
        for(int i = 0; i < membership.size(); i++){
            if(target.equals(membership.get(i).getAddress())) return i;
        }
        return -1;
    }

    class Control{
        /*
            Control: a class keeps the shared information between threads
        */
        public volatile String shared_msg = "";
        public ReadWriteLock lock = new ReentrantReadWriteLock();
        public volatile Vector<Message> shared_cache = new Vector<Message>();
        public ReadWriteLock cacheLock = new ReentrantReadWriteLock();
        public volatile String cmd = "";
        public ReadWriteLock cmdLock = new ReentrantReadWriteLock();
    }

    class IOthread implements Runnable{
        /*
            IOthread: a Runnable class as user interface
        */
        public void run() {
            Scanner inStream = new Scanner(System.in);
            while(true){
                String input = inStream.nextLine();
                if (input.equals("join")){
                    control.cmdLock.writeLock().lock();
                    control.cmd = input;
                    control.cmdLock.writeLock().unlock();
                }
                if (input.equals("list")){
                    printinglist();
                }
                if (input.equals("listid")){
                    try{
                        int temp = findIndex(ip);
                        System.out.println(membership.get(temp).getId());
                    } catch (Exception e) {
                        System.out.println("No id assigned yet.");
                    }
                }
                if (input.equals("leave")){
                    control.cmdLock.writeLock().lock();
                    control.cmd = input;
                    control.cmdLock.writeLock().unlock();
                    int temp = findIndex(ip);
                    if(temp == -1) continue;
                    ListEntry self = membership.get(temp);
                    Message sd_msg = new Message(0, self, self, 4, LEAVE, self);
                    control.lock.writeLock().lock();
                    control.shared_msg = sd_msg.toString();
                    control.lock.writeLock().unlock();
                }
            }
        }
    }

    public class Introducer implements Runnable{
        /*
            Introducer: a subclass processing new join requests
        */
        private DatagramSocket socket;
        private DatagramSocket comSocket;
        private ListEntry self;

        public Introducer(ListEntry in_self){
            self = in_self;
            try{
                socket = new DatagramSocket(INTR_PORT);
                comSocket = new DatagramSocket(INTR_COM_PORT);
            } catch (Exception e) {
                // System.out.println(e);
                System.exit(0);
            }
        }

        public void run(){
            InetAddress temp_Inet = null;
            byte[] buf = new byte[MSG_BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while(true){
                control.cmdLock.readLock().lock();
                String temp = control.cmd;
                control.cmdLock.readLock().unlock();
                if (temp.equals("leave")) break;
                try{
                    socket.setSoTimeout(CHECK_PERIOD);
                } catch(Exception e){}
                try {
                    socket.receive(packet);
                }
                catch(Exception e){
                    // System.out.println(e);
                    continue;
                }
                try{
                    temp_Inet = packet.getAddress();
                }catch(Exception e){
                    continue;
                }
                String back = introducer(temp_Inet);
                byte[] sdBuf = back.getBytes();
                DatagramPacket comPacket = new DatagramPacket(sdBuf, sdBuf.length, temp_Inet, INTR_COM_PORT);
                // waiting for acknowledgement, if not received within 1 second resend
                try{
                    comSocket.setSoTimeout(1000);
                } catch(Exception e){
                    // System.out.println(e);
                }
                do{
                    try {
                        comSocket.send(comPacket);
                    } catch(Exception e) {
                        // System.out.println(e);
                        continue;
                    }
                    try {
                        comSocket.receive(packet);
                    }
                    catch(SocketTimeoutException e){continue;}
                    catch(Exception e){
                        // System.out.println(e);
                        continue;
                    }
                } while (!packet.getAddress().equals(temp_Inet));
                int k = findIndex(temp_Inet);
                // if(k==-1) System.out.println("605");
                Message message = new Message(0, self, self, 4, JOIN, membership.get(k));
                control.lock.writeLock().lock();
                control.shared_msg = message.toString();
                control.lock.writeLock().unlock();
            }
        }
    }

    public class heartbeatRecvr implements Runnable{
        /*
            heartbeatRecvr: a subclass responsible for receiving heartbeats
        */
        private DatagramSocket socket;
        private ListEntry self;
        private ListEntry client;
        private int port2;

        public heartbeatRecvr(int port, ListEntry input, ListEntry in_self) {
            try {
                socket = new DatagramSocket(port);
                port2 = port;
            }
            catch(Exception e){
                // System.out.println(e);
                System.exit(0);
            }
            client = input;
            self = in_self;
        }

        public void run(){
          control.lock.readLock().lock();
          String temp = control.shared_msg;
          control.lock.readLock().unlock();

          while(temp == ""){
              if (client == null){
                  control.lock.readLock().lock();
                  temp = control.shared_msg;
                  control.lock.readLock().unlock();
                  break;
              }
              String msg = "";
              int gap = 1500;
              do{
                  control.lock.readLock().lock();
                  temp = control.shared_msg;
                  control.lock.readLock().unlock();
                  if (temp != ""){
                      break;
                  }
                  long startTime = System.currentTimeMillis();
                  try {
                      msg = receiver(socket, INTERVAL);
                  } catch (Exception e){
                      // System.out.println(e);
                  }

                  if (msg == ""){
                      gap -= INTERVAL;
                      if (gap < 0){
                          gap = 0;
                      }
                  }
                  else{
                      try{
                          Message message = new Message(msg);
                          if(message.getSender().equalsTo(client)){
                              if(message.getType() != NORMAL){
                                  control.cacheLock.readLock().lock();
                                  boolean in = isIn(control.shared_cache, message);
                                  control.cacheLock.readLock().unlock();
                                  if (!in){
                                      Message sd_msg = new Message(0, message.getOrigin(), self, message.getTTL() - 1, message.getType(), message.getTarget());
                                      control.lock.writeLock().lock();
                                      control.shared_msg = sd_msg.toString();
                                      control.lock.writeLock().unlock();
                                      temp = sd_msg.toString();
                                  }
                              }
                              break;
                          }
                          else{
                              // Sender does not match
                              long endTime = System.currentTimeMillis();
                              gap -= (int)(endTime - startTime);
                              if (gap < 0){
                                  gap = 0;
                              }
                          }
                      } catch(Exception e){
                        e.getStackTrace()[0].getLineNumber();
                        // System.out.println("Message break.");
                        long endTime = System.currentTimeMillis();
                        gap -= (int)(endTime - startTime);
                        if (gap < 0){
                            gap = 0;
                        }
                      }
                  }
              } while(gap > 0);
              if (gap == 0){
                  // Node time out
                  Message message = new Message(0, self, self, 4, DEAD, client);
                  control.lock.writeLock().lock();
                  control.shared_msg = message.toString();
                  control.lock.writeLock().unlock();
                  temp = message.toString();
                  break;
              }
              control.lock.readLock().lock();
              temp = control.shared_msg;
              control.lock.readLock().unlock();
          }
          socket.close();
        }

        public String receiver(DatagramSocket temp, int time){
            // System.out.println(time);
            byte[] buf = new byte[MSG_BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            String msg = "";
            try{
                temp.setSoTimeout(time);
            } catch(Exception e){
                // System.out.println(e);
            }
            try {
                temp.receive(packet);
                msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                // System.out.println(msg);
            }
            catch(SocketTimeoutException e){}
            catch(Exception e){
                // System.out.println(e);
            }
            finally{
                return msg;
            }
        }
    }

    public class heartbeatSdr implements Runnable{
        /*
        heartbeatSdr: a subclass responsible for sending heartbeats
        */
        private DatagramSocket socket;
        private ListEntry[] entrys = new ListEntry[3];
        private ListEntry self;
        private int port;
        private int target;
        private byte[] buf = new byte[MSG_BUFFER_SIZE];

        public heartbeatSdr(int in_port, ListEntry in_self, ListEntry input1, ListEntry input2, ListEntry input3, int in_target) {
            try{
                socket = new DatagramSocket(port);
                // System.out.println("OPENED SENDER SOCKET");
            }catch(Exception e){
                // System.out.println(e);
            }
            port = in_port;
            target = in_target;
            self = in_self;
            entrys[0] = input1;
            entrys[1] = input2;
            entrys[2] = input3;
        }

        public void run(){
            control.lock.readLock().lock();
            String temp = control.shared_msg;
            control.lock.readLock().unlock();
            for(int i  = 0; i < 3; i++){
                if (entrys[i] == null){continue;}
            }

            while(temp == ""){
                try{
                    Message toSend = new Message(0, self, self, 1, NORMAL, self);
                    // send a normal heartbeat wout piggybag message
                    send(toSend.toString(), target, entrys);
                    TimeUnit.MILLISECONDS.sleep(MSG_INTVAL);
                } catch(Exception e){
                    System.out.println(e);
                }
                control.lock.readLock().lock();
                temp = control.shared_msg;
                control.lock.readLock().unlock();
            }
            Message toSend = new Message(temp);
            if (toSend.getTTL() > 0){
                try{
                    send(toSend.toString(), target, entrys);
                } catch(Exception e){
                    // System.out.println(e);
                }
            }
            socket.close();
        }

        public void send(String msg, int port, ListEntry[] entrys){
            buf = msg.getBytes();
            DatagramPacket packet;
            for (int i = 0; i < entrys.length; i++){
                if (entrys[i] != null){
                    packet = new DatagramPacket(buf, buf.length, entrys[i].getAddress(), port + i + 1);
                    try {
                        socket.send(packet);
                    } catch(Exception e) {
                        // System.out.println(e);
                    }
                } else {
                    continue;
                }
            }
        }
    }

    public void restructure(){
        /*
        restructure(): function called everytime updates are made to membership list,
                       reconnect the nodes
        */
        // if(clients != null) clients.shutdownNow();
        Collections.sort(membership, (ListEntry p1, ListEntry p2) -> p1.getTime().compareTo(p2.getTime()));
        try{
            ip = InetAddress.getLocalHost();
            ip = InetAddress.getByName(ip.getHostAddress());
        } catch(Exception e){
            System.out.println(e);
        }

        Collections.sort(membership, (ListEntry p1, ListEntry p2) -> p1.getTime().compareTo(p2.getTime()));

        int i = findIndex(ip);
        if(i >= 3){
            sendBeats[0] = membership.get(Math.abs((i-3)%membership.size()));
            sendBeats[1] = membership.get(Math.abs((i-2)%membership.size()));
            sendBeats[2] = membership.get(Math.abs((i-1)%membership.size()));
        }
        else if(i >=2){
            sendBeats[0] = membership.get(Math.abs((membership.size()-1)%membership.size()));
            sendBeats[1] = membership.get(Math.abs((i-2)%membership.size()));
            sendBeats[2] = membership.get(Math.abs((i-1)%membership.size()));
        }
        else if(i >=1){
            sendBeats[0] = membership.get(Math.abs((membership.size()-2)%membership.size()));
            sendBeats[1] = membership.get(Math.abs((membership.size()-1)%membership.size()));
            sendBeats[2] = membership.get(Math.abs((i-1)%membership.size()));
        }
        else{
            // printinglist();
            sendBeats[0] = membership.get(Math.abs(membership.size()-3)%membership.size());
            sendBeats[1] = membership.get(Math.abs(membership.size()-2)%membership.size());
            sendBeats[2] = membership.get(Math.abs(membership.size()-1)%membership.size());
        }

        receiveBeats[0] = membership.get((i+3)%membership.size());
        receiveBeats[1] = membership.get((i+2)%membership.size());
        receiveBeats[2] = membership.get((i+1)%membership.size());


        Collections.sort(membership, (ListEntry p1, ListEntry p2) -> p1.getTime().compareTo(p2.getTime()));

        // for(int j = 1; j < receiveBeats.length; j++){
        //     if(receiveBeats[j] == receiveBeats[j-1]) receiveBeats[j-1] = null;
        // }

        // for(int j = 1; j < sendBeats.length; j++){
        //     if(sendBeats[j] == sendBeats[j-1]) sendBeats[j-1] = null;
        // }

        i = findIndex(ip);

        for(int j = 0; j < 3; j++){
            if(receiveBeats[j] == null) continue;
            if(receiveBeats[j].getId().equals(membership.get(i).getId())) receiveBeats[j] = null;
        }
        for(int j = 0; j < 3; j++){
            if(sendBeats[j] == null) continue;
            if(sendBeats[j].getId().equals(membership.get(i).getId())) sendBeats[j] = null;
        }

        if(membership.size() == 2){
            sendBeats[2] = null;
            receiveBeats[2] = null;
        }
    }

    public int getMachineNo(){
        /*
            getMachineNo(): return the virtural machine number
        */
        String[] servers = new String[11];
        servers[1] = "172.22.152.2";
        servers[2] = "172.22.154.2";
        servers[3] = "172.22.156.2";
        servers[4] = "172.22.152.3";
        servers[5] = "172.22.154.3";
        servers[6] = "172.22.156.3";
        servers[7] = "172.22.152.4";
        servers[8] = "172.22.154.4";
        servers[9] = "172.22.156.4";
        servers[10] = "172.22.152.5";
        for (int i = 1; i < 10; i++){
            if(ip.toString().substring(1).equals(servers[i]))
                return i;
        }
        return -1;
    }

    public boolean isIn(Vector<Message> cache, Message checker){
        for(int i = 0; i < cache.size(); i++){
            Message curr = cache.get(i);
            // System.out.println("Highly possible that -1 error occured here");
            if(checker.getType() == curr.getType() && checker.getTarget().equalsTo(curr.getTarget())){
                return true;
            }
        }
        return false;
    }

    private static int getRandomNumberInRange(int min, int max) {
        /*
            getRandomNumberInRange(): random number generator
        */
    		if (min >= max) {
    			   throw new IllegalArgumentException("max must be greater than min");
    		}
    		Random r = new Random();
    		return r.nextInt((max - min) + 1) + min;
  	}
}
