package Test;

import DEMS.Sequencer;
import Replicas.Replica1.CenterServer;
import Replicas.Replica1.DataStructures.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.net.*;

import static org.junit.Assert.*;

public class SequencerTest {

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

        JSONObject eRecord1 = new JSONObject();
        eRecord1.put("firstName","John");
        eRecord1.put("lastName", "Smith");
        eRecord1.put("employeeID","123");
        eRecord1.put("mailID", "john@gmail.com");
        eRecord1.put("projectID", "P12345");
        eRecord1.put("managerID", "CA1234");
        eRecord1.put("commandType","2");
        eRecord1.put("messageID","1");

        JSONObject eRecord2 = new JSONObject();
        eRecord2.put("firstName","John");
        eRecord2.put("lastName", "Smith");
        eRecord2.put("employeeID","123");
        eRecord2.put("mailID", "john@gmail.com");
        eRecord2.put("projectID", "P12345");
        eRecord2.put("managerID", "CA1234");
        eRecord2.put("commandType","2");
        eRecord2.put("messageID","2");

        JSONObject eRecord3 = new JSONObject();
        eRecord3.put("firstName","John");
        eRecord3.put("lastName", "Smith");
        eRecord3.put("employeeID","123");
        eRecord3.put("mailID", "john@gmail.com");
        eRecord3.put("projectID", "P12345");
        eRecord3.put("managerID", "CA1234");
        eRecord3.put("commandType","2");
        eRecord3.put("messageID","3");

        JSONObject eRecord4 = new JSONObject();
        eRecord4.put("firstName","John");
        eRecord4.put("lastName", "Smith");
        eRecord4.put("employeeID","123");
        eRecord4.put("mailID", "john@gmail.com");
        eRecord4.put("projectID", "P12345");
        eRecord4.put("managerID", "CA1234");
        eRecord4.put("commandType","2");
        eRecord4.put("messageID","4");

//        EmployeeRecord eRecord1 = new EmployeeRecord("John", "Smith", 123, "john@gmail.com", "P12345");
//        EmployeeRecord eRecord2 = new EmployeeRecord("Carl", "Santiago", 456, "carl@gmail.com", "P55665");
//        EmployeeRecord eRecord3 = new EmployeeRecord("Jane", "Doe", 789, "jane@gmail.com", "P99663");
//        EmployeeRecord eRecord4 = new EmployeeRecord("Cathy", "Diaz", 623, "cathy@gmail.com", "P23553");

        // Creating messages that follow the following format
        //sequence_num:ManagerID:msg_ID:command_type:param1:param2: ... :paramN

        byte[] msg1 = eRecord1.toString().getBytes();
        byte[] msg2 = eRecord2.toString().getBytes();
        byte[] msg3 = eRecord3.toString().getBytes();
        byte[] msg4 = eRecord4.toString().getBytes();

        sendFourRecords(msg1, msg2, msg3, msg4);

        System.out.println("\n=======================================");
        System.out.println("==== CREATING FOUR MANAGER RECORDS ====");
        System.out.println("=======================================\n");


        JSONArray projects = new JSONArray();
        JSONObject project = new JSONObject();
        project.put("projectID", "P12345");
        project.put("projectName", "Project");
        project.put("projectClient", "Corp.");
        projects.add(project);

        JSONObject mRecord1 = new JSONObject();
        mRecord1.put("firstName","John");
        mRecord1.put("lastName", "Smith");
        mRecord1.put("employeeID","123");
        mRecord1.put("mailID", "john@gmail.com");
        mRecord1.put("managerID", "CA1234");
        mRecord1.put("commandType","1");
        mRecord1.put("messageID","5");
        mRecord1.put("projects", projects);
        mRecord1.put("location", "CA");

