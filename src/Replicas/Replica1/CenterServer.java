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
    private ListenForPacketsThread listenForPackets;
    private ProcessMessagesThread processMessages;

    private int lastSequenceNumber = 0;
    private MulticastSocket socket;
    private PriorityQueue<String> deliveryQueue;
    private Semaphore mutex; // Used to ensure that the delivery queue is thread safe
    private Semaphore deliveryQueueMutex; // Used to signal the Process Messages Thread to start processing a message

    private class ListenForPacketsThread extends Thread {

        @Override
        public void run() {
            try {
                String msg_data;
                while (true) {
                    byte[] buffer = new byte[256];
                    DatagramPacket message = new DatagramPacket(buffer, buffer.length);
                    socket.receive(message);

                    // Get message string
                    msg_data = (new String(message.getData())).trim();

                    // Immediately send "SeqNum:ACK" after receiving a message
                    int seqNum =  Integer.parseInt(msg_data.split(":")[0]);
                    sendACK(seqNum);

                    // Add message to delivery queue
                    mutex.acquire();
                    deliveryQueue.add(msg_data);
                    mutex.release();

                    // Signal to Process Messages Thread to start processing a message from the queue
                    deliveryQueueMutex.release();
                }
            }
            catch (IOException e) {
                System.out.println("ListenForPacketsThread is shutting down");
            }
            catch (InterruptedException e) {
                System.out.println("ListenForPacketsThread is shutting down");
            }
        }

        private void sendACK(int num) throws IOException {
            byte[] ack = (num+":ACK").getBytes();
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), 8000);
            socket.send(packet);
        }
    }

    private class ProcessMessagesThread extends Thread {

        @Override
        public void run() {
            System.out.println("CenterServer: Processing messages\n");
            try {
                while (true) {

                    // Sleep a bit so that the message can be added to the queue
                    Thread.sleep(300);
                    deliveryQueueMutex.acquire();
                    int seqNum;
                    int nextSequenceNumber = lastSequenceNumber + 1;

                    while ((seqNum = Integer.parseInt(deliveryQueue.peek().split(":")[0])) < nextSequenceNumber)
                    {
                        deliveryQueueMutex.acquire();

                        System.out.println("\n*** Removing duplicate [" + seqNum + "] ***\n");

                        mutex.acquire();
                        deliveryQueue.remove(deliveryQueue.peek());
                        mutex.release();
                    }
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

//            System.out.println("CenterServer: Sending message to server: " + msg);
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
        // Instantiate Semaphores
        mutex = new Semaphore(1);
        deliveryQueueMutex = new Semaphore(0);

        // Instantiate Delivery Queue with the MessageComparator
        MessageComparator msgComp = new MessageComparator();
        deliveryQueue = new PriorityQueue<>(msgComp);
    }

    public void runServers() {

        // Start up servers
        CA_DEMS_server = new ServerThread("CA", CA_PORT);
        UK_DEMS_server = new ServerThread("UK", UK_PORT);
        US_DEMS_server = new ServerThread("US", US_PORT);

        CA_DEMS_server.start();
        UK_DEMS_server.start();
        US_DEMS_server.start();

        // Setup Multicast Socket, ListenForPackets thread and ProcessMessages thread
        try {
            if (CA_DEMS_server.isAlive() && UK_DEMS_server.isAlive() && US_DEMS_server.isAlive()) {
                setupMulticastSocket();
                listenForPackets = new ListenForPacketsThread();
                processMessages = new ProcessMessagesThread();
                listenForPackets.start();
                processMessages.start();
            }
        } catch (SocketException e) {
            System.out.println("CenterServer Multicast Socket is closed.");
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
        processMessages.interrupt();
        if (socket != null)
            socket.close();
    }
}

