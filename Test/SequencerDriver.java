package Test;

import DEMS.Sequencer;
import Replicas.Replica1.CenterServer;
import Replicas.Replica1.DataStructures.EmployeeRecord;
import org.junit.Before;
import org.junit.Test;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;

import static org.junit.Assert.*;

public class SequencerDriver {

    private Sequencer sequencer;
    private CenterServer centerServer;
    private SetupThread setupThread;

    private class SetupThread extends Thread {

        @Override
        public void run() {
            sequencer = new Sequencer();
            sequencer.startup();
        }
    }

    @Before
    public void setup() {
//        centerServer = new CenterServer();
//        centerServer.runServers();
        setupThread = new SetupThread();
        setupThread.start();
    }
    
    @Test
    public void testSequencer() {
        EmployeeRecord eRecord = new EmployeeRecord("John", "Smith", 123, "john@gmail.com", "P12345");

        // Creating messages that follow the following format
        //sequence_num:ManagerID:msg_ID:command_type:param1:param2: ... :paramN
        String msg_str_1 = "CA1234:1:2:" + eRecord.getData();
        String msg_str_2 = "CA1234:2:2:" + eRecord.getData();
        String msg_str_3 = "CA1234:3:2:" + eRecord.getData();
        String msg_str_4 = "CA1234:4:2:" + eRecord.getData();

        byte[] msg1 = msg_str_1.getBytes();
        byte[] msg2 = msg_str_2.getBytes();
        byte[] msg3 = msg_str_3.getBytes();
        byte[] msg4 = msg_str_4.getBytes();
        
        try {

            DatagramSocket socket = new DatagramSocket(8000);

            byte[] buffer = new byte[50];
            DatagramPacket receive = new DatagramPacket(buffer, buffer.length);

            socket.receive(receive);

            System.out.println(new String(receive.getData()).trim());

            InetAddress address = InetAddress.getLocalHost();

            DatagramPacket packet1 = new DatagramPacket(msg1, msg1.length, address, 8888);
            DatagramPacket packet2 = new DatagramPacket(msg2, msg2.length, address, 8888);
            DatagramPacket packet3 = new DatagramPacket(msg3, msg3.length, address, 8888);
            DatagramPacket packet4 = new DatagramPacket(msg4, msg4.length, address, 8888);

            socket.send(packet1);
            socket.send(packet2);
            socket.send(packet3);
            socket.send(packet4);

            System.out.println("Sent all packets");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
