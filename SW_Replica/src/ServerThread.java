import DataStructures.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Arrays;
import java.util.ArrayList;

public class ServerThread implements Runnable {
    
    private static final byte GET_COUNTS = 0x01, CHECK_RECORD = 0x02, ADD_RECORD = 0X03; 
    
    private DEMSHashMap map =  new DEMSHashMap();
    private String location = "";
    private int UDP_port;
    private DatagramSocket aSocket = null;
    private DatagramPacket request = null;
    private DEMSImpl demsImpl;
    
    public ServerThread (String location, int port) {
        this.location = location;
        UDP_port = port;
    }
    
    @Override
    public void run() {
    
        BufferedWriter log;
        try {
            // Create log file
            String logFileName = "../Logs/ServerLogs/" + location + "_server_log.txt";
            log = new BufferedWriter(new FileWriter(logFileName, true));
            
            System.out.println(location + "Server ready and waiting...");
            String msg = location + " server in running on port ";
            System.out.println(msg);
            writeToLogFile(msg, log);
            listen();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR: " + e);
        } finally {
        if(aSocket != null)
            aSocket.close();
    }
        System.out.println("DEMSServer exiting...");
    }
    
    /*
     *  WRITE TO LOG FILE
     */
    private void writeToLogFile(String message, BufferedWriter log) throws IOException{
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMMMMMM dd, yyyy HH:mm:ss");
        log.write( sdf.format(cal.getTime()) + " - "+ message + "\n");
        log.flush();
    }

    public void listen() throws SocketException, IOException {
        aSocket = new DatagramSocket(UDP_port+1);
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
                String recordID = new String(request.getData());
                checkRecords(recordID);
            } else if (request_type == ADD_RECORD) {
                String str_data = new String(request.getData());
                String[] data = str_data.split(":");
                addRecord(data);
            } else
                System.out.println("Invalid request type");
        }
    }
    
    /*
    *  SEND RECORD COUNTS
    */
    private void sendRecordCount() throws SocketException, IOException {
        byte[] m = (location + ":" + map.getRecordCount()).getBytes();
        DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
        aSocket.send(reply);
    }
    
    /*
     * CHECK RECORDS
     */
     private void checkRecords(String recordID) throws SocketException, IOException{
        
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
            EmployeeRecord eRecord = new EmployeeRecord(recordData[1], recordData[2], Integer.parseInt(recordData[3]), recordData[4], recordData[5]);
            System.out.println("Adding Employee Record to " + location + " server.");
            eRecord.printData();
            message = map.addRecord(recordData[0].trim(), eRecord);
        } else if (recordData[0].trim().charAt(0) == 'M') {
            ArrayList<Project> projects =  new ArrayList<>();
            
            int i = 5;
            while (i < recordData.length-1 ) {
                Project proj = new Project(recordData[i], recordData[i+1],recordData[i+2]);
                projects.add(proj);
                i +=3;
            }
        
            Project[] prj_arr = new Project[projects.size()];
            ManagerRecord mRecord = new ManagerRecord(recordData[1], recordData[2], Integer.parseInt(recordData[3]), recordData[4], projects.toArray(prj_arr), recordData[recordData.length-1]);
            System.out.println("Adding Manager Record to " + location + " server.");
            mRecord.printData();
            message = map.addRecord(recordData[0].trim(), mRecord);
        } else {
            System.err.println("Error. Record Data is neither an ER or MR");
        }
        
        byte[] m = message.getBytes();
        DatagramPacket reply = new DatagramPacket(m, m.length, request.getAddress(), request.getPort());
        aSocket.send(reply);
     }
}
