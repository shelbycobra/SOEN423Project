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

import DEMS.Config;

import Replicas.Replica2.DataStructures.EmployeeRecord;
import Replicas.Replica2.DataStructures.ManagerRecord;
import Replicas.Replica2.DataStructures.Project;
import Replicas.Replica2.DataStructures.Record;
import Replicas.Replica2.DataStructures.ServerManager;

public class CenterServer implements CenterServerInterface
{
    private static final byte GET_COUNTS = 0x01, CHECK_RECORD = 0x02, ADD_RECORD = 0x03;

    private ServerManager serverManager;
	private int port;
	private String location;
	public HashMap<Character, ArrayList<Record>> records;
	
	public CenterServer(String location, HashMap<Character, ArrayList<Record>> records)
	{
	    serverManager = new ServerManager();
		this.location = location;
		port = setPortNumber(location);
		this.records = records;
	}
	
	@Override
	public String createMRecord(String managerID, String firstName, String lastName, int employeeID, String mailID, Project[] projects, String location)
	{
		ManagerRecord managerRecord = new ManagerRecord(firstName, lastName, employeeID, mailID, projects, location, managerID);
		managerRecord.setRecordID("MR" + serverManager.getNextID());
		String message = location + " Server ";
		
		if (createRecord(managerRecord))
		{
			message += "manager Record Created " + managerRecord.getRecordID() + ": " + firstName + ", " + lastName + ", " + employeeID + ", " + mailID + ", " + location.toString();
		}
		else
		{
			message += "manager Record creation failed.";
		}

		log(message+ "\n");
		return message + "\n";
	}

	@Override
	public String createERecord(String managerID, String firstName, String lastName, int employeeID, String mailID, String projectID)
	{
		EmployeeRecord employeeRecord = new EmployeeRecord(firstName, lastName, employeeID, mailID, projectID, managerID);
		employeeRecord.setRecordID("ER" + serverManager.getNextID());
		String message = location + " Server ";
		
		if (createRecord(employeeRecord))
		{
			message += "employee Record Created " + employeeRecord.getRecordID() + ": " + firstName + ", " + lastName + ", " + employeeID + ", " + mailID + ", " + projectID;
		}
		else
		{
			message += "employee Record creation failed.";
		}

		log(message+ "\n");
		return message+ "\n";
	}
	
	public synchronized boolean createRecord(Record record)
	{
		Character key = record.getLastName().charAt(0);
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

	private synchronized boolean removeRecord(Character key, Record record) {

        ArrayList<Record> value;

        try
        {
            if (records.containsKey(key))
            {
                value = records.get(key);
                value.remove(record);
                return true;
            }
            else
            {
                return false;
            }
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
		
		try
		{
			socket = new DatagramSocket(port+10000);
			byte[] messageBuffer = {GET_COUNTS};
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
						byte[] buffer = new byte[100];
						DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
						socket.receive(reply);
						response += new String(reply.getData()).trim() + ", ";
					}
					catch (SocketTimeoutException e)
					{
						System.out.println("Server on port " + i + " is not responding.");
					}
					
				}
			}
			
			response += location + ": " + serverManager.getRecordCount(records) + "\n";
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
		
		return response+ "\n";
	}

