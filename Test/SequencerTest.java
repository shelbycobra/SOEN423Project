package Test;

import DEMS.Sequencer;
import Replicas.Replica1.CenterServer;
import Replicas.Replica2.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.net.*;
import DEMS.MessageKeys;

public class SequencerTest {

    private Sequencer sequencer;
    private CenterServer replica1Servers;
    private Server replica2Servers;
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
            replica1Servers = new CenterServer();
            replica2Servers = new Server();
            replica1Servers.runServers();
            replica2Servers.runServers();
        }
    }

    @Before
    public void setup() {
        try {
            setupThread = new SetupThread();
            setupThread.start();
            centerServerThread = new CenterServerThread();
//            centerServerThread.start();
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
        eRecord1.put(MessageKeys.FIRST_NAME,"John");
        eRecord1.put(MessageKeys.LAST_NAME, "Smith");
        eRecord1.put(MessageKeys.EMPLOYEE_ID,"123");
        eRecord1.put(MessageKeys.MAIL_ID, "john@gmail.com");
        eRecord1.put(MessageKeys.PROJECT_ID, "P12345");
        eRecord1.put(MessageKeys.MANAGER_ID, "CA1234");
        eRecord1.put(MessageKeys.COMMAND_TYPE,"2");
        eRecord1.put(MessageKeys.MESSAGE_ID,"1");

        JSONObject eRecord2 = new JSONObject();
        eRecord2.put(MessageKeys.FIRST_NAME,"John");
        eRecord2.put(MessageKeys.LAST_NAME, "Smith");
        eRecord2.put(MessageKeys.EMPLOYEE_ID,"123");
        eRecord2.put(MessageKeys.MAIL_ID, "john@gmail.com");
        eRecord2.put(MessageKeys.PROJECT_ID, "P12345");
        eRecord2.put(MessageKeys.MANAGER_ID, "CA1234");
        eRecord2.put(MessageKeys.COMMAND_TYPE,"2");
        eRecord2.put(MessageKeys.MESSAGE_ID,"2");

        JSONObject eRecord3 = new JSONObject();
        eRecord3.put(MessageKeys.FIRST_NAME,"John");
        eRecord3.put(MessageKeys.LAST_NAME, "Smith");
        eRecord3.put(MessageKeys.EMPLOYEE_ID,"123");
        eRecord3.put(MessageKeys.MAIL_ID, "john@gmail.com");
        eRecord3.put(MessageKeys.PROJECT_ID, "P12345");
        eRecord3.put(MessageKeys.MANAGER_ID, "CA1234");
        eRecord3.put(MessageKeys.COMMAND_TYPE,"2");
        eRecord3.put(MessageKeys.MESSAGE_ID,"3");

        JSONObject eRecord4 = new JSONObject();
        eRecord4.put(MessageKeys.FIRST_NAME,"John");
        eRecord4.put(MessageKeys.LAST_NAME, "Smith");
        eRecord4.put(MessageKeys.EMPLOYEE_ID,"123");
        eRecord4.put(MessageKeys.MAIL_ID, "john@gmail.com");
        eRecord4.put(MessageKeys.PROJECT_ID, "P12345");
        eRecord4.put(MessageKeys.MANAGER_ID, "CA1234");
        eRecord4.put(MessageKeys.COMMAND_TYPE,"2");
        eRecord4.put(MessageKeys.MESSAGE_ID,"4");

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
        project.put(MessageKeys.PROJECT_ID, "P12345");
        project.put(MessageKeys.PROJECT_NAME, "Project");
        project.put(MessageKeys.PROJECT_CLIENT, "Corp.");
        projects.add(project);

        JSONObject mRecord1 = new JSONObject();
        mRecord1.put(MessageKeys.FIRST_NAME,"John");
        mRecord1.put(MessageKeys.LAST_NAME, "Smith");
        mRecord1.put(MessageKeys.EMPLOYEE_ID,"123");
        mRecord1.put(MessageKeys.MAIL_ID, "john@gmail.com");
        mRecord1.put(MessageKeys.MANAGER_ID, "CA1234");
        mRecord1.put(MessageKeys.COMMAND_TYPE,"1");
        mRecord1.put(MessageKeys.MESSAGE_ID,"5");
        mRecord1.put(MessageKeys.PROJECTS, projects);
        mRecord1.put(MessageKeys.LOCATION, "CA");

        JSONObject mRecord2 = new JSONObject();
        mRecord2.put(MessageKeys.FIRST_NAME,"John");
        mRecord2.put(MessageKeys.LAST_NAME, "Smith");
        mRecord2.put(MessageKeys.EMPLOYEE_ID,"123");
        mRecord2.put(MessageKeys.MAIL_ID, "john@gmail.com");
        mRecord2.put(MessageKeys.MANAGER_ID, "US1234");
        mRecord2.put(MessageKeys.COMMAND_TYPE,"1");
        mRecord2.put(MessageKeys.MESSAGE_ID,"5");
        mRecord2.put(MessageKeys.PROJECTS, projects);
        mRecord2.put(MessageKeys.LOCATION, "US");

        JSONObject mRecord3 = new JSONObject();
        mRecord3.put(MessageKeys.FIRST_NAME,"John");
        mRecord3.put(MessageKeys.LAST_NAME, "Smith");
        mRecord3.put(MessageKeys.EMPLOYEE_ID,"123");
        mRecord3.put(MessageKeys.MAIL_ID, "john@gmail.com");
        mRecord3.put(MessageKeys.MANAGER_ID, "UK1234");
        mRecord3.put(MessageKeys.COMMAND_TYPE,"1");
        mRecord3.put(MessageKeys.MESSAGE_ID,"6");
        mRecord3.put(MessageKeys.PROJECTS, projects);
        mRecord3.put(MessageKeys.LOCATION, "UK");

        JSONObject mRecord4 = new JSONObject();
        mRecord4.put(MessageKeys.FIRST_NAME,"John");
        mRecord4.put(MessageKeys.LAST_NAME, "Smith");
        mRecord4.put(MessageKeys.EMPLOYEE_ID,"123");
        mRecord4.put(MessageKeys.MAIL_ID, "john@gmail.com");
        mRecord4.put(MessageKeys.MANAGER_ID, "CA1234");
        mRecord4.put(MessageKeys.COMMAND_TYPE,"1");
        mRecord4.put(MessageKeys.MESSAGE_ID,"7");
        mRecord4.put(MessageKeys.PROJECTS, projects);
        mRecord4.put(MessageKeys.LOCATION, "CA");

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
        getRecords1.put(MessageKeys.MANAGER_ID, "CA1234");
        getRecords1.put(MessageKeys.MESSAGE_ID,"9");
        getRecords1.put(MessageKeys.COMMAND_TYPE,"3");

        JSONObject editRecord1 = new JSONObject();
        editRecord1.put(MessageKeys.MANAGER_ID, "CA1234");
        editRecord1.put(MessageKeys.MESSAGE_ID,"10");
        editRecord1.put(MessageKeys.COMMAND_TYPE,"4");
        editRecord1.put(MessageKeys.RECORD_ID, "ER00000");
        editRecord1.put(MessageKeys.FIELD_NAME, MessageKeys.MAIL_ID);
        editRecord1.put(MessageKeys.NEW_VALUE, "mail@mail.com");

        JSONObject editRecord2 = new JSONObject();
        editRecord2.put(MessageKeys.MANAGER_ID, "UK1234");
        editRecord2.put(MessageKeys.MESSAGE_ID,"11");
        editRecord2.put(MessageKeys.COMMAND_TYPE,"4");
        editRecord2.put(MessageKeys.RECORD_ID, "MR00001");
        editRecord2.put(MessageKeys.FIELD_NAME, MessageKeys.PROJECT_ID);
        editRecord2.put(MessageKeys.NEW_VALUE, "P99999");

        JSONObject editRecord3 = new JSONObject();
        editRecord3.put(MessageKeys.MANAGER_ID, "US1234");
        editRecord3.put(MessageKeys.MESSAGE_ID,"12");
        editRecord3.put(MessageKeys.COMMAND_TYPE,"4");
        editRecord3.put(MessageKeys.RECORD_ID, "MR00001");
        editRecord3.put(MessageKeys.FIELD_NAME, MessageKeys.PROJECT_CLIENT);
        editRecord3.put(MessageKeys.NEW_VALUE, "Another Corp.");

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
        System.out.println("==== TRANSFFERING FOUR RECORDS ====");
        System.out.println("==================================\n");

        JSONObject transferRecord1 = new JSONObject();
        transferRecord1.put(MessageKeys.MANAGER_ID, "CA1234");
        transferRecord1.put(MessageKeys.MESSAGE_ID,"13");
        transferRecord1.put(MessageKeys.COMMAND_TYPE,"5");
        transferRecord1.put(MessageKeys.RECORD_ID, "ER00000");
        transferRecord1.put(MessageKeys.REMOTE_SERVER_NAME, "UK");

        JSONObject getRecords2 = new JSONObject();
        getRecords2.put(MessageKeys.MANAGER_ID, "UK1234");
        getRecords2.put(MessageKeys.MESSAGE_ID,"14");
        getRecords2.put(MessageKeys.COMMAND_TYPE,"3");

        JSONObject transferRecord2 = new JSONObject();
        transferRecord2.put(MessageKeys.MANAGER_ID, "CA1234");
        transferRecord2.put(MessageKeys.MESSAGE_ID,"15");
        transferRecord2.put(MessageKeys.COMMAND_TYPE,"5");
        transferRecord2.put(MessageKeys.RECORD_ID, "ER00001");
        transferRecord2.put(MessageKeys.REMOTE_SERVER_NAME, "UK");

        JSONObject getRecords3 = new JSONObject();
        getRecords3.put(MessageKeys.MANAGER_ID, "UK1234");
        getRecords3.put(MessageKeys.MESSAGE_ID,"16");
        getRecords3.put(MessageKeys.COMMAND_TYPE,"3");

//        String msg_str_13 = "CA1234:13:5:ER00000:UK";
//        String msg_str_14 = "UK1234:14:3";
//        String msg_str_15 = "CA1234:15:5:ER00001:UK";
//        String msg_str_16 = "UK1244:16:3";

        byte[] msg13 = transferRecord1.toString().getBytes();
        byte[] msg14 = getRecords2.toString().getBytes();
        byte[] msg15 = transferRecord2.toString().getBytes();
        byte[] msg16 = getRecords3.toString().getBytes();

        JSONObject transferRecord3 = new JSONObject();
        transferRecord3.put(MessageKeys.MANAGER_ID, "CA1234");
        transferRecord3.put(MessageKeys.MESSAGE_ID,"17");
        transferRecord3.put(MessageKeys.COMMAND_TYPE,"5");
        transferRecord3.put(MessageKeys.RECORD_ID, "MR00002");
        transferRecord3.put(MessageKeys.REMOTE_SERVER_NAME, "UK");

        JSONObject getRecords4 = new JSONObject();
        getRecords4.put(MessageKeys.MANAGER_ID, "US1234");
        getRecords4.put(MessageKeys.MESSAGE_ID,"18");
        getRecords4.put(MessageKeys.COMMAND_TYPE,"3");

        JSONObject transferRecord4 = new JSONObject();
        transferRecord4.put(MessageKeys.MANAGER_ID, "CA1234");
        transferRecord4.put(MessageKeys.MESSAGE_ID,"19");
        transferRecord4.put(MessageKeys.COMMAND_TYPE,"5");
        transferRecord4.put(MessageKeys.RECORD_ID, "ER00002");
        transferRecord4.put(MessageKeys.REMOTE_SERVER_NAME, "UK");

        JSONObject getRecords5 = new JSONObject();
        getRecords5.put(MessageKeys.MANAGER_ID, "US1234");
        getRecords5.put(MessageKeys.MESSAGE_ID,"20");
        getRecords5.put(MessageKeys.COMMAND_TYPE,"3");

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

//    @After
    public void shutdown() {
        replica1Servers.shutdownServers();
    }
}
