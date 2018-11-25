package Replicas.Replica2;

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

public class Server implements Replica
{
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

    public Server()
    {
        // Instantiate Semaphores
        mutex = new Semaphore(1);
        deliveryQueueMutex = new Semaphore(0);

        // Instantiate Delivery Queue with the MessageComparator
        MessageComparator msgComp = new MessageComparator();
        deliveryQueue = new PriorityQueue<>(msgComp);
    }

    @Override
    public void runServers()
    {
        // Start up servers
        CA_DEMS_server = new ServerThread("CA", Config.Replica2.CA_PORT);
        UK_DEMS_server = new ServerThread("UK", Config.Replica2.UK_PORT);
        US_DEMS_server = new ServerThread("US", Config.Replica2.US_PORT);

        CA_DEMS_server.start();
        UK_DEMS_server.start();
        US_DEMS_server.start();

        // Setup Multicast Socket, ListenForPackets thread and ProcessMessages thread
        try
        {
            if (CA_DEMS_server.isAlive() && UK_DEMS_server.isAlive() && US_DEMS_server.isAlive())
            {
                setupMulticastSocket();
                listenForPackets = new ListenForPacketsThread();
                processMessages = new ProcessMessagesThread();
                listenForPackets.start();
                processMessages.start();
            }
        }
        catch (SocketException e)
        {
            System.out.println("Server Multicast Socket is closed.");
        }
        catch (InterruptedException e )
        {
            System.out.println("Server is shutting down.");
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void setupMulticastSocket() throws Exception
    {
        InetAddress group = InetAddress.getByName("228.5.6.7");
        socket = new MulticastSocket(Config.PortNumbers.SEQ_RE);
        socket.joinGroup(group);
    }

    public PriorityQueue<JSONObject> getDeliveryQueue()
    {
        return deliveryQueue;
    }

    @Override
    public void shutdownServers()
    {
        System.out.println("\nShutting down servers...\n");
        CA_DEMS_server.interrupt();
        UK_DEMS_server.interrupt();
        US_DEMS_server.interrupt();
        listenForPackets.interrupt();
        processMessages.interrupt();
        
        if (socket != null)
        {
            socket.close();
        }
    }

    @SuppressWarnings("unchecked")
	@Override
    public JSONArray getData()
    {
    	JSONArray caRecords = CA_DEMS_server.getRecords();
    	JSONArray usRecords = US_DEMS_server.getRecords();
    	JSONArray ukRecords = UK_DEMS_server.getRecords();
    	JSONArray allRecords = new JSONArray();
    	
    	for (Object record : caRecords)
    	{
    		allRecords.add((JSONObject) record);
    	}
    	
    	for (Object record : usRecords)
    	{
    		allRecords.add((JSONObject) record);
    	}
    	
    	for (Object record : ukRecords)
    	{
    		allRecords.add((JSONObject) record);
    	}
    	
    	return allRecords;
    }

    @SuppressWarnings("unchecked")
	@Override
    public void setData(JSONArray array)
    {
    	JSONArray caRecords = new JSONArray();
    	JSONArray usRecords = new JSONArray();
    	JSONArray ukRecords = new JSONArray();
    	
    	for (Object record : array)
    	{
    		String location = ((JSONObject) record).get(MessageKeys.SERVER_LOCATION).toString();
    		
    		if (location.equals("CA"))
    		{
    			caRecords.add((JSONObject) record);
    		}
    		else if (location.equals("CA"))
    		{
    			usRecords.add((JSONObject) record);
    		}
    		else
    		{
    			ukRecords.add((JSONObject) record);
			}
    	}
    	
    	CA_DEMS_server.setRecords(caRecords);
    	US_DEMS_server.setRecords(usRecords);
    	UK_DEMS_server.setRecords(ukRecords);
    }

    private class ListenForPacketsThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                while (true)
                {
                    byte[] buffer = new byte[1000];
                    DatagramPacket message = new DatagramPacket(buffer, buffer.length);
                    socket.receive(message);

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
            }
            catch (InterruptedException | IOException e)
            {
                e.printStackTrace();
                System.out.println("ListenForPacketsThread is shutting down");
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("unchecked")
		private void sendACK(Integer num) throws IOException
        {
            JSONObject jsonAck = new JSONObject();
            jsonAck.put(MessageKeys.SEQUENCE_NUMBER, num);
            jsonAck.put(MessageKeys.COMMAND_TYPE, Config.ACK);
            byte[] ack = jsonAck.toString().getBytes();
            DatagramSocket socket = new DatagramSocket(11000);
            DatagramPacket packet = new DatagramPacket(ack, ack.length, InetAddress.getByName(Config.IPAddresses.SEQUENCER), Config.PortNumbers.FE_SEQ);
            socket.send(packet);
            socket.close();
        }
    }

    private class ProcessMessagesThread extends Thread
    {
        @Override
        public void run()
        {
            System.out.println("CenterServer: Processing messages\n");
            try
            {
                while (true)
                {
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
                }
            }
            catch (InterruptedException e)
            {
                System.out.println("ProcessMessageThread is shutting down.");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        private void sendMessageToServer(JSONObject message)
        {
            if (message == null)
            {
                System.out.println("Cannot send null to servers");
                return;
            }

            int port = setPortNumber(((String) message.get(MessageKeys.MANAGER_ID)).substring(0,2));

            try
            {
                // Remove msg from delivery queue
                mutex.acquire();
                deliveryQueue.remove(message);
                mutex.release();

                // Setup Server Socket
                InetAddress address = InetAddress.getByName("localhost");
                DatagramSocket serverSocket = new DatagramSocket();
                byte[] buffer = message.toString().getBytes();

                // Send packet
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                serverSocket.send(packet);
            }
            catch (SocketException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        private int setPortNumber(String location)
        {
            if ("CA".equals(location))
            {
                return Config.Replica2.CA_PORT;
            }
            else if ("UK".equals(location))
            {
                return Config.Replica2.UK_PORT;
            }
            else if ("US".equals(location))
            {
                return Config.Replica2.US_PORT;
            }
            
            return 0;
        }
    }
}