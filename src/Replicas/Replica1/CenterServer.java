package Replicas.Replica1;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

public class CenterServer {


    private static final int CA_PORT = 3500, UK_PORT = 4500, US_PORT = 5500;

    private MulticastSocket socket;
    private PriorityQueue<ArrayList<String>> deliveryQueue;
    private MessageComparator msgComp;

    public CenterServer() {
        msgComp = new MessageComparator();
        deliveryQueue = new PriorityQueue<>(msgComp);

    }

    public void runCenterServer() {


        ServerThread CA_DEMS_server = new ServerThread("CA", CA_PORT);
        ServerThread UK_DEMS_server = new ServerThread("UK", UK_PORT);
        ServerThread US_DEMS_server = new ServerThread("US", US_PORT);

        Thread CA_server = new Thread(CA_DEMS_server);
        Thread UK_server = new Thread(UK_DEMS_server);
        Thread US_server = new Thread(US_DEMS_server);

        CA_server.start();
        UK_server.start();
        US_server.start();

        try {
            setupMulticastSocket();
            waitForMessages();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null)
                socket.close();
        }
    }

    private void setupMulticastSocket() throws Exception {
        InetAddress group = InetAddress.getByName("228.5.6.7");
        socket = new MulticastSocket(6789);
        socket.joinGroup(group);
    }

    private void waitForMessages() throws Exception {
        byte[] buffer = new byte[1000];
        String[] msg_data;
        while (true) {
            DatagramPacket message = new DatagramPacket(buffer, buffer.length);
            socket.receive(message);
            msg_data = (new String(message.getData())).split(":");
            ArrayList<String> msg_list = new ArrayList<>(Arrays.asList(msg_data));
            deliveryQueue.add(msg_list);
        }
    }

}

