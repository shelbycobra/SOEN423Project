package Replicas.Replica2;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

import Replicas.Replica2.DataStructures.EmployeeRecord;
import Replicas.Replica2.DataStructures.ManagerRecord;
import Replicas.Replica2.DataStructures.Project;
import Replicas.Replica2.DataStructures.Record;
import Replicas.Replica2.DataStructures.ServerManager;

public class CenterServer implements CenterServerInterface
{
	int port;
	String location;
	public HashMap<Character, ArrayList<Record>> records;
	
	public CenterServer(String location, HashMap<Character, ArrayList<Record>> records)
	{
		this.location = location;
		port = ServerManager.caPort;
		this.records = records;
	}
	
	@Override
	public String createMRecord(String managerID, String firstName, String lastName, int employeeID, String mailID, Project[] projects, String location)
	{
		ManagerRecord managerRecord = new ManagerRecord(firstName, lastName, employeeID, mailID, projects, location);
		String message = "";
		
		if (createRecord(managerRecord))
		{
			message = "Manager Record Created " + managerRecord.mRecordID + ": " + firstName + ", " + lastName + ", " + employeeID + ", " + mailID + ", " + location.toString();
		}
		else
		{
			message = "Manager Record creation failed.";
		}
		
		log(message);
		return message;
	}

	@Override
	public String createERecord(String managerID, String firstName, String lastName, int employeeID, String mailID, String projectID)
	{
		EmployeeRecord employeeRecord = new EmployeeRecord(firstName, lastName, employeeID, mailID, projectID);
		String message = "";
		
		if (createRecord(employeeRecord))
		{
			message = "Employee Record Created " + employeeRecord.mRecordID + ": " + firstName + ", " + lastName + ", " + employeeID + ", " + mailID + ", " + projectID;
		}
		else
		{
			message = "Employee Record creation failed.";
		}
		
		log(message);
		return message;
	}
	
