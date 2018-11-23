package Replicas.Replica2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

import Replicas.Replica2.DataStructures.Record;

public class UDPRecordCountThread extends Thread
{
	String location;
	int port;
	public HashMap<Character, ArrayList<Record>> records;
	
	public UDPRecordCountThread(String location, int port, HashMap<Character, ArrayList<Record>> records)
	{
		this.location = location;
		this.port = port;
		this.records = records;
	}
	
	@Override
	public void run()
	{
		System.out.println(location + " Server listening on port " + port+" for Record Count requests...");
		DatagramSocket socket = null;

		try
		{
			socket = new DatagramSocket(this.port);
			byte[] buffer = new byte[1000];
	
			while (true)
			{
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.receive(request);
				
				String count = location + ": " + Integer.toString(records.size());
				byte[] countBuffer = count.getBytes();
				
				DatagramPacket reply = new DatagramPacket(countBuffer, countBuffer.length, request.getAddress(), request.getPort());
				socket.send(reply);
			}
		}
		catch (SocketException e)
		{
			System.out.println("Socket: " + e.getMessage());
		}
		catch (IOException e)
		{
			System.out.println("IO: " + e.getMessage());
		}
		finally
		{
			if (socket != null)
			{
				socket.close();
			}
		}
	}

}
