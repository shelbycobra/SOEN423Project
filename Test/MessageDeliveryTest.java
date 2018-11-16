package Test;

import Replicas.Replica1.CenterServer;
import Replicas.Replica1.DataStructures.EmployeeRecord;
import jdk.nashorn.internal.ir.debug.JSONWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.*;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

public class MessageDeliveryTest {

    private MulticastSocket socket;
    private EmployeeRecord eRecord;
    private InetAddress address;
    private CenterServerThread centerServerThread;
    private PriorityQueue deliveryQueue;


    private class CenterServerThread extends Thread {

        private CenterServer centerServer;

        public void run() {
            centerServer = new CenterServer();
            deliveryQueue = centerServer.getDeliveryQueue();
            centerServer.runServers();
        }

        public CenterServer getCenterServer() {
            return centerServer;
        }

        public void shutdown() {
            centerServer.shutdownServers();
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
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendTenPackets() {
//        setup();
        try {
            eRecord = new EmployeeRecord("John", "Smith", 123, "john@gmail.com", "P12345");

            // Creating messages that follow the following format
            //sequence_num:ManagerID:msg_ID:command_type:param1:param2: ... :paramN
            String msg_str_1 = "1:CA1234:1:2:" + eRecord.getData();
            String msg_str_2 = "2:CA1234:2:2:" + eRecord.getData();
            String msg_str_3 = "3:CA1234:3:2:" + eRecord.getData();
            String msg_str_4 = "4:CA1234:4:2:" + eRecord.getData();
            String msg_str_5 = "5:CA1234:5:2:" + eRecord.getData();
            String msg_str_6 = "6:CA1234:6:2:" + eRecord.getData();
            String msg_str_7 = "7:CA1234:7:2:" + eRecord.getData();
            String msg_str_8 = "8:CA1234:8:2:" + eRecord.getData();
            String msg_str_9 = "9:CA1234:9:2:" + eRecord.getData();
            String msg_str_10 = "10:CA1234:10:2:" + eRecord.getData();

            // Creating 10 message byte arrays
            byte[] msg1 = msg_str_1.getBytes();
            byte[] msg2 = msg_str_2.getBytes();
            byte[] msg3 = msg_str_3.getBytes();
            byte[] msg4 = msg_str_4.getBytes();
            byte[] msg5 = msg_str_5.getBytes();
            byte[] msg6 = msg_str_6.getBytes();
            byte[] msg7 = msg_str_7.getBytes();
            byte[] msg8 = msg_str_8.getBytes();
            byte[] msg9 = msg_str_9.getBytes();
            byte[] msg10= msg_str_10.getBytes();

            // Creating 10 datagram packets
            DatagramPacket packet1 = new DatagramPacket(msg1, msg1.length, address, 6789);
            DatagramPacket packet2 = new DatagramPacket(msg2, msg2.length, address, 6789);
            DatagramPacket packet3 = new DatagramPacket(msg3, msg3.length, address, 6789);
            DatagramPacket packet4 = new DatagramPacket(msg4, msg4.length, address, 6789);
            DatagramPacket packet5 = new DatagramPacket(msg5, msg5.length, address, 6789);
            DatagramPacket packet6 = new DatagramPacket(msg6, msg6.length, address, 6789);
            DatagramPacket packet7 = new DatagramPacket(msg7, msg7.length, address, 6789);
            DatagramPacket packet8 = new DatagramPacket(msg8, msg8.length, address, 6789);
            DatagramPacket packet9 = new DatagramPacket(msg9, msg9.length, address, 6789);
            DatagramPacket packet10 = new DatagramPacket(msg10, msg10.length, address, 6789);

            System.out.println("Sending packets");

//             Sending packets in reverse order
//            Thread.sleep(100);
            socket.send(packet10);
            Thread.sleep(10);
            socket.send(packet5);
            
            Thread.sleep(10);
            socket.send(packet4);
            
            Thread.sleep(10);
            socket.send(packet1);
            
            Thread.sleep(10);
            socket.send(packet3);
            
            Thread.sleep(10);
            socket.send(packet9);
            
            Thread.sleep(10);
            socket.send(packet8);
            
            Thread.sleep(10);
            socket.send(packet7);
            
            Thread.sleep(10);
            socket.send(packet6);
            
            Thread.sleep(10);
            socket.send(packet2);
            

            System.out.println("Sent all packets");

            // Getting top message

            while(deliveryQueue.size() > 0)
                Thread.sleep(10);

            String msg_list = (String) deliveryQueue.peek();

            // Getting new top message
            msg_list = (String) deliveryQueue.peek();

            // Top message should equal the next message in line.
            Assert.assertNotEquals(msg_str_2, msg_list);
            System.out.println("\nAt top of delivery queue: " + msg_list);

            // Making sure the delivery queue is empty
            Assert.assertNull(msg_list);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        tearDown();
    }

    @After
    public void tearDown() {
        try {
            Thread.sleep(1000);
            System.out.println("Tearing down");
            centerServerThread.shutdown();
            centerServerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