	private boolean createRecord(Record record)
	{
		Character key = record.mLastName.charAt(0);
		ArrayList<Record> value;
		
		try
		{
			if (records.containsKey(key))
			{
				value = records.get(key);
				value.add(record);
			}
			else
			{
				value = new ArrayList<Record>();
				value.add(record);
				records.put(key, value);
			}
			
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public String getRecordCount(String managerID)
	{
		String response = "";
		DatagramSocket socket = null;
		
		System.out.println("Attempting to get record counts from other servers...");
		
		try
		{
			socket = new DatagramSocket();
			String message = "Count";
			byte[] messageBuffer = message.getBytes();
			InetAddress host = InetAddress.getByName("localhost");
			
			for (int i = 6000; i < 6003; i++)
			{
				if (i != this.port)
				{
					DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, i);
					socket.send(request);
					socket.setSoTimeout(2000); // Set timeout if other servers aren't responding.
					
					try
					{
						byte[] buffer = new byte[1000];
						DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
						socket.receive(reply);
						System.out.println("Reply: " + new String(reply.getData(), reply.getOffset(), reply.getLength()));
						response += new String(reply.getData(), reply.getOffset(), reply.getLength()) + "\n";
					}
					catch (SocketTimeoutException e)
					{
						System.out.println("Server on port " + i + " is not responding.");
					}
					
				}
			}
			
			response += location + ": " + records.size();
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
		
		return response;
	}

	@Override
	public String editRecord(String managerID, String recordID, String fieldName, String newValue)
	{
		String message = "";
		
		if (recordID.length() == 8)
		{
			if (!recordID.substring(0, 2).matches("MR") && !recordID.substring(0, 2).matches("ER"))
			{
				message = "Invalid Record ID. Must begin with 'MR' or 'ER'.";
			}
		}
		else
		{
			message = "Invalid Record ID. Must contain 5 digits.";
		}
		
		if (!message.isEmpty())
		{
			log(message);
			return message;
		}
		
		if (recordID.substring(0, 2).matches("MR"))
		{
			ManagerRecord recordToEdit = (ManagerRecord) ServerManager.findRecordByID(recordID, records);
			
			if (recordToEdit == null)
			{
				message = "Couldn't find record with ID: " + recordID;
			}
			
			if (fieldName.matches("mailID"))
        	{
				recordToEdit.mMailID = newValue;
        	}
        	else if (fieldName.matches("project"))
        	{
        		// TODO: Do something about project...
        	}
        	else if (fieldName.matches("location"))
        	{
        		if (newValue.matches("CA") || newValue.matches("US") || newValue.matches("UK"))
        		{
        			recordToEdit.mLocation = newValue;
        		}
        		else
        		{
        			message = "Invalid location. Must be CA, US, or UK.";
				}
        	}
        	else
        	{
        		message = "Invalid field.";
			}
		}
		else
		{
			EmployeeRecord recordToEdit = (EmployeeRecord) ServerManager.findRecordByID(recordID, records);
			
			if (recordToEdit == null)
			{
				message = "Couldn't find record with ID: " + recordID;
			}
			
			if (fieldName.matches("mailID"))
        	{
				recordToEdit.mMailID = newValue;
        	}
        	else if (fieldName.matches("projectID"))
        	{
        		recordToEdit.mProjectID = newValue;
        	}
        	else
        	{
        		message = "Invalid field.";
			}
		}
		
        log("Changed " + fieldName + " for " + recordID + " to " + newValue);
		return message;
	}

	@Override
	public String transferRecord(String managerID, String recordID, String remoteCenterServerName)
	{
		// Searches its hash map to find if the record with recordID exists.
		// If it exists, then it checks with the remoteCenterServer if a record with recordID does not exist in that remoteCenterServer.
		// If the record does not exist in the remoteCenterServer, then the entire record is transferred to the remoteCenterServer. 
		// These checking and transfer requires server to server communication which should be implemented using UDP/IP.
		// Note that the record should be removed from the hash map of the initial server and should be added to the hash map of the remoteCenterServer atomically.
		// The server informs the manager whether the operation was successful or not and both the server and the manager store this information in their logs
		
		Record recordToTransfer = ServerManager.findRecordByID(recordID, records);
		String message = "";
		
		if (recordToTransfer == null)
		{
			message = "Couldn't find record with ID: " + recordID;
			log(message);
			return message;
		}
		
		if (!remoteCenterServerName.matches("CA") && !remoteCenterServerName.matches("US") && !remoteCenterServerName.matches("UK"))
		{
			message = "Invalid location. Must be CA, US, or UK.";
			log(message);
			return message;
		}
		
		if (doesRecordExistInRemoteCenterServer(recordID, remoteCenterServerName))
		{
			message = "Record already exists on remote server with ID: " + recordID;
			log(message);
			return message;
		}
		else
		{
			message = "Record does not exist on remote server";
		}
		
		if (transferRecordToRemoteCenterServer(recordID, remoteCenterServerName))
		{
			records.get(recordToTransfer.mLastName.charAt(0)).remove(recordToTransfer);
			
			message = "Record was successfully transferred to " + remoteCenterServerName;
		}
		else
		{
			message = "Record transfer failed to " + remoteCenterServerName;
		}
		
		log(message);
		return message;
	}

	private boolean doesRecordExistInRemoteCenterServer(String recordID, String remoteCenterServerName)
	{
		DatagramSocket socket = null;
		int port = 6000;
		
		System.out.println("Attempting to check if record exists at " + remoteCenterServerName + "...");
		
		switch (remoteCenterServerName)
		{
			case "CA":	port = ServerManager.caPort+10;
						break;
			case "US":	port = ServerManager.usPort+10;
						break;
			case "UK":	port = ServerManager.ukPort+10;
						break;
		}
		
		try
		{
			socket = new DatagramSocket();
			String message = recordID;
			byte[] messageBuffer = message.getBytes();
			InetAddress host = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, port);
			
			socket.send(request);
			socket.setSoTimeout(2000); // Set timeout if other servers aren't responding.
			
			try
			{
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				String replyString = new String(reply.getData(), reply.getOffset(), reply.getLength());
				
				return replyString.matches("success");
			}
			catch (SocketTimeoutException e)
			{
				System.out.println("Server on port " + port + " is not responding.");
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
		
		return false;
	}
	
	private boolean transferRecordToRemoteCenterServer(String recordID, String remoteCenterServerName)
	{
		DatagramSocket socket = null;
		int port = 6000;
		
		System.out.println("Attempting to transfer record to " + remoteCenterServerName + "...");
		
		switch (remoteCenterServerName)
		{
			case "CA":	port = ServerManager.caPort+20;
						break;
			case "US":	port = ServerManager.usPort+20;
						break;
			case "UK":	port = ServerManager.ukPort+20;
						break;
		}
		
		try
		{
			socket = new DatagramSocket();
			Record message = ServerManager.findRecordByID(recordID, records);
			byte[] messageBuffer = ServerManager.RecordToByte(message);
			InetAddress host = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, port);
			
			socket.send(request);
			socket.setSoTimeout(2000); // Set timeout if other servers aren't responding.
			
			try
			{
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				String replyString = new String(reply.getData(), reply.getOffset(), reply.getLength());
				
				return replyString.matches("success");
			}
			catch (SocketTimeoutException e)
			{
				System.out.println("Server on port " + port + " is not responding.");
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
		finally
		{
			if (socket != null)
			{
				socket.close();
			}
		}
		
		return false;
	}

	@Override
	public String printRecords()
	{
		String recordsOutput = "";
		
		for (Character key : records.keySet())
		{
			ArrayList<Record> listOfRecords = records.get(key);
			
			for (Record record : listOfRecords)
			{
				recordsOutput += record.print() + "\r\n";
			}
		}
		
		return recordsOutput.isEmpty() ? "None" : recordsOutput;
	}
	
	private void log(String input)
	{
		FileWriter fileWriter;
		
		try
		{
			fileWriter = new FileWriter(location+"CenterServerLog.txt", true);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.printf("[%s] %s \r\n", new Timestamp(System.currentTimeMillis()), input);
			printWriter.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		System.out.println(input);
	}
}
