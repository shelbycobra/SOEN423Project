package Replicas.Replica1;

import java.net.*;
import Replicas.Replica1.DataStructures.*;
import java.io.*;
import java.util.ArrayList;

public class UDPServerThread implements Runnable {

    private static final byte GET_COUNTS = 0x01, CHECK_RECORD = 0x02, ADD_RECORD = 0X03; 

    private DEMSImpl demsImpl;
    private DEMSHashMap map;
    private DatagramSocket aSocket = null;
    private DatagramPacket request = null;
    private int port;
    private String location;

    public UDPServerThread (DEMSImpl demsImpl, int port, String location) {
        this.demsImpl = demsImpl;
        this.map = demsImpl.getMap();
        this.port = port;
        this.location = location;
    }

    @Override
    public void run() {
        try {
            aSocket = new DatagramSocket(port);
            byte[] buffer = new byte[256];
            System.out.println(location + "server UDP Socket started. Waiting for request...");
            
            while(true) 
            { 
                request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                byte request_type = request.getData()[0];
                
                if (request_type == GET_COUNTS)
                    sendRecordCount();
                else if (request_type == CHECK_RECORD) {
                    String recordID = new String(request.getData(), 0);
                    checkRecords(recordID);
                } else if (request_type == ADD_RECORD) {
                    String recordData = new String(request.getData());
                    String[] data = recordData.split(":");
                    addRecord(data);
                } else {
                    String[] recordData = new String(request.getData()).split(":");

                    // recordData = [ SEQUENCE_ID, MANAGER_ID, MSG_ID, COMMAND_TYPE,
                    // FIRST_NAME, LAST_NAME, EMPLOYEEID, MAILID,
                    // { PROJECT ID } || { (PROJECT_ID, PROJECT_CLIENT, PROJECT_CLIENT_NAME) X N , LOCATION } ]

                    switch (Integer.parseInt(recordData[3])){
                        case 1: {
                            // Get projects
                            Project[] projects = getProjectArray(recordData, 9);

                            // Call DEMSImpl method
                            String msg = demsImpl.createMRecord(recordData[1], recordData[4], recordData[5], Integer.parseInt(recordData[6]), recordData[7], projects, recordData[recordData.length-1]);

                            System.out.println(msg);
                            continue;
                        } case 2: {
                            // Call DEMSImpl method
                            String msg = demsImpl.createERecord(recordData[1], recordData[4], recordData[5], Integer.parseInt(recordData[6]), recordData[7], recordData[8]);

                            System.out.println(msg);
                            continue;
                        } case 3: {
                            String counts = demsImpl.getRecordCounts(recordData[1]);
                            System.out.println("Record count: " + counts);
                            continue;
                        } case 4: {
                            String output = demsImpl.editRecord(recordData[1], recordData[4], recordData[5], recordData[6]);
                            System.out.println("\n" + output);
                            continue;
                        } case 5: {
                            String output = demsImpl.transferRecord(recordData[1], recordData[4], recordData[5]);
                            System.out.println(output);
                            continue;
                        } case 6: {
                            System.out.println("\nLogging out and exiting system...\n");
                            continue;
                        } default: {
                            System.out.println("Invalid request type");
                            continue;
                        }
                    }

                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
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

     private Project[] getProjectArray(String[] recordData, int startIndex) {
         ArrayList<Project> projects =  new ArrayList<>();

         // Add projects to list
         int i = 5;
         while (i < recordData.length-1 ) {
             Project proj = new Project(recordData[i], recordData[i+1],recordData[i+2]);
             projects.add(proj);
             i +=3;
         }

         Project[] prj_arr = new Project[projects.size()];
         return projects.toArray(prj_arr);
     }
}
