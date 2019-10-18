package mypack;
import java.io.*;
import java.net.*;
import java.lang.*;

public class Message{
  private int id;
  private ListEntry origin;
  private ListEntry sender;
  private int TTL;                                // Fields for each message
  private int type;
  private ListEntry target;

  public Message(String input){
      /*
      Message: a class holding the structure of a heartbeat message
      */
      String[] lines = input.split("[\\r\\n]+");   // seperate input string by change line
      try{
        id = Integer.parseInt(lines[0]);
        String[] temp = lines[1].substring(1).split("&");
        origin = new ListEntry(InetAddress.getByName(temp[0]), temp[1]);
        temp = lines[2].substring(1).split("&");
        sender = new ListEntry(InetAddress.getByName(temp[0]), temp[1]);
        TTL = Integer.parseInt(lines[3]);
        type = Integer.parseInt(lines[4]);
        temp = lines[5].substring(1).split("&");
        target = new ListEntry(InetAddress.getByName(temp[0]), temp[1]);    //parsing String to get fields
      } catch(Exception e){
        System.out.println(e);
      }
  }

  public Message(int in_id, ListEntry in_origin, ListEntry in_sender, int in_TTL, int in_type, ListEntry in_target){
    id = in_id;
    origin = in_origin;
    sender = in_sender;
    TTL = in_TTL;
    type = in_type;
    target = in_target;
  }

  public String toString(){                       // Creating a string containing the message
    String ret = "";
    ret += Integer.toString(id) + "\n";
    ret += origin.getId() + "\n";
    ret += sender.getId() + "\n";
    ret += Integer.toString(TTL) + "\n";
    ret += Integer.toString(type) + "\n";
    ret += target.getId();
    return ret;
  }

  public int getId() {                            // Methods to get message attributes
    return id;
  }

  public ListEntry getOrigin() {
    return origin;
  }

  public ListEntry getSender() {                  // Methods to get message attributes
    return sender;
  }
  public int getTTL() {
    return TTL;
  }

  public int getType() {                          // Methods to get message attributes
    return type;
  }

  public ListEntry getTarget() {
    return target;
  }
}
