package Test;

import Replicas.Replica1.CenterServer;
import Replicas.Replica1.DataStructures.EmployeeRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.*;
import java.util.ArrayList;

public class MessageDeliveryTest {

    private MulticastSocket socket;
    private DatagramPacket packet1;
    private DatagramPacket packet2;
    private EmployeeRecord eRecord;
    private InetAddress address;
    private CenterServerThread centerServerThread;

    private class CenterServerThread extends Thread {

        private CenterServer centerServer;

        public void run() {
            centerServer = new CenterServer();
            centerServer.runServers();
        }

        public CenterServer getCenterServer() {
            return centerServer;
        }

    }

    @Before
    public void setup() {
        try {
            address = InetAddress.getByName("228.5.6.7");
            socket = new MulticastSocket(6789);
            socket.joinGroup(address);
            centerServerThread = new CenterServerThread();
            centerServerThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendPacket1() {
        eRecord = new EmployeeRecord("John", "Smith", 123, "john@gmail.com", "P12345");
        //sequence_num:FE_data:msg_ID:param1:param2: ... :paramN
        String msg_str = "1:FE Data:" + eRecord.getData();

        byte[] msg = msg_str.getBytes();

        packet1 = new DatagramPacket(msg, msg.length, address, 6789);

        System.out.println("Sending packet");
        try {
            Thread.sleep(1000);
            socket.send(packet1);

            Thread.sleep(1000);
            ArrayList<String> msg_list = centerServerThread.getCenterServer().getDeliveryQueue().peek();
            System.out.println("In test: " + msg_list);
            System.out.println("Sequence number = " + msg_list.get(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        try {
            centerServerThread.getCenterServer().shutdownServers();
            centerServerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