	@Override
	public String editRecord(String managerID, String recordID, String fieldName, String newValue)
	{
		String message = "";
		
		if (recordID.length() == 7)
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
			ManagerRecord recordToEdit = (ManagerRecord) serverManager.findRecordByID(recordID, records);
			
			if (recordToEdit == null)
			{
				message = location + " Server Couldn't find manager record with ID: " + recordID;
			}
			else
			{
                if (fieldName.matches("mail_id"))
                {
                    recordToEdit.setMailID(newValue);
                }
                else if (fieldName.matches("project_id"))
                {
                    // TODO: Do something about project...
                }
                else if (fieldName.matches("location"))
                {
                    if (newValue.matches("CA") || newValue.matches("US") || newValue.matches("UK"))
                    {
                        recordToEdit.setLocation(newValue);
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
		}
		else
		{
			EmployeeRecord recordToEdit = (EmployeeRecord) serverManager.findRecordByID(recordID, records);
			
			if (recordToEdit == null)
			{
				message = location + " Server couldn't find record employee with ID: " + recordID;
			}
			else
			{
                if (fieldName.matches("mail_id"))
                {
                    recordToEdit.setMailID(newValue);
                }
                else if (fieldName.matches("project_id"))
                {
                    recordToEdit.setProjectID(newValue);
                }
                else
                {
                    message = "Invalid field.";
                }
            }
		}

        log(location + " Server Changed " + fieldName + " for " + recordID + " to " + newValue);
		return message+ "\n";
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

        System.out.println(location + " Server attempting to check if record exists in the " + remoteCenterServerName + " server...");

        Record recordToTransfer = serverManager.findRecordByID(recordID, records);
		String message = location + " Server ";
		
		if (recordToTransfer == null)
		{
			message += "Couldn't find record with ID: " + recordID + " in " + location + " database.";
			log(message);
			return message;
		}
		
		if (!remoteCenterServerName.matches("CA") && !remoteCenterServerName.matches("US") && !remoteCenterServerName.matches("UK"))
		{
			message += "Invalid location. Must be CA, US, or UK.";
			log(message);
			return message;
		}
		
		if (!doesRecordExistInRemoteCenterServer(recordID, remoteCenterServerName))
		{
			message += "Record already exists on remote server with ID: " + recordID + ".";
			log(message);
			return message;
		}
		else {
            message += "Record does not exist on remote server.";

            if (transferRecordToRemoteCenterServer(recordID, remoteCenterServerName)) {
                message += "Record was successfully transferred to " + remoteCenterServerName + " server.";
            } else {
                message += "Record transfer failed to " + remoteCenterServerName + " server.";
            }
        }

		log(message+ "\n");
		return message+ "\n";
	}

	private boolean doesRecordExistInRemoteCenterServer(String recordID, String remoteCenterServerName)
	{
		DatagramSocket socket = null;
		int remotePort = setPortNumber(remoteCenterServerName);

		try
		{
			socket = new DatagramSocket();
			byte[] messageBuffer = new byte[1000];
			messageBuffer[0] = CHECK_RECORD;
            System.arraycopy(recordID.getBytes(), 0, messageBuffer, 1, recordID.getBytes().length);
			InetAddress host = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, remotePort);
			
			socket.send(request);
			socket.setSoTimeout(2000); // Set timeout if other servers aren't responding.
			
			try
			{
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				String replyString = new String(reply.getData()).trim();
				return replyString.equals("Y");
			}
			catch (SocketTimeoutException e)
			{
				System.out.println("Server on port " + remotePort + " is not responding.");
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
		int remotePort = setPortNumber(remoteCenterServerName);
		
		System.out.println("Attempting to transfer record to " + remoteCenterServerName + " server...");
		
		try
		{
			socket = new DatagramSocket();
			Record message = serverManager.findRecordByID(recordID, records);
			byte[] messageBuffer = new byte[1000];
			messageBuffer[0] = ADD_RECORD;
			System.arraycopy(serverManager.RecordToByte(message), 0, messageBuffer, 1, serverManager.RecordToByte(message).length);

			InetAddress host = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(messageBuffer, messageBuffer.length, host, remotePort);
			
			socket.send(request);
			socket.setSoTimeout(1000); // Set timeout if other servers aren't responding.
			
			try
			{
				byte[] buffer = new byte[1000];
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
				socket.receive(reply);
				String replyString = new String(reply.getData(), reply.getOffset(), reply.getLength());
				if (replyString.equals("success")) {
                    removeRecord(message.getLastName().charAt(0), message);
                    return true;
                }
                return false;
			}
			catch (SocketTimeoutException e)
			{
				System.out.println("Server on port " + remotePort + " is not responding.");
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

    private static int setPortNumber(String loc) {

        if ("CA".equals(loc))
            return Config.Replica2.CA_PORT;
        else if ("UK".equals(loc))
            return Config.Replica2.UK_PORT;
        else if ("US".equals(loc))
            return Config.Replica2.US_PORT;
        else
            return 6000;
    }
}
