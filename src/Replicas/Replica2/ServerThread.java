package Replicas.Replica2;

import java.util.*;

import Replicas.Replica2.DataStructures.Record;

public class ServerThread extends Thread
{
    private String location;
    HashMap<Character, ArrayList<Record>> records;
    CenterServer server;
    int port;

    public ServerThread(String location, int portNumber)
	{
    	if (!location.matches("CA") && !location.matches("US") && !location.matches("UK"))
		{
			System.err.println("Invalid Location. Please choose CA, US, or UK.");
			System.exit(1);
		}

		this.records = new HashMap<>();
		this.server = new CenterServer(location, records);
    	this.port = portNumber;
        this.location = location;
    }
    
    @Override
    public void run()
    {
    	Thread recordCountThread = new UDPRecordCountThread(location, port + 30, records);
		Thread recordCheckThread = new UDPRecordCheckThread(location, port+10, records);
		Thread recordTransferThread = new UDPRecordTransferThread(location, port+20, records);
		Thread serverThread = new UDPServerThread(server, port, location);

		recordCountThread.start();
		recordCheckThread.start();
		recordTransferThread.start();
		serverThread.start();
    }
}
