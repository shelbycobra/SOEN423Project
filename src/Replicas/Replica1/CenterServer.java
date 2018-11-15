package Replicas.Replica1;

import java.io.IOException;
import java.net.*;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

public class CenterServer {

    private static final int CA_PORT = 3500, UK_PORT = 4500, US_PORT = 5500;

    private ServerThread CA_DEMS_server;
    private ServerThread UK_DEMS_server;
    private ServerThread US_DEMS_server;
    private MulticastSocket socket;
    private PriorityQueue<String> deliveryQueue;
    private int lastSequenceNumber = 0;
    private Semaphore mutex;

    private ListenForPacketsThread listenForPackets;
    private ProcessMessageThread processMessageThread;
    
    private class ListenForPacketsThread extends Thread {
        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1000];
                String msg_data;
                while (true) {
                    System.out.println("listen");
                    DatagramPacket message = new DatagramPacket(buffer, buffer.length);
                    socket.receive(message);
                    mutex.acquire();
                        msg_data = (new String(message.getData())).trim();
                        deliveryQueue.add(msg_data);
                    mutex.release();
                }
            } catch (IOException e) {
                System.out.println("ListenForPacketsThread is shutting down");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ProcessMessageThread extends Thread {
        @Override
        public void run() {
            System.out.println("Processing messages");

            while (true) {
                System.out.println("process");
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (deliveryQueue.peek() != null) {
                    int seqNum = Integer.parseInt(deliveryQueue.peek().split(":")[0]);
                    System.out.println(seqNum);
                    if (seqNum == lastSequenceNumber + 1) {
                        lastSequenceNumber = seqNum;
                        System.out.println("Last Sequence Number: " + lastSequenceNumber);
                        sendMessageToServer(deliveryQueue.peek());
                    }
                }
            }
        }

        private void sendMessageToServer(String msg){
            // Set port number
            int port = setPortNumber(msg.split(":")[1].substring(0,2));
            System.out.println("Port = " + port);

            // Remove msg from delivery queue
            deliveryQueue.remove(msg);

            try {
                // Setup Server Socket
                InetAddress address = InetAddress.getByName("localhost");
                DatagramSocket serverSocket = new DatagramSocket();
                byte[] buffer = msg.getBytes();

                // Send packet
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                serverSocket.send(packet);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private int setPortNumber(String location) {
            System.out.println("Location = " + location);
            if ("CA".equals(location))
                return CA_PORT;
            else if ("UK".equals(location)) {
                return UK_PORT;
            }
            else if ("US".equals(location)) {
                return US_PORT;
            }
            return 0;
        }
    }

    public CenterServer() {
        mutex = new Semaphore(1);
        MessageComparator msgComp = new MessageComparator();
        deliveryQueue = new PriorityQueue<>(msgComp);
    }

    public void runServers() {
        CA_DEMS_server = new ServerThread("CA", CA_PORT);
        UK_DEMS_server = new ServerThread("UK", UK_PORT);
        US_DEMS_server = new ServerThread("US", US_PORT);

        CA_DEMS_server.start();
        UK_DEMS_server.start();
        US_DEMS_server.start();

        try {
            if (CA_DEMS_server.isAlive() && UK_DEMS_server.isAlive() && US_DEMS_server.isAlive()) {
                setupMulticastSocket();
                listenForPackets = new ListenForPacketsThread();
                listenForPackets.start();
                processMessageThread = new ProcessMessageThread();
                processMessageThread.start();
            }
        } catch (SocketException e) {
            System.out.println("CenterServer Socket is closed.");
        } catch (InterruptedException e ) {
            System.out.println("CenterServer is shutting down.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupMulticastSocket() throws Exception {
        InetAddress group = InetAddress.getByName("228.5.6.7");
        socket = new MulticastSocket(6789);
        socket.joinGroup(group);
    }

    public PriorityQueue<String> getDeliveryQueue() {
        return deliveryQueue;
    }

    public void shutdownServers() {
        System.out.println("\nShutting down servers...\n");
        CA_DEMS_server.interrupt();
        UK_DEMS_server.interrupt();
        US_DEMS_server.interrupt();
        listenForPackets.interrupt();
        processMessageThread.interrupt();
        if (socket != null)
            socket.close();
    }
}

