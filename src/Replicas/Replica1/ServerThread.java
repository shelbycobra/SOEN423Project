package Replicas.Replica1;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class ServerThread extends Thread {

    private DEMSImpl demsImpl;
    private String location;
    private int UDP_port;
    
    public ServerThread (String location, int port) {
        this.location = location;
        UDP_port = port;
    }
    
    @Override
    public void run() {
        Thread udp_thd = null;
        BufferedWriter log = null;
        try {
            // Create log file
            String logFileName = "Logs/ServerLogs/" + location + "_server_log.txt";
            log = new BufferedWriter(new FileWriter(logFileName, true));

            System.out.println(location + "Server ready and waiting...");
            String msg = location + " server in running on port ";
            System.out.println(msg);
            writeToLogFile(msg, log);

            DEMSImpl demsImpl = new DEMSImpl(location, log);

            UDPServerThread UDPThread = new UDPServerThread(demsImpl, UDP_port, location);

            udp_thd = new Thread(UDPThread);

            udp_thd.start();

        } catch (Exception e) {
            System.err.println("ERROR: " + e);
        }
        try {
            if (udp_thd != null)
                udp_thd.join();
            if (log != null)
                log.close();
        } catch (InterruptedException e) {
            System.out.println("Shutting down " + location + " server");
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public DEMSImpl getDemsImpl() {
        return demsImpl;
    }
}
