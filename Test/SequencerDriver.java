package Test;

import DEMS.Sequencer;
import Replicas.Replica1.CenterServer;
import Replicas.Replica1.DataStructures.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.net.*;

import static org.junit.Assert.*;

public class SequencerDriver {

    private Sequencer sequencer;
    private CenterServer centerServer;
    private SetupThread setupThread;
    private CenterServerThread centerServerThread;
    private DatagramSocket socket;
    private InetAddress address;

    private class SetupThread extends Thread {

        @Override
        public void run() {
            sequencer = new Sequencer();
            sequencer.startup();
        }
    }

    private class CenterServerThread extends Thread {

        @Override
        public void run() {
            centerServer = new CenterServer();
            centerServer.runServers();
        }
    }

    @Before
    public void setup() {
        try {
            setupThread = new SetupThread();
            setupThread.start();
            centerServerThread = new CenterServerThread();
            centerServerThread.start();
            socket = new DatagramSocket();
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Sockets already bound");
        }
    }
    
    @Test
    public void testSequencerEmployeeRecords() {
        System.out.println("\n========================================");
        System.out.println("==== CREATING FOUR EMPLOYEE RECORDS ====");
        System.out.println("========================================\n");

        EmployeeRecord eRecord1 = new EmployeeRecord("John", "Smith", 123, "john@gmail.com", "P12345");
        EmployeeRecord eRecord2 = new EmployeeRecord("Carl", "Santiago", 456, "carl@gmail.com", "P55665");
        EmployeeRecord eRecord3 = new EmployeeRecord("Jane", "Doe", 789, "jane@gmail.com", "P99663");
        EmployeeRecord eRecord4 = new EmployeeRecord("Cathy", "Diaz", 623, "cathy@gmail.com", "P23553");

        // Creating messages that follow the following format
        //sequence_num:ManagerID:msg_ID:command_type:param1:param2: ... :paramN
        String msg_str_1 = "CA1234:1:2:" + eRecord1.getData();
        String msg_str_2 = "CA1234:2:2:" + eRecord2.getData();
        String msg_str_3 = "CA1234:3:2:" + eRecord3.getData();
        String msg_str_4 = "UK1234:4:2:" + eRecord4.getData();

        byte[] msg1 = msg_str_1.getBytes();
        byte[] msg2 = msg_str_2.getBytes();
        byte[] msg3 = msg_str_3.getBytes();
        byte[] msg4 = msg_str_4.getBytes();

        sendFourRecords(msg1, msg2, msg3, msg4);

        System.out.println("\n=======================================");
        System.out.println("==== CREATING FOUR MANAGER RECORDS ====");
        System.out.println("=======================================\n");

        Project project = new Project("P12345", "Some Corp.", "Project 1");
        Project[] projs = {project};
        ManagerRecord mRecord1 = new ManagerRecord("John", "Smith", 123, "john@gmail.com", projs, "CA");
        ManagerRecord mRecord2 = new ManagerRecord("Carl", "Santiago", 456, "carl@gmail.com", projs, "UK");
        ManagerRecord mRecord3 = new ManagerRecord("Jane", "Doe", 789, "jane@gmail.com", projs, "US");
        ManagerRecord mRecord4 = new ManagerRecord("Cathy", "Diaz", 623, "cathy@gmail.com", projs, "CA");

        // Creating messages that follow the following format
        // sequence_num:ManagerID:msg_ID:command_type:param1:param2: ... :paramN
        String msg_str_5 = "CA1234:5:1:" + mRecord1.getData();
        String msg_str_6 = "UK1234:6:1:" + mRecord2.getData();
        String msg_str_7 = "US1234:7:1:" + mRecord3.getData();
        String msg_str_8 = "CA1234:8:1:" + mRecord4.getData();

        byte[] msg5 = msg_str_5.getBytes();
        byte[] msg6 = msg_str_6.getBytes();
        byte[] msg7 = msg_str_7.getBytes();
        byte[] msg8 = msg_str_8.getBytes();

        sendFourRecords(msg5, msg6, msg7, msg8);

        System.out.println("\n==============================");
        System.out.println("==== EDITING FOUR RECORDS ====");
        System.out.println("==============================\n");

        // Getting Record Count
        String msg_str_9 = "CA1234:9:3";
        String msg_str_10 = "CA1234:10:4:ER00000:mailID:mail@mail.com";
        String msg_str_11 = "UK1234:11:4:MR00001:projectID:P99999";
        String msg_str_12 = "US5533:12:4:MR00001:projectClient:Another Corp.";

        byte[] msg9 = msg_str_9.getBytes();
        byte[] msg10 = msg_str_10.getBytes();
        byte[] msg11 = msg_str_11.getBytes();
        byte[] msg12 = msg_str_12.getBytes();

        sendFourRecords(msg9, msg10, msg11, msg12);

        System.out.println("\n==================================");
        System.out.println("==== TRANSFERING FOUR RECORDS ====");
        System.out.println("==================================\n");

        String msg_str_13 = "CA1234:13:5:ER00000:UK";
        String msg_str_14 = "UK1234:14:3";
        String msg_str_15 = "CA1234:15:5:ER00001:UK";
        String msg_str_16 = "UK1244:16:3";

        byte[] msg13 = msg_str_13.getBytes();
        byte[] msg14 = msg_str_14.getBytes();
        byte[] msg15 = msg_str_15.getBytes();
        byte[] msg16 = msg_str_16.getBytes();

        String msg_str_17 = "CA1234:17:5:MR00002:UK";
        String msg_str_18 = "US1234:18:3";
        String msg_str_19 = "CA1234:19:5:ER00002:UK";
        String msg_str_20 = "US1234:20:3";

        byte[] msg17 = msg_str_17.getBytes();
        byte[] msg18 = msg_str_18.getBytes();
        byte[] msg19 = msg_str_19.getBytes();
        byte[] msg20 = msg_str_20.getBytes();

        sendFourRecords(msg13, msg14, msg15, msg16);
        sendFourRecords(msg17, msg18, msg19, msg20);
    }

    private void sendFourRecords(byte[] msg1, byte[] msg2, byte[] msg3, byte[] msg4) {
        try {
            DatagramPacket packet1 = new DatagramPacket(msg1, msg1.length, address, 8000);
            DatagramPacket packet2 = new DatagramPacket(msg2, msg2.length, address, 8000);
            DatagramPacket packet3 = new DatagramPacket(msg3, msg3.length, address, 8000);
            DatagramPacket packet4 = new DatagramPacket(msg4, msg4.length, address, 8000);

            Thread.sleep(150);
            socket.send(packet1);
            Thread.sleep(150);
            socket.send(packet2);
            Thread.sleep(150);
            socket.send(packet3);
            Thread.sleep(150);
            socket.send(packet4);

            System.out.println("Sent all packets");
            Thread.sleep(1000);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void shutdown() {
        centerServer.shutdownServers();
    }
}
