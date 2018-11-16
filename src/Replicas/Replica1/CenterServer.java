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
    private Semaphore deliveryQueueMutex;

    private ListenForPacketsThread listenForPackets;
    private ProcessMessageThread processMessage;

    private class ListenForPacketsThread extends Thread {

        @Override
        public void run() {
            try {

                String msg_data;
                while (true) {
                    byte[] buffer = new byte[256];
                    DatagramPacket message = new DatagramPacket(buffer, buffer.length);
                    socket.receive(message);
                    msg_data = (new String(message.getData())).trim();
                    mutex.acquire();
                    deliveryQueue.add(msg_data);
                    mutex.release();

                    deliveryQueueMutex.release();
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
            try {
                while (true) {

                    Thread.sleep(100);
                    deliveryQueueMutex.acquire();

                    int seqNum;
                    while ((seqNum = Integer.parseInt(deliveryQueue.peek().split(":")[0])) != lastSequenceNumber + 1);
                    lastSequenceNumber = seqNum;
                    sendMessageToServer(deliveryQueue.peek());

                }
            } catch (InterruptedException e) {
                System.out.println("ProcessMessageThread is shutting down.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendMessageToServer(String msg){

            System.out.println("Sending message to server: " + msg);
            // Set port number
            int port = setPortNumber(msg.split(":")[1].substring(0,2));

            try {
                // Remove msg from delivery queue
                mutex.acquire();
                deliveryQueue.remove(msg);
                mutex.release();

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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        private int setPortNumber(String location) {
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
        deliveryQueueMutex = new Semaphore(0);
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
                processMessage = new ProcessMessageThread();
                processMessage.start();
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
        processMessage.interrupt();
        if (socket != null)
            socket.close();
    }
}

