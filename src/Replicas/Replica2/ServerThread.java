package Replicas.Replica2;

import java.util.*;

import Replicas.Replica2.DataStructures.Record;

public class ServerThread extends Thread
{
    private String location;
    HashMap<Character, ArrayList<Record>> records;
    int port;

    public ServerThread(String location, int portNumber)
	{
    	if (!location.matches("CA") && !location.matches("US") && !location.matches("UK"))
		{
			System.err.println("Invalid Location. Please choose CA, US, or UK.");
			System.exit(1);
		}
    	
    	this.port = portNumber;
        this.location = location;
        this.records = new HashMap<>();
    }
    
    @Override
    public void run()
    {
    	Thread recordCountThread = new UDPRecordCountThread(location, port, records);
		Thread recordCheckThread = new UDPRecordCheckThread(location, port+10, records);
		Thread recordTransferThread = new UDPRecordTransferThread(location, port+20, records);
		
		recordCountThread.start();
		recordCheckThread.start();
		recordTransferThread.start();
    }
}
