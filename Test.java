// import mypack.Server;
// import mypack.Client;
// // import mypack.heartbeatRecvr;
// // import mypack.heartbeatSdr;
// import mypack.Message;
// import mypack.ListEntry;
// import java.net.*;
// import java.io.*;
// import java.util.concurrent.TimeoutException;
// import java.util.concurrent.locks.ReadWriteLock;
// import java.util.concurrent.locks.ReentrantReadWriteLock;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.Executors;
// import java.util.concurrent.ExecutorService;
//
// //
// // public class Test{
// //     public static void main(String[] args){
// //         // new Server().start();
// //         // Client client = new Client();
// //         // System.out.println("34");
// //         // new Server().start();
// //         // whenCanSendAndReceivePacket_thenCorrect(client);
// //         // tearDown(client);
// //
// //
// //         //change
// //         // heartbeatSd();
// //         testMessage();
// //     }
// //
// //     // public void setup(){
// //     //     // new Server().start();
// //     //     Client client = new Client();
// //     //     System.out.println("34");
// //     // }
// //
// //     // @Test
// //     // public static void whenCanSendAndReceivePacket_thenCorrect(Client client) {
// //     //     System.out.println("20");
// //     //     String echo = client.sendEcho("hello server");
// //     //     System.out.println(echo);
// //     //     if("hello server" == echo){
// //     //         System.out.println("True");
// //     //     }
// //     //     else System.out.println("False");
// //
// //     //     echo = client.sendEcho("server is working");
// //     //     System.out.println(echo);
// //     //     if("hello server" != echo){
// //     //         System.out.println("True");
// //     //     }
// //     //     else System.out.println("False");
// //     // }
// //
// //     // // @After
// //     // public static void tearDown(Client client) {
// //     //     client.sendEcho("end");
// //     //     client.close();
// //     // }
// //
// //     public static void heartbeatSd(){
// //       heartbeatSdr sd = new heartbeatSdr();
// //       // sd.run();
// //     }
// //
// //     public static void heartbeatRecv(){
// //       heartbeatRecvr recv = new heartbeatRecvr();
// //     }
// //
// //     public static void testMessage(){
// //       InetAddress a = null;
// //       InetAddress b = null;
// //       InetAddress c = null;
// //       try{
// //         a = InetAddress.getByName("192.168.29.168");
// //         b = InetAddress.getByName("192.168.29.172");
// //         c = InetAddress.getByName("192.168.29.146");
// //       } catch(Exception e){
// //         System.out.println("Oops");
// //       }
// //       ListEntry aa, bb, cc;
// //       aa = new ListEntry(a, "20.57");
// //       bb = new ListEntry(b, "20.58");
// //       cc = new ListEntry(c, "20.59");
// //       Message msg = new Message(1, aa, bb, 3, 0, cc);
// //       System.out.println(msg.toString());
// //       Message msg2 = new Message(msg.toString());
// //       System.out.println(msg2.toString());
// //     }
// // }
//
// public class Test{
//   class Control{
//     public volatile String shared_msg = "";
//     public ReadWriteLock lock = new ReentrantReadWriteLock();
//   }
//   final Control control = new Control();
//
//   public class heartbeatRecvr implements Runnable{
//     private DatagramSocket socket;
//     private ListEntry client;
//
//     public heartbeatRecvr(int port, ListEntry input) {
//         try {
//             socket = new DatagramSocket(port);
//         }
//         catch(Exception e){
//             System.out.println(e);
//             System.exit(0);
//         }
//         client = input;
//     }
//
//     public void run(){
//       control.lock.readLock().lock();
//       String temp = control.shared_msg;
//       control.lock.readLock().unlock();
//       while(temp == ""){
//         String msg = "";
//         int gap = 5000;
//         do{
//           long startTime = System.currentTimeMillis();
//           try {
//               msg = receiver(socket, gap);
//           } catch (Exception e){
//             System.out.println(e);
//           }
//
//           if (msg == ""){
//             System.out.println("Node time out.");
//             break;
//           }
//           else{
//             // System.out.println("heartbeat received.");
//             // System.out.println(msg);
//             try{
//               Message message = new Message(msg);
//               if(message.getSender().equalsTo(client)){
//                 System.out.println("heartbeat received.");
//                 control.lock.writeLock().lock();
//                 control.shared_msg = message.toString();
//                 control.lock.writeLock().unlock();
//                 temp = message.toString();
//                 break;
//               }
//               else{
//                 System.out.println("Sender does not match.");
//                 long endTime = System.currentTimeMillis();
//                 gap -= (int)(endTime - startTime);
//               }
//             } catch(Exception e){
//               System.out.println("Message break.");
//               long endTime = System.currentTimeMillis();
//               gap -= (int)(endTime - startTime);
//             }
//           }
//         } while(gap != 5000);
//         control.lock.readLock().lock();
//         temp = control.shared_msg;
//         control.lock.readLock().unlock();
//       }
//     }
//
//     public String receiver(DatagramSocket temp, int time){
//       byte[] buf = new byte[256];
//       DatagramPacket packet = new DatagramPacket(buf, buf.length);
//       String msg = "";
//       try{
//         temp.setSoTimeout(time);
//       } catch(Exception e){
//         System.out.println("setSoTimeout");
//         System.out.println(e);
//       }
//       try {
//           temp.receive(packet);
//           msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
//       }
//       catch(SocketTimeoutException e){}
//       catch(Exception e){
//         System.out.println("receiver:");
//         System.out.println(e);
//       }
//       finally{
//           // System.out.println("receiver return");
//           return msg;
//       }
//     }
//   }
//
//   public class heartbeatSdr implements Runnable{
//     private DatagramSocket socket;
//     private ListEntry entry;
//     private ListEntry self;
//     private int port;
//     private int target;
//     private byte[] buf = new byte[256];
//
//     public heartbeatSdr(int in_port, ListEntry input, int in_target) {
//       // System.out.println("reached here");
//         port = in_port;
//         target = in_target;
//         try {
//             socket = new DatagramSocket(port);
//             InetAddress ip = InetAddress.getLocalHost();
//             ip = InetAddress.getByName(ip.getHostAddress());
//             self = new ListEntry(ip, "16.03");
//         }
//         catch(Exception e){
//             System.out.println(e);
//         }
//         // byte addr[] = {(byte)98, (byte)228, (byte)60, (byte)62};
//         // try{
//         //   // address = InetAddress.getByAddress(addr);
//         //   address = InetAddress.getByName("192.168.1.142");
//         // }
//         // catch(Exception e){
//         //     System.out.println(e);
//         // }
//         // System.out.println(address);
//         entry = input;
//     }
//
//     public void run(){
//       // System.out.println("run is running here");
//       control.lock.readLock().lock();
//       String temp = control.shared_msg;
//       control.lock.readLock().unlock();
//       while(temp == ""){
//         try{
//           Message toSend = new Message(2, self, self, 1, 1, self);
//           send(toSend.toString(), target);
//           TimeUnit.MILLISECONDS.sleep(200);
//         } catch(Exception e){
//             System.out.println(e);
//         }
//         control.lock.readLock().lock();
//         temp = control.shared_msg;
//         control.lock.readLock().unlock();
//       }
//       try{
//         Message toSend = new Message(temp);
//         send(toSend.toString(), target);
//       } catch(Exception e){
//           System.out.println(e);
//       }
//     }
//
//     public void send(String msg, int port){
//       buf = msg.getBytes();
//       DatagramPacket packet;
//       packet = new DatagramPacket(buf, buf.length, entry.getAddress(), port);
//       try {
//               // System.out.println("Before sending msg");
//               socket.send(packet);
//               // System.out.println("After sending msg");
//           }
//           catch(Exception e){
//               System.out.println(e);
//           }
//     }
//   }
//
//   private void test(){
//     ExecutorService clients = Executors.newFixedThreadPool(2);
//     InetAddress temp = null;
//     try{
//       temp = InetAddress.getByName("192.168.0.109");
//     } catch (Exception e){
//       System.out.println(e);
//     }
//     ListEntry shit = new ListEntry(temp, "16.28");
//     clients.execute(new heartbeatSdr(5242, shit, 4569));
//     clients.execute(new heartbeatRecvr(4569, shit));
//     clients.shutdown();
//   }
//
//   public static void main(String[] args){
//     try{
//       Test test = new Test();
//       test.test();
//     } catch(Exception e){
//       System.out.println(e);
//     }
//   }
// }
