package Replicas.Replica3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import DEMS.Config;
import DEMS.MessageKeys;

public class CenterServerController implements DEMS.Replica {

	private Logger logger;

	private CenterServer centerServerCA;
	private CenterServer centerServerUS;
	private CenterServer centerServerUK;

	private MulticastSocket multicastSocket;

	private Thread listenForPackets;
	private Thread processMessages;

	private int lastSequenceNumber = 0;
	private PriorityQueue<JSONObject> deliveryQueue;
	private Semaphore mutex;
	private Semaphore deliveryQueueMutex;
	private JSONParser jsonParser = new JSONParser();

	private int numMessages = 0;
	private Config.Failure errorType = Config.Failure.NONE;

	private class ListenForPacketsThread extends Thread {

		@Override
		public void run() {
			try {
				while (true) {
					byte[] buffer = new byte[1000];
					DatagramPacket message = new DatagramPacket(buffer, buffer.length);
					logger.log("listening on port: " + Config.PortNumbers.SEQ_RE);
					multicastSocket.receive(message);

					// logger.log("Received Message = " + new String(message.getData()));
					// Get message string
					JSONObject jsonMessage = (JSONObject) jsonParser.parse(new String(message.getData()).trim());

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
				e.printStackTrace();
				logger.log("ListenForPacketsThread is shutting down");
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		private void sendACK(Integer num) throws IOException {
			JSONObject jsonAck = new JSONObject();
			jsonAck.put(MessageKeys.SEQUENCE_NUMBER, num);
			jsonAck.put(MessageKeys.COMMAND_TYPE, Config.ACK);
			byte[] ack = jsonAck.toString().getBytes();
			DatagramSocket socket = new DatagramSocket(12000);
			DatagramPacket packet = new DatagramPacket(ack, ack.length, InetAddress.getByName(Config.IPAddresses.SEQUENCER), Config.PortNumbers.FE_SEQ);
			socket.send(packet);
			socket.close();
		}
	}

	private class ProcessMessagesThread extends Thread {

		@Override
		public void run() {
			logger.log("CenterServerController: Processing messages\n");
			try {
				while (true) {
					// Checks if failure should start
					checkToStartFailure();

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
			int port = 0;
			if (location.equals("CA")) {
				port = DEMS.Config.Replica3.CA_PORT;
			} else if (location.equals("UK")) {
				port = DEMS.Config.Replica3.UK_PORT;
			} else if (location.equals("US")) {
				port = DEMS.Config.Replica3.US_PORT;
			}

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

		centerServerCA = new CenterServer("CA");
		centerServerUS = new CenterServer("US");
		centerServerUK = new CenterServer("UK");

		// Instantiate Semaphores
		mutex = new Semaphore(1);
		deliveryQueueMutex = new Semaphore(0);

		// Instantiate Delivery Queue with the MessageComparator
		MessageComparator msgComp = new MessageComparator();
		deliveryQueue = new PriorityQueue<>(msgComp);
	}

	void checkToStartFailure() throws IOException, InterruptedException {
		if (numMessages >= Config.MESSAGE_DELAY) {
			switch(this.errorType) {
				case BYZANTINE:
					byzantineFailure();
					numMessages = 0;
					break;
				case PROCESS_CRASH:
					processCrashFailure();
					numMessages = 0;
					break;
				default:
					break;
			}
		}
	}

	void byzantineFailure() throws IOException, InterruptedException {
		while (true) {
			JSONObject obj = new JSONObject();
			obj.put(MessageKeys.FAILURE_TYPE, Config.StatusCode.FAIL);
			byte[] buffer = obj.toString().getBytes();

			DatagramSocket socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Config.IPAddresses.FRONT_END), Config.PortNumbers.RE_FE);

			socket.send(packet);
			Thread.sleep(200);
		}
	}

	void processCrashFailure() {
		while (true);
	}

	@Override
	public void runServers(int errorType) {
		if (errorType == Config.Failure.NONE.ordinal()) {
			// no error
		} else if (errorType == Config.Failure.BYZANTINE.ordinal()) {
			this.errorType = Config.Failure.BYZANTINE;
		} else if (errorType == Config.Failure.PROCESS_CRASH.ordinal()) {
			this.errorType = Config.Failure.PROCESS_CRASH;
		}

		try {
			setupMulticastSocket();

			centerServerCA.start();
			centerServerUS.start();
			centerServerUK.start();

			listenForPackets = new ListenForPacketsThread();
			processMessages = new ProcessMessagesThread();
			listenForPackets.start();
			processMessages.start();

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
		multicastSocket = new MulticastSocket(Config.PortNumbers.SEQ_RE);
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
		JSONArray jsonArray = new JSONArray();

		List<CenterServer> centerServers = Arrays.asList(centerServerCA, centerServerUK, centerServerUS);

		for (CenterServer centerServer : centerServers) {
			String serverLocation = centerServer.getLocation();
			jsonArray.add(centerServer.getRecords().getJSONArray(serverLocation));
		}

		return jsonArray;
	}

	@Override
	public void setData(JSONArray jsonArray) {
		Records recordsCA = new Records();
		Records recordsUK = new Records();
		Records recordsUS = new Records();

		for (int i = 0; i < jsonArray.size(); i++){
			JSONObject jsonObject = (JSONObject) jsonArray.get(i);
			String location = (String) jsonObject.get(MessageKeys.SERVER_LOCATION);
			if (location.toLowerCase().equals("ca")) {
				recordsCA.addRecord(jsonObject);
			} else if (location.toLowerCase().equals("uk")) {
				recordsUK.addRecord(jsonObject);
			} else if (location.toLowerCase().equals("us")) {
				recordsUS.addRecord(jsonObject);
			}
		}

		centerServerCA.setRecords(recordsCA);
		centerServerUK.setRecords(recordsUK);
		centerServerUS.setRecords(recordsUS);
	}

}
