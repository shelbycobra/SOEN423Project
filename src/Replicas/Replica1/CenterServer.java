package Replicas.Replica1;

import DEMS.MessageKeys;
import DEMS.Replica;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.*;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import DEMS.UDPPortNumbers;

public class CenterServer implements Replica {

    private ServerThread CA_DEMS_server;
    private ServerThread UK_DEMS_server;
    private ServerThread US_DEMS_server;
    private ListenForPacketsThread listenForPackets;
    private ProcessMessagesThread processMessages;

    private int lastSequenceNumber = 0;
    private MulticastSocket socket;
    private PriorityQueue<JSONObject> deliveryQueue;
    private Semaphore mutex; // Used to ensure that the delivery queue is thread safe
    private Semaphore deliveryQueueMutex; // Used to signal the Process Messages Thread to start processing a message
    private JSONParser parser = new JSONParser();
    private AtomicBoolean keepRunning = new AtomicBoolean(true);

    private class ListenForPacketsThread extends Thread {

        @Override
        public void run() {
            try {
                while (keepRunning.get()) {
                    byte[] buffer = new byte[1000];
                    DatagramPacket message = new DatagramPacket(buffer, buffer.length);
                    socket.receive(message);

//                    System.out.println("Received Message = " + new String(message.getData()));
                    // Get message string
                    JSONObject jsonMessage = (JSONObject) parser.parse(new String(message.getData()).trim());

                    // Immediately send "SeqNum:ACK" after receiving a message
                    int seqNum =  Integer.parseInt( (String) jsonMessage.get(MessageKeys.SEQUENCE_NUMBER));
                    sendACK(seqNum);

                    // Add message to delivery queue
                    mutex.acquire();
                    deliveryQueue.add(jsonMessage);
                    mutex.release();

                    // Signal to Process Messages Thread to start processing a message from the queue
                    deliveryQueueMutex.release();
                }
            } catch (InterruptedException | IOException e) {
                System.out.println("ListenForPacketsThread is shutting down");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        private void sendACK(Integer num) throws IOException {
            JSONObject jsonAck = new JSONObject();
            jsonAck.put(MessageKeys.SEQUENCE_NUMBER, num);
            jsonAck.put(MessageKeys.COMMAND_TYPE, "ACK");
            byte[] ack = jsonAck.toString().getBytes();
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
                while (keepRunning.get()) {

                    // Sleep a bit so that the message can be added to the queue
                    Thread.sleep(300);
                    deliveryQueueMutex.acquire();
                    int seqNum;
                    int nextSequenceNumber = lastSequenceNumber + 1;

                    while ((seqNum = Integer.parseInt( (String) deliveryQueue.peek().get(MessageKeys.SEQUENCE_NUMBER))) < nextSequenceNumber)
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
            }
        }

        private void sendMessageToServer(JSONObject message){

            if (message == null) {
                System.out.println("Cannot send null to servers");
                return;
            }

            int port = setPortNumber(((String) message.get(MessageKeys.MANAGER_ID)).substring(0,2));

            try {
                // Remove msg from delivery queue
                mutex.acquire();
                deliveryQueue.remove(message);
                mutex.release();

                // Setup Server Socket
                InetAddress address = InetAddress.getLocalHost();
                DatagramSocket serverSocket = new DatagramSocket();
                byte[] buffer = message.toString().getBytes();
                System.out.println("CenterServer msg to server = " + message.toString());
                // Send packet
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                serverSocket.send(packet);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        private int setPortNumber(String location) {
            if ("CA".equals(location))
                return UDPPortNumbers.R1_CA_PORT;
            if ("UK".equals(location))
                return UDPPortNumbers.R1_UK_PORT;
            if ("US".equals(location))
                return UDPPortNumbers.R1_US_PORT;
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

    @Override
    public void runServers() {

        // Start up servers
        CA_DEMS_server = new ServerThread("CA", UDPPortNumbers.R1_CA_PORT);
        UK_DEMS_server = new ServerThread("UK", UDPPortNumbers.R1_UK_PORT);
        US_DEMS_server = new ServerThread("US", UDPPortNumbers.R1_US_PORT);

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
            e.printStackTrace();
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

    public PriorityQueue<JSONObject> getDeliveryQueue() {
        return deliveryQueue;
    }

    @Override
    public void shutdownServers() {
        System.out.println("\nShutting down servers...\n");
        keepRunning.set(false);
        CA_DEMS_server.interrupt();
        UK_DEMS_server.interrupt();
        US_DEMS_server.interrupt();
        listenForPackets.interrupt();
        processMessages.interrupt();
        if (socket != null)
            socket.close();
    }

    @Override
    public JSONArray getData() {

        JSONArray CAServerArray = CA_DEMS_server.getData();
        JSONArray UKServerArray = UK_DEMS_server.getData();
        JSONArray USServerArray = US_DEMS_server.getData();

        JSONArray allRecordsArray = new JSONArray();

        int maxSize = CAServerArray.size() > UKServerArray.size() ? CAServerArray.size() : (UKServerArray.size() > USServerArray.size() ? UKServerArray.size() : USServerArray.size());

        for(int i = 0; i < maxSize; i++) {
            if (i < CAServerArray.size())
                allRecordsArray.add(CAServerArray.get(i));
            if (i < UKServerArray.size())
                allRecordsArray.add(UKServerArray.get(i));
            if (i < USServerArray.size())
                allRecordsArray.add(USServerArray.get(i));
        }

        return allRecordsArray;
    }

    @Override
    public void setData(JSONArray array) {

    }
}