        JSONObject mRecord2 = new JSONObject();
        mRecord2.put("firstName","John");
        mRecord2.put("lastName", "Smith");
        mRecord2.put("employeeID","123");
        mRecord2.put("mailID", "john@gmail.com");
        mRecord2.put("managerID", "US1234");
        mRecord2.put("commandType","1");
        mRecord2.put("messageID","5");
        mRecord2.put("projects", projects);
        mRecord2.put("location", "US");

        JSONObject mRecord3 = new JSONObject();
        mRecord3.put("firstName","John");
        mRecord3.put("lastName", "Smith");
        mRecord3.put("employeeID","123");
        mRecord3.put("mailID", "john@gmail.com");
        mRecord3.put("managerID", "UK1234");
        mRecord3.put("commandType","1");
        mRecord3.put("messageID","6");
        mRecord3.put("projects", projects);
        mRecord3.put("location", "UK");

        JSONObject mRecord4 = new JSONObject();
        mRecord4.put("firstName","John");
        mRecord4.put("lastName", "Smith");
        mRecord4.put("employeeID","123");
        mRecord4.put("mailID", "john@gmail.com");
        mRecord4.put("managerID", "CA1234");
        mRecord4.put("commandType","1");
        mRecord4.put("messageID","7");
        mRecord4.put("projects", projects);
        mRecord4.put("location", "CA");

//        Project project = new Project("P12345", "Some Corp.", "Project 1");
//        Project[] projs = {project};
//        ManagerRecord mRecord1 = new ManagerRecord("John", "Smith", 123, "john@gmail.com", projs, "CA");
//        ManagerRecord mRecord2 = new ManagerRecord("Carl", "Santiago", 456, "carl@gmail.com", projs, "UK");
//        ManagerRecord mRecord3 = new ManagerRecord("Jane", "Doe", 789, "jane@gmail.com", projs, "US");
//        ManagerRecord mRecord4 = new ManagerRecord("Cathy", "Diaz", 623, "cathy@gmail.com", projs, "CA");

        // Creating messages that follow the following format
        // sequence_num:ManagerID:msg_ID:command_type:param1:param2: ... :paramN
//        String msg_str_5 = "CA1234:5:1:" + mRecord1.getData();
//        String msg_str_6 = "UK1234:6:1:" + mRecord2.getData();
//        String msg_str_7 = "US1234:7:1:" + mRecord3.getData();
//        String msg_str_8 = "CA1234:8:1:" + mRecord4.getData();

        byte[] msg5 = mRecord1.toString().getBytes();
        byte[] msg6 = mRecord2.toString().getBytes();
        byte[] msg7 = mRecord3.toString().getBytes();
        byte[] msg8 = mRecord4.toString().getBytes();

        sendFourRecords(msg5, msg6, msg7, msg8);

        System.out.println("\n==============================");
        System.out.println("==== EDITING FOUR RECORDS ====");
        System.out.println("==============================\n");

        // Getting Record Count

        JSONObject getRecords1 = new JSONObject();
        getRecords1.put("managerID", "CA1234");
        getRecords1.put("messageID","9");
        getRecords1.put("commandType","3");

        JSONObject editRecord1 = new JSONObject();
        editRecord1.put("managerID", "CA1234");
        editRecord1.put("messageID","10");
        editRecord1.put("commandType","4");
        editRecord1.put("recordID", "ER00000");
        editRecord1.put("fieldName", "mailID");
        editRecord1.put("newValue", "mail@mail.com");

        JSONObject editRecord2 = new JSONObject();
        editRecord2.put("managerID", "UK1234");
        editRecord2.put("messageID","11");
        editRecord2.put("commandType","4");
        editRecord2.put("recordID", "MR00001");
        editRecord2.put("fieldName", "projectID");
        editRecord2.put("newValue", "P99999");

