package Replicas.Replica1;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

public class CenterServer {

    private static final int CA_PORT = 3500, UK_PORT = 4500, US_PORT = 5500;

    private Thread CA_server;
    private Thread UK_server;
    private Thread US_server;
    private MulticastSocket socket;
    private PriorityQueue<ArrayList<String>> deliveryQueue;

    public CenterServer() {
        MessageComparator msgComp = new MessageComparator();
        deliveryQueue = new PriorityQueue<>(msgComp);
    }

    public void runServers() {

        ServerThread CA_DEMS_server = new ServerThread("CA", CA_PORT);
        ServerThread UK_DEMS_server = new ServerThread("UK", UK_PORT);
        ServerThread US_DEMS_server = new ServerThread("US", US_PORT);

        CA_server = new Thread(CA_DEMS_server);
        UK_server = new Thread(UK_DEMS_server);
        US_server = new Thread(US_DEMS_server);

        CA_server.start();
        UK_server.start();
        US_server.start();

        try {
            if (CA_server.isAlive() && UK_server.isAlive() && US_server.isAlive()) {
                setupMulticastSocket();
                waitForMessages();
            }
        } catch (SocketException e) {
            System.out.println("CenterServer Socket is closed.");
        } catch (Exception e) {
            e.printStackTrace();
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
            msg_data = (new String(message.getData())).trim().split(":");
            ArrayList<String> msg_list = new ArrayList<>(Arrays.asList(msg_data));
            deliveryQueue.add(msg_list);
        }
    }

    public PriorityQueue<ArrayList<String>> getDeliveryQueue() {
        return deliveryQueue;
    }

    public void shutdownServers() throws InterruptedException {
        System.out.println("\nShutting down servers...\n");
        CA_server.interrupt();
        CA_server.join();
        UK_server.interrupt();
        UK_server.join();
        US_server.interrupt();
        US_server.join();
        if (socket != null)
            socket.close();
    }

}

