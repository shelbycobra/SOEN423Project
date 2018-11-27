package Replicas.Replica1;

import DEMS.Config;
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

public class CenterServer implements Replica {

    private ServerThread CA_DEMS_server;
    private ServerThread UK_DEMS_server;
    private ServerThread US_DEMS_server;
    private ListenForPacketsThread listenForPackets;
    private ProcessMessagesThread processMessages;
    private CommunicateWithRMThread communicateWithRM;

    private int lastSequenceNumber = 0;
    private int numMessages = 0;
    private Config.Failure failureType = Config.Failure.NONE;
    private MulticastSocket socket;
    private PriorityQueue<JSONObject> deliveryQueue;
    private Semaphore mutex; // Used to ensure that the delivery queue is thread safe
    private Semaphore deliveryQueueMutex; // Used to signal the Process Messages Thread to start processing a message
    private Semaphore proceedWithMessagesMutex; // Used when the RM needs to communicate with the replica
    private JSONParser parser = new JSONParser();
    private AtomicBoolean keepRunning = new AtomicBoolean(true);

    /* ========================================= */
    /* ==== LISTEN FOR PACKETS THREAD CLASS ==== */
    /* ========================================= */

    private class ListenForPacketsThread extends Thread {

        @Override
        public void run() {
        	while (keepRunning.get()) {
	            try {
	                socket.setSoTimeout(1000);
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
	                
	            } catch (SocketTimeoutException e) {
	            	continue;
	            } catch (InterruptedException | IOException e) {
	            } catch (ParseException e) {
	                e.printStackTrace();
	            }
        	}

            System.out.println("ListenForPacketsThread is shutting down");
        	socket.close();
        }

        private void sendACK(Integer num) throws IOException {
            JSONObject jsonAck = new JSONObject();
            jsonAck.put(MessageKeys.SEQUENCE_NUMBER, num);
            jsonAck.put(MessageKeys.COMMAND_TYPE, Config.ACK);
            byte[] ack = jsonAck.toString().getBytes();
            DatagramSocket socket = new DatagramSocket(10000);
            DatagramPacket packet = new DatagramPacket(ack, ack.length, InetAddress.getByName(Config.IPAddresses.SEQUENCER), Config.PortNumbers.FE_SEQ);
            socket.send(packet);
            socket.close();
        }
    } // END THREAD CLASS

    /* ======================================= */
    /* ==== PROCESS MESSAGES THREAD CLASS ==== */
    /* ======================================= */

    private class ProcessMessagesThread extends Thread {

        DatagramSocket serverSocket;
        @Override
        public void run() {
            System.out.println("CenterServer: Processing messages\n");
            while (keepRunning.get()) {
            try {
            	serverSocket = new DatagramSocket();
            	serverSocket.setSoTimeout(1000);
                    
                   
                    // Sleep a bit so that the message can be added to the queue
                    Thread.sleep(300);
                    deliveryQueueMutex.acquire();
                    int seqNum;
                    int nextSequenceNumber = lastSequenceNumber + 1;

                    JSONObject obj = deliveryQueue.peek();

                    while ((seqNum = Integer.parseInt( (String) obj.get(MessageKeys.SEQUENCE_NUMBER))) < nextSequenceNumber)
                    {
                        deliveryQueueMutex.acquire();

                        System.out.println("\n*** Removing duplicate [" + seqNum + "] ***\n");

                        mutex.acquire();
                        deliveryQueue.remove(obj);
                        obj = deliveryQueue.peek();
                        mutex.release();
                    }

                    lastSequenceNumber = seqNum;
                    sendMessageToServer(obj);
                    numMessages++;
                
	            } catch (SocketTimeoutException e) {
	            	continue;
	            } catch (IOException e) {
	                e.printStackTrace();
	            } catch (InterruptedException e) {
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
            }

            System.out.println("ProcessMessageThread is shutting down.");
            if (serverSocket != null)
            	serverSocket.close();
        }

        private void sendMessageToServer(JSONObject message) throws InterruptedException, IOException {

            if (message == null) {
                System.out.println("Cannot send null to servers");
                return;
            }

            int port = setPortNumber(((String) message.get(MessageKeys.MANAGER_ID)).substring(0,2));

            // Remove msg from delivery queue
            mutex.acquire();
            deliveryQueue.remove(message);
            mutex.release();
            
         // Checks if failure should start
            if (checkWhenToStartFailure(message.get(MessageKeys.MESSAGE_ID).toString())) {
            	return;
            }

            // Setup Server Socket
            InetAddress address = InetAddress.getLocalHost();
            byte[] buffer = message.toString().getBytes();
            System.out.println("CenterServer msg to server = " + message.toString());
            // Send packet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

            proceedWithMessagesMutex.acquire();
            serverSocket.send(packet);
            proceedWithMessagesMutex.release();
        }

        private int setPortNumber(String location) {
            if ("CA".equals(location))
                return Config.Replica1.CA_PORT;
            if ("UK".equals(location))
                return Config.Replica1.UK_PORT;
            if ("US".equals(location))
                return Config.Replica1.US_PORT;
            return 0;
        }
    } // END THREAD CLASS

