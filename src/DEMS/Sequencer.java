package DEMS;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Sequencer {

    private final static int MAX_NUM_ACKS = 3, TIME_LIMIT = 50;

    private static int sequenceNumber = 1;
    private static ArrayDeque<JSONObject> deliveryQueue = new ArrayDeque<>();
    private static Map<Integer, SentMessage> sentMessagesHashMap = new HashMap<>();
    private static MulticastSocket multicastSocket;
    private static DatagramSocket datagramSocket;
    private static InetAddress group;
    private static Semaphore mutex = new Semaphore(1);
    private static Semaphore processMessageSem = new Semaphore(0);
    private static JSONParser parser;

    private static class SentMessage {

        private JSONObject message;
        private int numAcks = 0;
        private long creationTime;

        SentMessage(JSONObject message) {
            this.message = message;
            creationTime = System.currentTimeMillis();
        }

        int incrementNumAcks() {
            return ++numAcks;
        }

        JSONObject getMessage() {
            return message;
        }

        long getCreationTime() {
            return creationTime;
        }
    }

    private static class ListenForMessagesThread extends Thread {

        public void run() {
            try {

                while (true) {
                    byte[] buffer = new byte[1000];
                    DatagramPacket message = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(message);
                    String data = new String(message.getData()).trim();
                    JSONObject jsonMessage;
                    jsonMessage = (JSONObject) parser.parse(data);
                    System.out.println("Received message from Front End: " + jsonMessage);
                    // Check if received message is an ACK message
					try {
					    if ((jsonMessage.get(MessageKeys.COMMAND_TYPE)).equals(Config.ACK)) {
					        int ackSeqNum = Integer.parseInt("" + jsonMessage.get(MessageKeys.SEQUENCE_NUMBER));
					        System.out.println("\n*** Receiving ACK " + ackSeqNum + " from port " + message.getPort() + " ***\n");
					        processAck(ackSeqNum);
					    } else throw new NullPointerException();
					} catch (NullPointerException e) {
					    // Add message to queue
					    mutex.acquire();
					    deliveryQueue.add(jsonMessage);
					
					    // Tells Sequencer to start processing message
					    processMessageSem.release();
					    mutex.release();
					
					    // Send ACK to FE
					    sendAckToFE(jsonMessage.get(MessageKeys.MESSAGE_ID).toString());
					}
                }
            } catch (ParseException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        private void processAck(int ackSeqNum) throws IOException {

            if (sentMessagesHashMap.isEmpty()) {
                System.out.println("\n---Hash map is empty---\n");
                return;
            }

            long currentTime = System.currentTimeMillis();
            SentMessage msg = sentMessagesHashMap.get(ackSeqNum);

            long difference = currentTime - msg.getCreationTime();
            if (MAX_NUM_ACKS <= msg.incrementNumAcks()) {
                System.out.println("\nSequence Number " + ackSeqNum + " - All Replicas have successfully received message. Time: " + difference);
                sentMessagesHashMap.remove(ackSeqNum);
            } else {
                for (Map.Entry<Integer, SentMessage> entry : sentMessagesHashMap.entrySet()) {
                    difference = currentTime - entry.getValue().getCreationTime();
                    if (difference >= TIME_LIMIT){
                        System.out.println("\nSequence Number " + entry.getKey() + " - One or more replicas have not received the message in time. Time difference = " + difference);
                        resend(entry.getValue().getMessage());
                    }
                }
            }
        }

        private void resend(JSONObject message) throws IOException {
            System.out.println("Resending message: " + message.toString()+"\n");
            byte[] buffer = message.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, Config.PortNumbers.SEQ_RE);
            multicastSocket.send(packet);
        }

        private void sendAckToFE(String messageID) {
            try {
                DatagramSocket socket = new DatagramSocket();
                JSONObject message = new JSONObject();
                message.put(MessageKeys.MESSAGE_ID, messageID);
                message.put(MessageKeys.COMMAND_TYPE, Config.ACK);
                byte[] buffer = message.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Config.IPAddresses.FRONT_END), Config.PortNumbers.SEQ_FE);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main (String[] args) {
        startup();
    }

    public static void startup() {
        try {
            setupSockets();
            parser = new JSONParser();

            ListenForMessagesThread listenForMessages  = new ListenForMessagesThread();
            listenForMessages.start();
            processMessage();
        } catch (IOException e) {
            System.out.println("Socket already bound.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void setupSockets() throws IOException{
        System.out.println("Setting up sockets\n");
        group = InetAddress.getByName("228.5.6.7");
        multicastSocket = new MulticastSocket(Config.PortNumbers.SEQ_RE);
        multicastSocket.joinGroup(group);
        datagramSocket = new DatagramSocket(Config.PortNumbers.FE_SEQ);
    }

    private static void processMessage() throws IOException, InterruptedException {
        while (true) {
            // Wait until queue isn't empty
            processMessageSem.acquire();

            //  Add sequence number to message and send to all replicas
            JSONObject jsonMessage =  deliveryQueue.removeFirst();
            String num = ""+sequenceNumber;
            jsonMessage.put(MessageKeys.SEQUENCE_NUMBER, num);

            // Remove message from deliveryQueue and add it to sentMessageHashMap
            mutex.acquire();
            sentMessagesHashMap.put(sequenceNumber, new SentMessage(jsonMessage));
            mutex.release();

            byte[] buffer = jsonMessage.toString().getBytes();
            DatagramPacket message = new DatagramPacket(buffer, buffer.length, group, Config.PortNumbers.SEQ_RE);
            multicastSocket.send(message);

            // Increment sequence number
            sequenceNumber++;
        }
    }
}
