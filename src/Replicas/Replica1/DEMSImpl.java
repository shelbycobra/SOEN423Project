package Replicas.Replica1;

import java.io.BufferedWriter;
import java.io.IOException;

import Replicas.Replica1.DataStructures.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.net.*;
import java.lang.System;

public class DEMSImpl {
    
    private static final byte GET_COUNTS = 0x01, CHECK_RECORD = 0x02, ADD_RECORD = 0X03;
    private static final char YES = 'Y', NO = 'N';
    private static final int[] PORT_NUMS = {3500, 4500, 5500};
    private static final String[] PORT_NAMES= {"CA", "UK", "US"};
    private static final int NUM_THREADS = 3;
    private DEMSHashMap map;
    private String location;
    private BufferedWriter log;
    
    public DEMSImpl (String location, BufferedWriter log) {
        super();
        this.map = new DEMSHashMap();
        this.location = location;
        this.log = log;
    }
    
    //Implementation of createMRecord 
    public String createMRecord (String ManagerID, String firstName, String lastName, int employeeID, String mailID, Project[] projects, String location) {
        if (lastName.length() <1) 
            return "Last Name must not be empty";
        ManagerRecord mRecord = new ManagerRecord(firstName, lastName, employeeID, mailID, projects, location);
        String recordID = map.addRecord(mRecord, "MR");
        String msg = "Manager ID: " + ManagerID + "\nAdded Manager Record: " + recordID + "\n";
        System.out.println("\nServer: "+location + " - " + msg);
        msg += mRecord.printData();
        
        try {
            writeToLogFile(msg, log);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return msg;
    }
    
    //Implementation of createERecord
    public String createERecord (String ManagerID, String firstName, String lastName, int employeeID, String mailID, String projectID) {
        if (lastName.length() <1) 
            return "Last Name must not be empty";
        EmployeeRecord eRecord = new EmployeeRecord(firstName, lastName, employeeID, mailID, projectID);
        String recordID = map.addRecord(eRecord, "ER");
        String msg = "Manager ID: " + ManagerID + "\nAdded Employee Record: " + recordID + "\n";
        System.out.println("\nServer: "+location + " - " + msg);
        msg += eRecord.printData();
        
        try {
            writeToLogFile(msg, log);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return msg;
    }
    
    //Implementation of getRecordCounts
    public String getRecordCounts (String ManagerID) {
        String counts = "";
        
        try{
            DatagramSocket aSocket = new DatagramSocket();
            
            UDPRequestServerThread[] threads = new UDPRequestServerThread[NUM_THREADS];
            
            for (int i = 0; i < NUM_THREADS; i++)
                if (!PORT_NAMES[i].equals(location))
                    threads[i] = new UDPRequestServerThread(aSocket, PORT_NUMS[i], GET_COUNTS);
            
            for (UDPRequestServerThread thd : threads)
                if (thd != null)
                    thd.start();
            
            // let all threads catch up
            Thread.sleep(100);
            
            counts = location + ":" + this.map.getRecordCount() + " ";
            
            for (UDPRequestServerThread thd : threads) {
                if (thd != null) {
                    counts += thd.getMessage() + " ";
                    thd.join();
                    }
            }
            writeToLogFile("Record Counts: "+ counts, log);
        } catch (SocketException e) {
            e.getMessage();
        } catch (IOException e) {
            e.getMessage();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Manager ID: " + ManagerID + " - " + "Record Counts: "+ counts);
        return counts;
    }
    
    //Implementation of editRecord
    public String editRecord (String ManagerID, String recordID, String fieldName, String newValue) {
        if (newValue.length() <1) 
            return "New value must not be empty";
        String msg = "\nManager ID: " + ManagerID + " - " + "Server "+location + ":\n";
        boolean result = false;
        try {
            if (recordID.startsWith("ER")) {
                EmployeeRecord record = (EmployeeRecord) map.getRecord(recordID);
                if (record == null){
                    msg += "Record does not exist.";
                    writeToLogFile(msg, log);
                    System.out.println(msg);
                } else {
                    if (fieldName.equals("mailID"))
                        record.setMailID(newValue);
                    else if (fieldName.equals("projectID"))
                        record.setProjectID(newValue);
                    else {
                        msg += "DEMSClass: Invalid Field Name: " + fieldName;
                        writeToLogFile(msg, log);
                        System.out.println(msg);
                        return msg;
                    }
                    result = map.replaceRecord(record);
                    msg += "DEMSClass: Successfully edited employee record "+ recordID + "\n";
                    System.out.println(msg);
                    msg += record.printData();
                    writeToLogFile(msg, log);
                }
            } else if (recordID.startsWith("MR")) {
                ManagerRecord record = (ManagerRecord) map.getRecord(recordID);
                if (record == null){
                    msg += "Record does not exist.";
                    writeToLogFile(msg, log);
                } else {
                    if (fieldName.equals("mailID"))
                        record.setMailID(newValue);
                    else if (fieldName.trim().equals("location"))
                        record.setLocation(newValue);
                    else if (fieldName.equals("projectID")) {
                        Project project = record.getProject(0);
                        project.setProjectID(newValue);
                    } else if (fieldName.equals("projectClient")) {
                        Project project = record.getProject(0);
                        project.setProjectClient(newValue);
                    } else if (fieldName.equals("projectName")) {
                        Project project = record.getProject(0);
                        project.setProjectName(newValue);
                    } else {
                        msg += "DEMSClass: Invalid Field Name: "+ fieldName;
                        writeToLogFile(msg, log);
                        System.out.println(msg);
                        return msg;
                    }
                    result = map.replaceRecord(record);
                    msg += "DEMSClass: Successfully edited manager record "+ recordID + "\n";
                    System.out.println(msg);
                    msg += record.printData();
                    writeToLogFile(msg, log);
                }
            }
            if (result)
                return msg;
            else return "ERROR:Record does not exist in hashmap";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "ERROR: Somehow an invalid record ID got in here.";
    }
    
    //Implementation of transferRecord
    public synchronized String transferRecord (String ManagerID, String recordID, String remoteCenterServerName) {
        String msg = "";
        int port = setPortNumber(remoteCenterServerName);
        DatagramSocket aSocket = null;
        
        if (remoteCenterServerName.trim().equals(location))
            return "Entry already exists in database:" + map;
        
        try {
            InetAddress address = InetAddress.getByName("localhost");
            
            Record record = map.getRecord(recordID.trim());
            
            if (record == null) 
                return "Invalid Record ID: Record Does Not Exist in Current Database:" + map;
            
            //Create socket and buffer
            aSocket = new DatagramSocket();
            byte[] buffer = new byte[1024];
            buffer[0] = CHECK_RECORD;
            System.arraycopy(recordID.getBytes(), 0, buffer, 1, recordID.getBytes().length);
            
            //Send check record packet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port+1);
            aSocket.send(packet);
            
            //Receive answer
            packet = new DatagramPacket(buffer, buffer.length);
            aSocket.receive(packet);
            
            char answer = (char)buffer[0];
            if (answer == YES) {
                System.out.println("Record was not found in " + remoteCenterServerName + " database.\n" + ManagerID + " is transferring record " + recordID);
                buffer[0] = ADD_RECORD;
            
                String recordData = recordID + ":" + record.getData();
                
                System.arraycopy(recordData.getBytes(), 0, buffer, 1, recordData.getBytes().length);
                
                //Send transfer request.
                packet = new DatagramPacket(buffer, buffer.length, address, port+1);
                aSocket.send(packet);
                
                //receive answer
                packet = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(packet);
                msg += new String(packet.getData(), 0, packet.getLength());
                
                System.out.println("Removing record "+ recordID + " from " + location + " database.");
                map.removeRecord(recordID);
                
            } else {
                return msg += "Record already exists in the " + remoteCenterServerName + " server.";
            }
            
            writeToLogFile("Manager ID: " + ManagerID + " - " + msg, log);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        
        return "Record was successfully transferred. " + remoteCenterServerName + " server added record ID " + msg;
    }
    
    /**
     * 
     * @param message
     * @param log
     * @throws IOException
     */
    public static void writeToLogFile(String message, BufferedWriter log) throws IOException{
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMMMMMMMM dd, yyyy HH:mm:ss");
        log.write( sdf.format(cal.getTime()) + " - "+ message + "\n");
        log.flush();
    }
    
    private static int setPortNumber(String loc) {
        
        if ("CA".equals(loc)) 
            return 3500;
        else if ("UK".equals(loc))
            return 4500;
        else if ("US".equals(loc))
            return 5500;
        else 
            return 6000;
    }
    
    public DEMSHashMap getMap() {
        return map;
    }
}
