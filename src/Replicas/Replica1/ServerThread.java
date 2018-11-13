package Replicas.Replica1;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class ServerThread implements Runnable {


    private String location;
    private int UDP_port;
    
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

            DEMSImpl demsImpl = new DEMSImpl(location, log);

            UDPServerThread UDPThread = new UDPServerThread(demsImpl.getMap(), UDP_port, location);

            Thread udp_thd = new Thread(UDPThread);

            udp_thd.start();

            udp_thd.join();
        } catch (Exception e) {
            System.err.println("ERROR: " + e);
        }
        System.out.println("DEMSServer exiting...");
    }
    
    /*
     *  WRITE TO LOG FILE
     */
    private void writeToLogFile(String message, BufferedWriter log) throws IOException {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMMMMMM dd, yyyy HH:mm:ss");
        log.write( sdf.format(cal.getTime()) + " - "+ message + "\n");
        log.flush();
    }
}
