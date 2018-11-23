package Replicas.Replica2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

import DEMS.Config;
import DEMS.MessageKeys;
import Replicas.Replica2.DataStructures.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerThread extends Thread
{
	private static final byte GET_COUNTS = 0x01, CHECK_RECORD = 0x02, ADD_RECORD = 0x03;

	private String location;
    HashMap<Character, ArrayList<Record>> records;
    CenterServer server;
    private ServerManager serverManager;
    int port;
	private DatagramSocket aSocket = null;
	private DatagramPacket request = null;

    public ServerThread(String location, int portNumber)
	{
    	if (!location.matches("CA") && !location.matches("US") && !location.matches("UK"))
		{
			System.err.println("Invalid Location. Please choose CA, US, or UK.");
			System.exit(1);
		}

		serverManager = new ServerManager();
		this.records = new HashMap<>();
		this.server = new CenterServer(location, records);
    	this.port = portNumber;
        this.location = location;
    }
    
    @Override
    public void run()
    {

//		Thread recordCheckThread = new UDPRecordCheckThread(location, port+10, records);
//		Thread recordTransferThread = new UDPRecordTransferThread(location, port+20, records);
//		Thread recordCountThread = new UDPRecordCountThread(location, port+30, records);
////		Thread serverThread = new UDPServerThread(server, port, location);
//
//		recordCountThread.start();
//		recordCheckThread.start();
//		recordTransferThread.start();
////		serverThread.start();

		try
		{
			aSocket = new DatagramSocket(port);
			System.out.println(location + " server UDP Socket started on port " + port+". Waiting for request...");

			while(true)
			{
				byte[] buffer = new byte[1000];
				request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);


				byte request_type = request.getData()[0];

				// Checks first for Transfer Record messages
				if (request_type == GET_COUNTS)
				{
				    sendRecordCount();
				}
				else if (request_type == CHECK_RECORD)
				{
					String recordID = new String(request.getData());
					checkRecords(recordID);
				}
				else if (request_type == ADD_RECORD)
				{
					byte[] data = new byte[1000];
					System.arraycopy(request.getData(), 1, data, 0, (new String (request.getData())).trim().getBytes().length);
					addRecord(data);
				}
				else
				{
					JSONParser parser = new JSONParser();
					String str = new String(request.getData()).trim();
					JSONObject jsonMessage = (JSONObject) parser.parse(str);

					// recordData = [ SEQUENCE_ID, MANAGER_ID, MSG_ID, COMMAND_TYPE,
					// FIRST_NAME, LAST_NAME, EMPLOYEEID, MAILID,
					// { PROJECT ID } || { (PROJECT_ID, PROJECT_CLIENT, PROJECT_CLIENT_NAME) X N , LOCATION } ]

					switch (Integer.parseInt(jsonMessage.get(MessageKeys.COMMAND_TYPE).toString()))
					{
						case Config.CREATE_MANAGER_RECORD:
						{
							// Get projects
							JSONArray jsonProjects = (JSONArray) jsonMessage.get(MessageKeys.PROJECTS);
							Project[] projects = getProjectArray(jsonProjects);
//							System.out.println("Creating Record: " + jsonMessage.toJSONString());

							// Create Manager Record
							String msg = server.createMRecord((String) jsonMessage.get(MessageKeys.MANAGER_ID),
									(String) jsonMessage.get(MessageKeys.FIRST_NAME),
									(String) jsonMessage.get(MessageKeys.LAST_NAME),
									Integer.parseInt((String) jsonMessage.get(MessageKeys.EMPLOYEE_ID)),
									(String) jsonMessage.get(MessageKeys.MAIL_ID),
									projects,
									(String) jsonMessage.get(MessageKeys.LOCATION));
							System.out.println(msg);
							continue;
						}
						case Config.CREATE_EMPLOYEE_RECORD:
						{
							// Create Employee Record
							String msg = server.createERecord((String) jsonMessage.get(MessageKeys.MANAGER_ID),
									(String) jsonMessage.get(MessageKeys.FIRST_NAME),
									(String) jsonMessage.get(MessageKeys.LAST_NAME),
									Integer.parseInt((String) jsonMessage.get(MessageKeys.EMPLOYEE_ID)),
									(String) jsonMessage.get(MessageKeys.MAIL_ID),
									(String) jsonMessage.get(MessageKeys.PROJECT_ID));
							System.out.println(msg);
							continue;
						}
						case Config.GET_RECORD_COUNT:
						{
							// Get Record Count
							String counts = server.getRecordCount((String) jsonMessage.get(MessageKeys.MANAGER_ID));
							System.out.println("Record count: " + counts);
							continue;
						}
						case Config.EDIT_RECORD:
						{
							// Edit Record
							String output = server.editRecord((String) jsonMessage.get(MessageKeys.MANAGER_ID), (String) jsonMessage.get(MessageKeys.RECORD_ID), (String) jsonMessage.get(MessageKeys.FIELD_NAME), (String) jsonMessage.get(MessageKeys.NEW_VALUE));
							System.out.println("\n" + output);
							continue;
						}
						case Config.TRANSFER_RECORD:
						{
							// Transfer Record
							String output = server.transferRecord((String) jsonMessage.get(MessageKeys.MANAGER_ID), (String) jsonMessage.get(MessageKeys.RECORD_ID), (String) jsonMessage.get(MessageKeys.REMOTE_SERVER_NAME));
							System.out.println(output);
							continue;
						}
						case Config.EXIT:
						{
							// Exit System
							System.out.println("\nLogging out and exiting system...\n");
							continue;
						}
						default:
						{
							System.out.println("Invalid request type");
							continue;
						}
					}
				}
			}
		}
		catch (ParseException | SocketException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(aSocket != null)
			{
				aSocket.close();
			}
		}
    }

	private void sendRecordCount() throws IOException
	{
		byte[] m = (location + ":" + serverManager.getRecordCount(records)).getBytes();
		DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
		aSocket.send(reply);
	}

	private void checkRecords(String recordID) throws IOException
	{
		byte[] m = new byte[1];
		Record retrievedRecord = serverManager.findRecordByID(recordID, records);

		if (retrievedRecord == null)
		{
			m[0] = 'Y';
		}
		else
		{
			m[0] = 'N';
		}

		DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
		aSocket.send(reply);
	}

	/*
     * ADD RECORD
     * recordData = [ RECORD_ID, FIRST_NAME, LAST_NAME, EMPLOYEEID, MAILID,
     { PROJECT ID } || { (PROJECT_ID, PROJECT_CLIENT, PROJECT_CLIENT_NAME) X N , LOCATION } ]
     */
	private void addRecord(byte[] recordData) throws IOException, ClassNotFoundException
	{
		String message = "";

        Record record = serverManager.byteToRecord(recordData);

		if (record.mRecordID.charAt(0) == 'E')
		{
			// Make employee record
			EmployeeRecord eRecord = (EmployeeRecord) record;

			System.out.println("Adding Employee Record " + record.mRecordID + " to " + location + " server.");
			eRecord.print();

			// Add record to map
            server.createRecord(eRecord);
            message = "success";

		}
		else if (record.mRecordID.charAt(0)  == 'M')
		{
			// Make manager Record
			ManagerRecord mRecord = (ManagerRecord) record;

			System.out.println("Adding Manager Record " + record.mRecordID + "  to " + location + " server.");
			mRecord.print();

			// Add record to map
            server.createRecord(mRecord);
            message = "success";
		}
		else
		{
			System.err.println("Error. Record Data is neither an ER or MR");
		}

		byte[] m = message.getBytes();
		DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
		aSocket.send(reply);
	}

	private Project[] getProjectArray(JSONArray recordData)
	{
		ArrayList<Project> projects =  new ArrayList<>();

		// Add projects to list
		for (Object obj : recordData)
		{
			JSONObject jsonProject = (JSONObject) obj;
			Project proj = new Project((String) jsonProject.get(MessageKeys.PROJECT_ID), (String) jsonProject.get(MessageKeys.PROJECT_CLIENT), (String) jsonProject.get(MessageKeys.PROJECT_NAME));
			projects.add(proj);
		}

		Project[] prj_arr = new Project[projects.size()];
		return projects.toArray(prj_arr);
	}

	private Project[] getProjectArray(String[] recordData, int startIndex)
	{
		ArrayList<Project> projects =  new ArrayList<>();

		// Add projects to list
		int i = startIndex;
		while (i < recordData.length-1 )
		{
			Project proj = new Project(recordData[i], recordData[i+1],recordData[i+2]);
			projects.add(proj);
			i +=3;
		}

		Project[] prj_arr = new Project[projects.size()];
		return projects.toArray(prj_arr);
	}
}
