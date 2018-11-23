package Replicas.Replica1;

import org.json.simple.JSONArray;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class ServerThread extends Thread {

    private String location;
    private int UDP_port;
    private DEMSImpl demsImpl;

    public ServerThread (String location, int port) {
        this.location = location;
        UDP_port = port;
    }
    
    @Override
    public void run() {
        UDPServerThread UDPThread = null;
        BufferedWriter log = null;
        try {
            // Create log file
            String logFileName = "Logs/ServerLogs/" + location + "_server_log.txt";
            log = new BufferedWriter(new FileWriter(logFileName, true));

            System.out.println(location + " server ready and waiting...");
            String msg = location + " server is running on port " + UDP_port + "\n";
            System.out.println(msg);
            writeToLogFile(msg, log);

            demsImpl = new DEMSImpl(location, log);

            UDPThread = new UDPServerThread(demsImpl, UDP_port, location);

            UDPThread.start();

        } catch (Exception e) {
            System.err.println("ERROR: " + e);
        }
        try {
            if (UDPThread != null)
                UDPThread.join();
            if (log != null)
                log.close();
        } catch (InterruptedException e) {
            System.out.println("Shutting down " + location + " server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    JSONArray getData(){
        return demsImpl.getMap().getData();
    }

    void setData(JSONArray array) {
        demsImpl.getMap().setData(array);
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
