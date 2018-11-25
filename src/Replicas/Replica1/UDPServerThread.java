package Replicas.Replica1;

import DEMS.Config;
import DEMS.MessageKeys;
import Replicas.Replica1.DataStructures.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class UDPServerThread extends Thread {

    private static final byte GET_COUNTS = 0x01, CHECK_RECORD = 0x02, ADD_RECORD = 0X03; 

    private DEMSImpl demsImpl;
    private DEMSHashMap map;
    private DatagramSocket aSocket = null;
    private DatagramPacket request = null;
    private int port;
    private String msgID;
    private String location;

    UDPServerThread (DEMSImpl demsImpl, int port, String location) {
        this.demsImpl = demsImpl;
        this.map = demsImpl.getMap();
        this.port = port;
        this.location = location;
    }

    @Override
    public void run() {
        try {
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
                    sendRecordCount();
                else if (request_type == CHECK_RECORD) {
                    String recordID = new String(request.getData());
                    checkRecords(recordID);
                } else if (request_type == ADD_RECORD) {
                    String recordData = new String(request.getData()).trim();
                    String[] data = recordData.split(":");
                    addRecord(data);
                }

                // All other message types
                else {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonMessage = (JSONObject) parser.parse(new String(request.getData()).trim());

                    msgID = (String) jsonMessage.get(MessageKeys.MESSAGE_ID);

                    // recordData = [ SEQUENCE_ID, MANAGER_ID, MSG_ID, COMMAND_TYPE,
                    // FIRST_NAME, LAST_NAME, EMPLOYEEID, MAILID,
                    // { PROJECT ID } || { (PROJECT_ID, PROJECT_CLIENT, PROJECT_CLIENT_NAME) X N , LOCATION } ]

                    switch (jsonMessage.get(MessageKeys.COMMAND_TYPE).toString()) {
                        case Config.CREATE_MANAGER_RECORD: {
                            // Get projects
                            JSONArray jsonProjects = (JSONArray) jsonMessage.get(MessageKeys.PROJECTS);
                            Project[] projects = getProjectArray(jsonProjects);
                            System.out.println("Creating Record: " + jsonMessage.toJSONString());

                            // Create Manager Record
                            String msg = demsImpl.createMRecord((String) jsonMessage.get(MessageKeys.MANAGER_ID),
                                    (String) jsonMessage.get(MessageKeys.FIRST_NAME),
                                    (String) jsonMessage.get(MessageKeys.LAST_NAME),
                                    Integer.parseInt( (String) jsonMessage.get(MessageKeys.EMPLOYEE_ID)),
                                    (String) jsonMessage.get(MessageKeys.MAIL_ID),
                                    projects,
                                    (String) jsonMessage.get(MessageKeys.LOCATION));
                            replyToFE(msg, Config.StatusCode.SUCCESS);
                            continue;
                        } case Config.CREATE_EMPLOYEE_RECORD: {
                            // Create Employee Record
                            String msg = demsImpl.createERecord((String) jsonMessage.get(MessageKeys.MANAGER_ID), (String) jsonMessage.get(MessageKeys.FIRST_NAME), (String) jsonMessage.get(MessageKeys.LAST_NAME), Integer.parseInt( (String) jsonMessage.get(MessageKeys.EMPLOYEE_ID)), (String) jsonMessage.get(MessageKeys.MAIL_ID), (String) jsonMessage.get(MessageKeys.PROJECT_ID));
                            replyToFE(msg, Config.StatusCode.SUCCESS);
                            continue;
                        } case Config.GET_RECORD_COUNT: {
                            // Get Record Count
                            String counts = demsImpl.getRecordCounts((String) jsonMessage.get(MessageKeys.MANAGER_ID));
                            replyToFE("Record count: " + counts, Config.StatusCode.SUCCESS);
                            continue;
                        } case Config.EDIT_RECORD: {
                            // Edit Record
                            String output = demsImpl.editRecord((String) jsonMessage.get(MessageKeys.MANAGER_ID), (String) jsonMessage.get(MessageKeys.RECORD_ID), (String) jsonMessage.get(MessageKeys.FIELD_NAME), (String) jsonMessage.get(MessageKeys.NEW_VALUE));
                            replyToFE(output, Config.StatusCode.SUCCESS);
                            continue;
                        } case Config.TRANSFER_RECORD: {
                            // Transfer Record
                            String output = demsImpl.transferRecord((String) jsonMessage.get(MessageKeys.MANAGER_ID), (String) jsonMessage.get(MessageKeys.RECORD_ID), (String) jsonMessage.get(MessageKeys.REMOTE_SERVER_NAME));
                            replyToFE(output, Config.StatusCode.SUCCESS);
                            continue;
                        } case Config.EXIT: {
                            // Exit System
                            System.out.println("\nLogging out and exiting system...\n");
                            continue;
                        } default: {
                            System.out.println("Invalid request type");
                            replyToFE("Invalid request type", Config.StatusCode.FAIL);
                        }
                    }
                }
            }
        } catch (ParseException | SocketException e) {
            e.printStackTrace();
        } catch (IOException e){
            System.out.println("Socket already bound.");
        } finally {
            if(aSocket != null) 
                aSocket.close();
        }
    }
    
    /*
    *  SEND RECORD COUNTS
    */
    private void sendRecordCount() throws IOException {
        byte[] m = (location + ":" + map.getRecordCount()).getBytes();
        DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
        aSocket.send(reply);
    }
    
    /*
     * CHECK RECORDS
     */
     private void checkRecords(String recordID) throws IOException{
        
        byte[] m = new byte[1];
        
        Record tmp = map.getRecord(recordID.trim());
        if (tmp == null){
            m[0] = 'Y';
        } else
            m[0] = 'N';
        
        DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
        aSocket.send(reply);
     }
     
     /*
      * ADD RECORD
      * recordData = [ RECORD_ID, FIRST_NAME, LAST_NAME, EMPLOYEEID, MAILID, 
      { PROJECT ID } || { (PROJECT_ID, PROJECT_CLIENT, PROJECT_CLIENT_NAME) X N , LOCATION } ]
      */
     private void addRecord(String[] recordData) throws IOException {

        String message = "";

         if (recordData[0].trim().charAt(0) == 'E') {

            // Make employee record
            EmployeeRecord eRecord = new EmployeeRecord(recordData[1], recordData[2], Integer.parseInt(recordData[3]), recordData[4], recordData[5]);

            System.out.println("Adding Employee Record to " + location + " server.");
            eRecord.printData();

            // Add record to map
            message = map.addRecord(recordData[0].trim(), eRecord);

        } else if (recordData[0].trim().charAt(0) == 'M') {

            // Get projects
            Project[] projects = getProjectArray(recordData, 5);

            // Make manager Record
            ManagerRecord mRecord = new ManagerRecord(recordData[1], recordData[2], Integer.parseInt(recordData[3]), recordData[4], projects, recordData[recordData.length-1]);

            System.out.println("Adding Manager Record to " + location + " server.");
            mRecord.printData();

            // Add record to map
            message = map.addRecord(recordData[0].trim(), mRecord);

        } else {
            System.err.println("Error. Record Data is neither an ER or MR");
        }
        
        byte[] m = message.getBytes();
        DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
        aSocket.send(reply);
     }

     private void replyToFE(String msg, Config.StatusCode status) throws IOException {

         DatagramSocket socket = new DatagramSocket();
         JSONObject message = new JSONObject();
         message.put(MessageKeys.MESSAGE, msg);
         message.put(MessageKeys.MESSAGE_ID, msgID);
         message.put(MessageKeys.RM_PORT_NUMBER, Config.Replica1.RM_PORT);
         message.put(MessageKeys.STATUS_CODE, status);

         byte[] buffer = message.toString().getBytes();
         DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Config.IPAddresses.FRONT_END), Config.PortNumbers.RE_FE);

         socket.send(packet);
     }

     private Project[] getProjectArray(JSONArray recordData) {
         ArrayList<Project> projects =  new ArrayList<>();

         // Add projects to list
         for (Object obj : recordData) {
             JSONObject jsonProject = (JSONObject) obj;
             Project proj = new Project((String) jsonProject.get(MessageKeys.PROJECT_ID), (String) jsonProject.get(MessageKeys.PROJECT_CLIENT), (String) jsonProject.get(MessageKeys.PROJECT_NAME));
             projects.add(proj);
         }

         Project[] prj_arr = new Project[projects.size()];
         return projects.toArray(prj_arr);
     }

    private Project[] getProjectArray(String[] recordData, int startIndex) {
        ArrayList<Project> projects =  new ArrayList<>();

        // Add projects to list
        int i = startIndex;
        while (i < recordData.length-1 ) {
            Project proj = new Project(recordData[i], recordData[i+1],recordData[i+2]);
            projects.add(proj);
            i +=3;
        }

        Project[] prj_arr = new Project[projects.size()];
        return projects.toArray(prj_arr);
    }
}
