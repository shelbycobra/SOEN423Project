package Test;

import DEMS.Config;
import DEMS.MessageKeys;
import DEMS.Sequencer;
import Replicas.Replica1.CenterServer;
import Replicas.Replica2.Server;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.*;

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
            replica1Servers.runServers(0);
            replica2Servers.runServers(0);
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
        eRecord1.put(MessageKeys.COMMAND_TYPE, Config.CREATE_EMPLOYEE_RECORD);
        eRecord1.put(MessageKeys.MESSAGE_ID,"1");

        JSONObject eRecord2 = new JSONObject();
        eRecord2.put(MessageKeys.FIRST_NAME,"John");
        eRecord2.put(MessageKeys.LAST_NAME, "Smith");
        eRecord2.put(MessageKeys.EMPLOYEE_ID,"123");
        eRecord2.put(MessageKeys.MAIL_ID, "john@gmail.com");
        eRecord2.put(MessageKeys.PROJECT_ID, "P12345");
        eRecord2.put(MessageKeys.MANAGER_ID, "CA1234");
        eRecord2.put(MessageKeys.COMMAND_TYPE, Config.CREATE_EMPLOYEE_RECORD);
        eRecord2.put(MessageKeys.MESSAGE_ID,"2");

        JSONObject eRecord3 = new JSONObject();
        eRecord3.put(MessageKeys.FIRST_NAME,"John");
        eRecord3.put(MessageKeys.LAST_NAME, "Smith");
        eRecord3.put(MessageKeys.EMPLOYEE_ID,"123");
        eRecord3.put(MessageKeys.MAIL_ID, "john@gmail.com");
        eRecord3.put(MessageKeys.PROJECT_ID, "P12345");
        eRecord3.put(MessageKeys.MANAGER_ID, "CA1234");
        eRecord3.put(MessageKeys.COMMAND_TYPE, Config.CREATE_EMPLOYEE_RECORD);
        eRecord3.put(MessageKeys.MESSAGE_ID,"3");

        JSONObject eRecord4 = new JSONObject();
        eRecord4.put(MessageKeys.FIRST_NAME,"John");
        eRecord4.put(MessageKeys.LAST_NAME, "Smith");
        eRecord4.put(MessageKeys.EMPLOYEE_ID,"123");
        eRecord4.put(MessageKeys.MAIL_ID, "john@gmail.com");
        eRecord4.put(MessageKeys.PROJECT_ID, "P12345");
        eRecord4.put(MessageKeys.MANAGER_ID, "CA1234");
        eRecord4.put(MessageKeys.COMMAND_TYPE, Config.CREATE_EMPLOYEE_RECORD);
        eRecord4.put(MessageKeys.MESSAGE_ID,"4");

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
        mRecord1.put(MessageKeys.COMMAND_TYPE, Config.CREATE_MANAGER_RECORD);
        mRecord1.put(MessageKeys.MESSAGE_ID,"5");
        mRecord1.put(MessageKeys.PROJECTS, projects);
        mRecord1.put(MessageKeys.LOCATION, "CA");

        JSONObject mRecord2 = new JSONObject();
        mRecord2.put(MessageKeys.FIRST_NAME,"John");
        mRecord2.put(MessageKeys.LAST_NAME, "Smith");
        mRecord2.put(MessageKeys.EMPLOYEE_ID,"123");
        mRecord2.put(MessageKeys.MAIL_ID, "john@gmail.com");
        mRecord2.put(MessageKeys.MANAGER_ID, "US1234");
        mRecord2.put(MessageKeys.COMMAND_TYPE, Config.CREATE_MANAGER_RECORD);
        mRecord2.put(MessageKeys.MESSAGE_ID,"5");
        mRecord2.put(MessageKeys.PROJECTS, projects);
        mRecord2.put(MessageKeys.LOCATION, "US");

        JSONObject mRecord3 = new JSONObject();
        mRecord3.put(MessageKeys.FIRST_NAME,"John");
        mRecord3.put(MessageKeys.LAST_NAME, "Smith");
        mRecord3.put(MessageKeys.EMPLOYEE_ID,"123");
        mRecord3.put(MessageKeys.MAIL_ID, "john@gmail.com");
        mRecord3.put(MessageKeys.MANAGER_ID, "UK1234");
        mRecord3.put(MessageKeys.COMMAND_TYPE, Config.CREATE_MANAGER_RECORD);
        mRecord3.put(MessageKeys.MESSAGE_ID,"6");
        mRecord3.put(MessageKeys.PROJECTS, projects);
        mRecord3.put(MessageKeys.LOCATION, "UK");

        JSONObject mRecord4 = new JSONObject();
        mRecord4.put(MessageKeys.FIRST_NAME,"John");
        mRecord4.put(MessageKeys.LAST_NAME, "Smith");
        mRecord4.put(MessageKeys.EMPLOYEE_ID,"123");
        mRecord4.put(MessageKeys.MAIL_ID, "john@gmail.com");
        mRecord4.put(MessageKeys.MANAGER_ID, "CA1234");
        mRecord4.put(MessageKeys.COMMAND_TYPE, Config.CREATE_MANAGER_RECORD);
        mRecord4.put(MessageKeys.MESSAGE_ID,"7");
        mRecord4.put(MessageKeys.PROJECTS, projects);
        mRecord4.put(MessageKeys.LOCATION, "CA");

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
        getRecords1.put(MessageKeys.COMMAND_TYPE, Config.GET_RECORD_COUNT);

        JSONObject editRecord1 = new JSONObject();
        editRecord1.put(MessageKeys.MANAGER_ID, "CA1234");
        editRecord1.put(MessageKeys.MESSAGE_ID,"10");
        editRecord1.put(MessageKeys.COMMAND_TYPE, Config.EDIT_RECORD);
        editRecord1.put(MessageKeys.RECORD_ID, "ER00000");
        editRecord1.put(MessageKeys.FIELD_NAME, MessageKeys.MAIL_ID);
        editRecord1.put(MessageKeys.NEW_VALUE, "mail@mail.com");

        JSONObject editRecord2 = new JSONObject();
        editRecord2.put(MessageKeys.MANAGER_ID, "UK1234");
        editRecord2.put(MessageKeys.MESSAGE_ID,"11");
        editRecord2.put(MessageKeys.COMMAND_TYPE, Config.EDIT_RECORD);
        editRecord2.put(MessageKeys.RECORD_ID, "MR00001");
        editRecord2.put(MessageKeys.FIELD_NAME, MessageKeys.PROJECT_ID);
        editRecord2.put(MessageKeys.NEW_VALUE, "P99999");

        JSONObject editRecord3 = new JSONObject();
        editRecord3.put(MessageKeys.MANAGER_ID, "US1234");
        editRecord3.put(MessageKeys.MESSAGE_ID,"12");
        editRecord3.put(MessageKeys.COMMAND_TYPE, Config.EDIT_RECORD);
        editRecord3.put(MessageKeys.RECORD_ID, "MR00001");
        editRecord3.put(MessageKeys.FIELD_NAME, MessageKeys.PROJECT_CLIENT);
        editRecord3.put(MessageKeys.NEW_VALUE, "Another Corp.");

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
        transferRecord1.put(MessageKeys.COMMAND_TYPE, Config.TRANSFER_RECORD);
        transferRecord1.put(MessageKeys.RECORD_ID, "ER00000");
        transferRecord1.put(MessageKeys.REMOTE_SERVER_NAME, "US");

        JSONObject getRecords2 = new JSONObject();
        getRecords2.put(MessageKeys.MANAGER_ID, "UK1234");
        getRecords2.put(MessageKeys.MESSAGE_ID,"14");
        getRecords2.put(MessageKeys.COMMAND_TYPE, Config.GET_RECORD_COUNT);

        JSONObject transferRecord2 = new JSONObject();
        transferRecord2.put(MessageKeys.MANAGER_ID, "CA1234");
        transferRecord2.put(MessageKeys.MESSAGE_ID,"15");
        transferRecord2.put(MessageKeys.COMMAND_TYPE, Config.TRANSFER_RECORD);
        transferRecord2.put(MessageKeys.RECORD_ID, "ER00001");
        transferRecord2.put(MessageKeys.REMOTE_SERVER_NAME, "US");

        JSONObject getRecords3 = new JSONObject();
        getRecords3.put(MessageKeys.MANAGER_ID, "UK1234");
        getRecords3.put(MessageKeys.MESSAGE_ID,"16");
        getRecords3.put(MessageKeys.COMMAND_TYPE, Config.GET_RECORD_COUNT);

        byte[] msg13 = transferRecord1.toString().getBytes();
        byte[] msg14 = getRecords2.toString().getBytes();
        byte[] msg15 = transferRecord2.toString().getBytes();
        byte[] msg16 = getRecords3.toString().getBytes();

        JSONObject transferRecord3 = new JSONObject();
        transferRecord3.put(MessageKeys.MANAGER_ID, "CA1234");
        transferRecord3.put(MessageKeys.MESSAGE_ID,"17");
        transferRecord3.put(MessageKeys.COMMAND_TYPE, Config.TRANSFER_RECORD);
        transferRecord3.put(MessageKeys.RECORD_ID, "MR00002");
        transferRecord3.put(MessageKeys.REMOTE_SERVER_NAME, "US");

        JSONObject getRecords4 = new JSONObject();
        getRecords4.put(MessageKeys.MANAGER_ID, "US1234");
        getRecords4.put(MessageKeys.MESSAGE_ID,"18");
        getRecords4.put(MessageKeys.COMMAND_TYPE, Config.GET_RECORD_COUNT);

        JSONObject transferRecord4 = new JSONObject();
        transferRecord4.put(MessageKeys.MANAGER_ID, "CA1234");
        transferRecord4.put(MessageKeys.MESSAGE_ID,"19");
        transferRecord4.put(MessageKeys.COMMAND_TYPE, Config.TRANSFER_RECORD);
        transferRecord4.put(MessageKeys.RECORD_ID, "ER00002");
        transferRecord4.put(MessageKeys.REMOTE_SERVER_NAME, "US");

        JSONObject getRecords5 = new JSONObject();
        getRecords5.put(MessageKeys.MANAGER_ID, "US1234");
        getRecords5.put(MessageKeys.MESSAGE_ID,"20");
        getRecords5.put(MessageKeys.COMMAND_TYPE, Config.GET_RECORD_COUNT);

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