    /* ========================================== */
    /* ==== COMMUNICATE WITH RM THREAD CLASS ==== */
    /* ========================================== */
    private class CommunicateWithRMThread extends Thread {

        private DatagramSocket socket;

        @Override
        public void run(){

            try {
				socket = new DatagramSocket(Config.Replica1.RE_PORT);
			} catch (SocketException e1) {
				e1.printStackTrace();
			}

            System.out.println("\n*** Listening for packets from RM 1 on port "+Config.Replica1.RE_PORT+" ***\n");
            
        	while (keepRunning.get()) {
        		try {
                    byte[] buffer = new byte[1024*10];

        			socket.setSoTimeout(1000);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    System.out.println("\n*** Received message from RM: ***" + new String(packet.getData()).trim());
                    
                    JSONObject obj = (JSONObject) parser.parse(new String(packet.getData()).trim());
                    if (obj.get(MessageKeys.COMMAND_TYPE).toString().equals(Config.GET_DATA)) {
                    	System.out.println("\n*** Sending Data to RM ***");
                        sendDataToRM(packet.getPort());
                    } else if (obj.get(MessageKeys.COMMAND_TYPE).toString().equals(Config.SET_DATA)) {
                        JSONArray arr = (JSONArray) parser.parse(new String(obj.get(MessageKeys.MESSAGE).toString()).trim());
                        proceedWithMessagesMutex.acquire();
                        System.out.println("\n*** Received data from RM. Resetting Data ***\n");
                        setData(arr);
                        proceedWithMessagesMutex.release();
                    }
	            } catch (SocketTimeoutException e) {
	            	continue;
	            } catch (IOException e) {
	                e.printStackTrace();
	            } catch (ParseException e) {
	                e.printStackTrace();
	            } catch (InterruptedException e) {
	              
	            }
	        }
        	
            System.out.println("CommunicateWithRMThread is shutting down...");
            socket.close();
        }

