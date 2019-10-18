package mypack;
import java.io.*;
import java.net.*;

public class ListEntry{
    /*
    ListEntry: a subclass keeps membership list entries
    */
    InetAddress address;
    String timestamp;
    String Id;

    public ListEntry(InetAddress a, String b){  //Custom object to store Node Entries in membership lists
        address = a;
        timestamp = b;
        Id = (address).toString() + "&" + timestamp;
    }

    public InetAddress getAddress(){            // Methods to retrieve data
        return address;
    }

    public String getTime(){
        return timestamp;
    }

    public String getId(){                      // Methods to retrieve data
        return Id;
    }

    public boolean equalsTo(ListEntry other){   // Method to compare Nodes
        String otherId = other.getId();
        if(Id.equals(otherId)) return true;
        return false;
    }

}