        JSONObject editRecord3 = new JSONObject();
        editRecord3.put("managerID", "US1234");
        editRecord3.put("messageID","12");
        editRecord3.put("commandType","4");
        editRecord3.put("recordID", "MR00001");
        editRecord3.put("fieldNamw", "projectClient");
        editRecord3.put("newValue", "Another Corp.");

//        String msg_str_9 = "CA1234:9:3";
//        String msg_str_10 = "CA1234:10:4:ER00000:mailID:mail@mail.com";
//        String msg_str_11 = "UK1234:11:4:MR00001:projectID:P99999";
//        String msg_str_12 = "US5533:12:4:MR00001:projectClient:Another Corp.";

        byte[] msg9 = getRecords1.toString().getBytes();
        byte[] msg10 = editRecord1.toString().getBytes();
        byte[] msg11 = editRecord2.toString().getBytes();
        byte[] msg12 = editRecord3.toString().getBytes();

        sendFourRecords(msg9, msg10, msg11, msg12);

        System.out.println("\n==================================");
        System.out.println("==== TRANSFERING FOUR RECORDS ====");
        System.out.println("==================================\n");

        JSONObject transferRecord1 = new JSONObject();
        transferRecord1.put("managerID", "CA1234");
        transferRecord1.put("messageID","13");
        transferRecord1.put("commandType","5");
        transferRecord1.put("recordID", "ER00000");
        transferRecord1.put("targetServer", "UK");

        JSONObject getRecords2 = new JSONObject();
        getRecords2.put("managerID", "UK1234");
        getRecords2.put("messageID","14");
        getRecords2.put("commandType","3");

        JSONObject transferRecord2 = new JSONObject();
        transferRecord2.put("managerID", "CA1234");
        transferRecord2.put("messageID","15");
        transferRecord2.put("commandType","5");
        transferRecord2.put("recordID", "ER00001");
        transferRecord2.put("targetServer", "UK");

        JSONObject getRecords3 = new JSONObject();
        getRecords3.put("managerID", "UK1234");
        getRecords3.put("messageID","16");
        getRecords3.put("commandType","3");

//        String msg_str_13 = "CA1234:13:5:ER00000:UK";
//        String msg_str_14 = "UK1234:14:3";
//        String msg_str_15 = "CA1234:15:5:ER00001:UK";
//        String msg_str_16 = "UK1244:16:3";

        byte[] msg13 = transferRecord1.toString().getBytes();
        byte[] msg14 = getRecords2.toString().getBytes();
        byte[] msg15 = transferRecord2.toString().getBytes();
        byte[] msg16 = getRecords3.toString().getBytes();

        JSONObject transferRecord3 = new JSONObject();
        transferRecord3.put("managerID", "CA1234");
        transferRecord3.put("messageID","17");
        transferRecord3.put("commandType","5");
        transferRecord3.put("recordID", "MR00002");
        transferRecord3.put("targetServer", "UK");

        JSONObject getRecords4 = new JSONObject();
        getRecords4.put("managerID", "US1234");
        getRecords4.put("messageID","18");
        getRecords4.put("commandType","3");

        JSONObject transferRecord4 = new JSONObject();
        transferRecord4.put("managerID", "CA1234");
        transferRecord4.put("messageID","19");
        transferRecord4.put("commandType","5");
        transferRecord4.put("recordID", "ER00002");
        transferRecord4.put("targetServer", "UK");

        JSONObject getRecords5 = new JSONObject();
        getRecords5.put("managerID", "US1234");
        getRecords5.put("messageID","20");
        getRecords5.put("commandType","3");

//        String msg_str_17 = "CA1234:17:5:MR00002:UK";
//        String msg_str_18 = "US1234:18:3";
//        String msg_str_19 = "CA1234:19:5:ER00002:UK";
//        String msg_str_20 = "US1234:20:3";

        byte[] msg17 = transferRecord3.toString().getBytes();
        byte[] msg18 = getRecords4.toString().getBytes();
        byte[] msg19 = transferRecord4.toString().getBytes();
        byte[] msg20 = getRecords5.toString().getBytes();

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