        private void sendDataToRM(int port) throws IOException,InterruptedException {
            proceedWithMessagesMutex.acquire();
            JSONArray arr = getData();
            System.out.println("\n*** DATA: " + arr.toString().length());
            System.out.println(arr.toString());
            proceedWithMessagesMutex.release();
            byte[] buffer = arr.toString().getBytes();
            socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Config.IPAddresses.REPLICA1), port);
            socket.send(packet);
        }
    } // END THREAD CLASS

    /* =============================== */
    /* ==== CENTER SERVER METHODS ==== */
    /* =============================== */

    public CenterServer() {
        // Instantiate Semaphores
        mutex = new Semaphore(1);
        deliveryQueueMutex = new Semaphore(0);
        proceedWithMessagesMutex = new Semaphore(1);

        MessageComparator msgComp = new MessageComparator();
        deliveryQueue = new PriorityQueue<>(msgComp);
    }

    private void setupMulticastSocket() throws Exception {
        InetAddress group = InetAddress.getByName("228.5.6.7");
        socket = new MulticastSocket(Config.PortNumbers.SEQ_RE);
        socket.joinGroup(group);
    }

    @Override
    public void runServers(int i) {
        checkErrorType(i);
        keepRunning.set(true);
        // Start up servers
        CA_DEMS_server = new ServerThread("CA", Config.Replica1.CA_PORT);
        UK_DEMS_server = new ServerThread("UK", Config.Replica1.UK_PORT);
        US_DEMS_server = new ServerThread("US", Config.Replica1.US_PORT);
        
        try {

	        CA_DEMS_server.start();
	        UK_DEMS_server.start();
	        US_DEMS_server.start();

	        // Setup Multicast Socket, ListenForPackets thread and ProcessMessages thread
       
            if (CA_DEMS_server.isAlive() && UK_DEMS_server.isAlive() && US_DEMS_server.isAlive()) {
                setupMulticastSocket();
                listenForPackets = new ListenForPacketsThread();
                processMessages = new ProcessMessagesThread();
                communicateWithRM = new CommunicateWithRMThread();
                listenForPackets.start();
                processMessages.start();
                communicateWithRM.start();
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

    @Override
    public void shutdownServers() {
        System.out.println("\nShutting down servers...\n");
        keepRunning.set(false);

        socket.close();
        CA_DEMS_server.interrupt();
        UK_DEMS_server.interrupt();
        US_DEMS_server.interrupt();
        listenForPackets.interrupt();
        processMessages.interrupt();
        communicateWithRM.interrupt();
    
        try {
			CA_DEMS_server.join();
			UK_DEMS_server.join();
            US_DEMS_server.join();
            listenForPackets.join();
            processMessages.join();
            communicateWithRM.join();
            Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }

    @Override
    public JSONArray getData() {

        JSONArray CAServerArray = CA_DEMS_server.getData();
        JSONArray UKServerArray = UK_DEMS_server.getData();
        JSONArray USServerArray = US_DEMS_server.getData();

        JSONArray allRecordsArray = new JSONArray();

        int maxSize = CAServerArray.size() > UKServerArray.size() ? CAServerArray.size() : (UKServerArray.size() > USServerArray.size() ? UKServerArray.size() : USServerArray.size());

        for (int i = 0; i < maxSize; i++) {
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
    public void setData(JSONArray recordArray) {
        JSONArray CAServerArray = new JSONArray();
        JSONArray UKServerArray = new JSONArray();
        JSONArray USServerArray = new JSONArray();

        JSONObject record;
        for (int i = 0; i < recordArray.size(); i++) {
            record = (JSONObject) recordArray.get(i);

            String location = (String) record.get(MessageKeys.SERVER_LOCATION);
            if (("CA").equals(location))
                CAServerArray.add(record);
            else if (("UK").equals(location))
                UKServerArray.add(record);
            else if (("US").equals(location))
                USServerArray.add(record);
        }

        CA_DEMS_server.setData(CAServerArray);
        UK_DEMS_server.setData(UKServerArray);
        US_DEMS_server.setData(USServerArray);
    }

    /* ======================== */
    /* ==== ERROR CHECKING ==== */
    /* ======================== */

    private void checkErrorType(int type) {
        if (Config.Failure.BYZANTINE.ordinal() == type)
            failureType = Config.Failure.BYZANTINE;
        else if (Config.Failure.PROCESS_CRASH.ordinal() == type)
            failureType = Config.Failure.PROCESS_CRASH;
    }

    boolean checkWhenToStartFailure(String msgID) throws IOException, InterruptedException {
        if (numMessages >= Config.MESSAGE_DELAY) {
            switch(failureType) {
                case BYZANTINE:
                    byzantineFailure(msgID);
                    return true;
                case PROCESS_CRASH:
                	processCrashFailure();
                default:
                    return false;
            }
        } 
        return false;
    }

    void byzantineFailure(String msgID) throws IOException, InterruptedException {
        JSONObject obj = new JSONObject();
        obj.put(MessageKeys.STATUS_CODE, Config.StatusCode.FAIL);
        obj.put(MessageKeys.RM_PORT_NUMBER, Config.Replica1.RM_PORT);
        obj.put(MessageKeys.MESSAGE_ID, msgID);
        obj.put(MessageKeys.MESSAGE, "Byzantine Failure");
        
        byte[] buffer = obj.toString().getBytes();

        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Config.IPAddresses.FRONT_END), Config.PortNumbers.RE_FE);

        socket.send(packet);
        socket.close();
    }

    void processCrashFailure() {
        while (keepRunning.get());
    }
}

