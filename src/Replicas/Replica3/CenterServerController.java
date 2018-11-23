package Replicas.Replica3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import DEMS.MessageKeys;

public class CenterServerController implements DEMS.Replica {

	private Logger logger;

	private Thread centerServerCA;
	private Thread centerServerUS;
	private Thread centerServerUK;

	private MulticastSocket multicastSocket;

	private Thread listenForPackets;
	private Thread processMessages;

	private int lastSequenceNumber = 0;
	private PriorityQueue<JSONObject> deliveryQueue;
	private Semaphore mutex;
	private Semaphore deliveryQueueMutex;
	private JSONParser jsonParser = new JSONParser();

	private class ListenForPacketsThread extends Thread {

		@Override
		public void run() {
			try {
				while (true) {
					byte[] buffer = new byte[1000];
					DatagramPacket message = new DatagramPacket(buffer, buffer.length);
					multicastSocket.receive(message);

					// logger.log("Received Message = " + new String(message.getData()));
					// Get message string
					JSONObject jsonMessage = (JSONObject) jsonParser.parse(new String(message.getData()).trim());

					// Immediately send "SeqNum:ACK" after receiving a message
					int seqNum =  Integer.parseInt( (String) jsonMessage.get("sequenceNumber"));
					sendACK(seqNum);

					// Add message to delivery queue
					mutex.acquire();
					deliveryQueue.add(jsonMessage);
					mutex.release();

					// Signal to Process Messages Thread to start processing a message from the queue
					deliveryQueueMutex.release();
				}
			} catch (InterruptedException | IOException e) {
				logger.log("ListenForPacketsThread is shutting down");
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		private void sendACK(Integer num) throws IOException {
			JSONObject jsonAck = new JSONObject();
			jsonAck.put("sequenceNumber", num);
			jsonAck.put("commandType", "ACK");
			byte[] ack = jsonAck.toString().getBytes();
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), 8000);
			socket.send(packet);
		}
	}

	private class ProcessMessagesThread extends Thread {

		@Override
		public void run() {
			logger.log("CenterServerController: Processing messages\n");
			try {
				while (true) {

					// Sleep a bit so that the message can be added to the queue
					Thread.sleep(300);
					deliveryQueueMutex.acquire();
					int seqNum;
					int nextSequenceNumber = lastSequenceNumber + 1;

					while ((seqNum = Integer.parseInt( (String) deliveryQueue.peek().get(MessageKeys.SEQUENCE_NUMBER))) < nextSequenceNumber)
					{
						deliveryQueueMutex.acquire();

						logger.log("\n*** Removing duplicate [" + seqNum + "] ***\n");

						mutex.acquire();
						deliveryQueue.remove(deliveryQueue.peek());
						mutex.release();
					}
					lastSequenceNumber = seqNum;
					sendMessageToServer(deliveryQueue.peek());
				}
			} catch (InterruptedException e) {
				logger.log("ProcessMessageThread is shutting down.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void sendMessageToServer(JSONObject message){

			if (message == null) {
				logger.log("Cannot send null to servers");
				return;
			}

			String location = ((String) message.get(MessageKeys.MANAGER_ID)).substring(0,2);
			int port = CenterServer.UDPPortMap.get(location);

			try {
				// Remove msg from delivery queue
				mutex.acquire();
				deliveryQueue.remove(message);
				mutex.release();

				// Setup Server Socket
				InetAddress address = InetAddress.getLocalHost();
				DatagramSocket serverSocket = new DatagramSocket();
				byte[] buffer = message.toString().getBytes();
				logger.log("CenterServerController msg to server = " + message.toString());
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

	}

	public CenterServerController() {
		this.logger = new Logger("CenterServerController");

		centerServerCA = new Thread(new CenterServer("CA"));
		centerServerUS = new Thread(new CenterServer("US"));
		centerServerUK = new Thread(new CenterServer("UK"));

		// Instantiate Semaphores
		mutex = new Semaphore(1);
		deliveryQueueMutex = new Semaphore(0);

		// Instantiate Delivery Queue with the MessageComparator
		MessageComparator msgComp = new MessageComparator();
		deliveryQueue = new PriorityQueue<>(msgComp);
	}

	@Override
	public void runServers() {
		centerServerCA.start();
		centerServerUS.start();
		centerServerUK.start();

		try {
			if (centerServerCA.isAlive() && centerServerUS.isAlive() && centerServerUK.isAlive()) {
				setupMulticastSocket();
				listenForPackets = new ListenForPacketsThread();
				processMessages = new ProcessMessagesThread();
				listenForPackets.start();
				processMessages.start();
			}
		} catch (SocketException e) {
			this.logger.log("CenterServerController Multicast Socket is closed.");
		} catch (InterruptedException e ) {
			this.logger.log("CenterServerController is shutting down.");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void setupMulticastSocket() throws Exception {
		InetAddress group = InetAddress.getByName("228.5.6.7");
		multicastSocket = new MulticastSocket(6789);
		multicastSocket.joinGroup(group);
	}

	public PriorityQueue<JSONObject> getDeliveryQueue() {
		return deliveryQueue;
	}

	@Override
	public void shutdownServers() {
		this.logger.log("\nShutting down servers...\n");

		centerServerCA.interrupt();
		centerServerUS.interrupt();
		centerServerUK.interrupt();

		listenForPackets.interrupt();
		processMessages.interrupt();

		if (multicastSocket != null) {
			multicastSocket.close();
		}
	}

	@Override
	public JSONArray getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setData(JSONArray array) {
		// TODO Auto-generated method stub
	}

}
