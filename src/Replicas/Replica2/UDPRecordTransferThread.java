package Replicas.Replica2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

import Replicas.Replica2.DataStructures.Record;
import Replicas.Replica2.DataStructures.ServerManager;

public class UDPRecordTransferThread extends Thread
{
	String location;
	int port;
	public HashMap<Character, ArrayList<Record>> records;
	
	public UDPRecordTransferThread(String location, int port, HashMap<Character, ArrayList<Record>> records)
	{
		this.location = location;
		this.port = port;
		this.records = records;
	}
	
	@Override
	public void run()
	{
		System.out.println(location + " Server listening on port " + port + " for Record Transfer requests...");
		DatagramSocket socket = null;

		try
		{
			socket = new DatagramSocket(this.port);
			byte[] buffer = new byte[1000];
	
			while (true)
			{
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				socket.receive(request);
				Record requestRecord = ServerManager.byteToRecord(request.getData());
				Character key = requestRecord.mLastName.charAt(0);
				ArrayList<Record> value = records.get(key);
				String replyString;
				
				try
				{
					if (records.containsKey(key))
					{
						value = records.get(key);
						value.add(requestRecord);
					}
					else
					{
						value = new ArrayList<Record>();
						value.add(requestRecord);
						records.put(key, value);
					}
					
					System.out.println("Record successfully received!");
					replyString = "success";
				}
				catch (Exception e)
				{
					System.out.println("Failed to receive record.");
					replyString = "fail";
				}
				
				byte[] replyBuffer = replyString.getBytes();
				DatagramPacket reply = new DatagramPacket(replyBuffer, replyBuffer.length, request.getAddress(), request.getPort());
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
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
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
