package Replicas.Replica2;

import java.net.*;
import Replicas.Replica2.DataStructures.*;

import org.json.simple.*;
import org.json.simple.parser.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class UDPServerThread extends Thread
{
    private static final byte GET_COUNTS = 0x01, CHECK_RECORD = 0x02, ADD_RECORD = 0x03; 

    private CenterServer server;
    private HashMap<Character, ArrayList<Record>> records;
    private DatagramSocket aSocket = null;
    private DatagramPacket request = null;
    private int port;
    private String location;

    UDPServerThread (CenterServer server, int port, String location)
    {
        this.server = server;
        this.records = server.records;
        this.port = port;
        this.location = location;
    }

    @Override
    public void run()
    {
        try
        {
            aSocket = new DatagramSocket(port);
            System.out.println(location + "server UDP Socket started. Waiting for request...");
            
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
    String recordData = new String(request.getData()).trim();
    String[] data = recordData.split(":");
    addRecord(data);
}
else
{
    JSONParser parser = new JSONParser();
    JSONObject jsonMessage = (JSONObject) parser.parse(new String(request.getData()).trim());

    // recordData = [ SEQUENCE_ID, MANAGER_ID, MSG_ID, COMMAND_TYPE,
    // FIRST_NAME, LAST_NAME, EMPLOYEEID, MAILID,
    // { PROJECT ID } || { (PROJECT_ID, PROJECT_CLIENT, PROJECT_CLIENT_NAME) X N , LOCATION } ]

    switch (Integer.parseInt( (String) jsonMessage.get("commandType")))
    {
        case 1:
        {
            // Get projects
            JSONArray jsonProjects = (JSONArray) jsonMessage.get("projects");
            Project[] projects = getProjectArray(jsonProjects);
            System.out.println("Creating Record: " + jsonMessage.toJSONString());

            // Create Manager Record
            String msg = server.createMRecord((String) jsonMessage.get("managerID"),
                    (String) jsonMessage.get("firstName"),
                    (String) jsonMessage.get("lastName"),
                    Integer.parseInt((String) jsonMessage.get("employeeID")),
                    (String) jsonMessage.get("mailID"),
                    projects,
                    (String) jsonMessage.get("location"));
                            System.out.println(msg);
                            continue;
                        }
                        case 2:
                        {
                            // Create Employee Record
                            String msg = server.createERecord((String) jsonMessage.get("managerID"),
                            		(String) jsonMessage.get("firstName"),
                            		(String) jsonMessage.get("lastName"),
                            		Integer.parseInt((String) jsonMessage.get("employeeID")),
                            		(String) jsonMessage.get("mailID"),
                            		(String) jsonMessage.get("projectID"));
                            System.out.println(msg);
                            continue;
                        }
                        case 3:
                        {
                            // Get Record Count
                            String counts = server.getRecordCount((String) jsonMessage.get("managerID"));
                            System.out.println("Record count: " + counts);
                            continue;
                        }
                        case 4:
                        {
                            // Edit Record
                            String output = server.editRecord((String) jsonMessage.get("managerID"), (String) jsonMessage.get("recordID"), (String) jsonMessage.get("fieldName"), (String) jsonMessage.get("newValue"));
                            System.out.println("\n" + output);
                            continue;
                        }
                        case 5:
                        {
                            // Transfer Record
                            String output = server.transferRecord((String) jsonMessage.get("managerID"), (String) jsonMessage.get("recordID"), (String) jsonMessage.get("targetServer"));
                            System.out.println(output);
                            continue;
                        }
                        case 6:
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
        catch (ParseException | SocketException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            System.out.println("Socket already bound.");
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
        byte[] m = (location + ":" + server.records.size()).getBytes();
        DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
        aSocket.send(reply);
    }
    
    private void checkRecords(String recordID) throws IOException
    {    
        byte[] m = new byte[1];
        Record retrievedRecord = ServerManager.findRecordByID(recordID, records);
		
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
    private void addRecord(String[] recordData) throws IOException
    {
        String message = "";

         if (recordData[0].trim().charAt(0) == 'E') 
         {
            // Make employee record
            EmployeeRecord eRecord = new EmployeeRecord(recordData[1], recordData[2], Integer.parseInt(recordData[3]), recordData[4], recordData[5]);

            System.out.println("Adding Employee Record to " + location + " server.");
            eRecord.print();

            // Add record to map
//            message = map.addRecord(recordData[0].trim(), eRecord);

        }
        else if (recordData[0].trim().charAt(0) == 'M')
        {
            // Get projects
            Project[] projects = getProjectArray(recordData, 5);

            // Make manager Record
            ManagerRecord mRecord = new ManagerRecord(recordData[1], recordData[2], Integer.parseInt(recordData[3]), recordData[4], projects, recordData[recordData.length-1]);

            System.out.println("Adding Manager Record to " + location + " server.");
            mRecord.print();

            // Add record to map
//            message = map.addRecord(recordData[0].trim(), mRecord);
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

         System.out.println("Getting Project Array: " + recordData.toJSONString());

         // Add projects to list
         for (Object obj : recordData)
         {
             JSONObject jsonProject = (JSONObject) obj;
             Project proj = new Project((String) jsonProject.get("projectID"), (String) jsonProject.get("projectClient"), (String) jsonProject.get("projectName"));
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