package Replicas.Replica3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

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

	AtomicBoolean runThreadsAtomicBoolean = new AtomicBoolean(true);
    private CommunicateWithRMThread communicateWithRM;
    private Semaphore proceedWithMessagesMutex; // Used when the RM needs to communicate with the replica

    private JSONParser parser = new JSONParser();

    private class CommunicateWithRMThread extends Thread {

        private DatagramSocket socket;

        @Override
        public void run(){

            try {
				socket = new DatagramSocket(Config.Replica3.RE_PORT);
			} catch (SocketException e1) {
				e1.printStackTrace();
			}

            logger.log("listening for packats from RM on port: " + Config.Replica3.RE_PORT);
            
        	while (runThreadsAtomicBoolean.get()) {
        		try {
                    byte[] buffer = new byte[1024*10];

        			socket.setSoTimeout(1000);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    JSONObject obj = (JSONObject) parser.parse(new String(packet.getData()).trim());
                    logger.log("received message from RM: " + obj.toJSONString());
                    if (obj.get(MessageKeys.COMMAND_TYPE).toString().equals(Config.GET_DATA)) {
                        sendDataToRM(packet.getPort());
                    } else if (obj.get(MessageKeys.COMMAND_TYPE).toString().equals(Config.SET_DATA)) {
                        JSONArray arr = (JSONArray) parser.parse(new String(obj.get(MessageKeys.MESSAGE).toString()).trim());
                        proceedWithMessagesMutex.acquire();
                        logger.log("setting data");
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
        	
            logger.log("communicateWithRMThread is shutting down...");
            socket.close();
        }

        private void sendDataToRM(int port) throws IOException,InterruptedException {
        	logger.log("sending data to RM on port: " + port);
            proceedWithMessagesMutex.acquire();
            JSONArray arr = getData();
            proceedWithMessagesMutex.release();
            byte[] buffer = arr.toString().getBytes();
            socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Config.IPAddresses.REPLICA1), port);
            socket.send(packet);
        }
    }
    
	private class ListenForPacketsThread extends Thread {

		@Override
		public void run() {
			logger.log("started ListenForPacketsThread");
			try {
				while (true) {
					byte[] buffer = new byte[1000];
					DatagramPacket message = new DatagramPacket(buffer, buffer.length);
					logger.log("listening on port: " + Config.PortNumbers.SEQ_RE);

					while (true) {
						try {
							multicastSocket.receive(message);
							break;
						} catch(SocketTimeoutException e) {
							if (runThreadsAtomicBoolean.get()) {
								continue;
							} else {
								throw new Exception("runThreadsAtomicBoolean is false");
							}
						}
					}

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
			} catch (Exception e) {
				e.printStackTrace();
				logger.log("exiting ListenForPacketsThread");
				multicastSocket.close();
				return;
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
			logger.log("started ProcessMessagesThread");
			try {
				while (true) {
					if (!runThreadsAtomicBoolean.get()) {
						throw new Exception("runThreadsAtomicBoolean is false");
					}

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
			} catch (Exception e) {
				e.printStackTrace();
				logger.log("exiting ProcessMessageThread");
				multicastSocket.close();
				return;
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
				
				// Checks if failure should start
				if (checkToStartFailure(message.get(MessageKeys.MESSAGE_ID).toString())) {
					return;
				}

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

		// Instantiate Semaphores
		mutex = new Semaphore(1);
		deliveryQueueMutex = new Semaphore(0);
        proceedWithMessagesMutex = new Semaphore(1);

		// Instantiate Delivery Queue with the MessageComparator
		MessageComparator msgComp = new MessageComparator();
		deliveryQueue = new PriorityQueue<>(msgComp);
	}

	boolean checkToStartFailure(String messageID) throws IOException, InterruptedException {
		if (numMessages >= Config.MESSAGE_DELAY) {
			switch(this.errorType) {
				case BYZANTINE:
					byzantineFailure(messageID);
					return true;
				case PROCESS_CRASH:
					processCrashFailure();
					return true;
				default:
					return false;
			}
		}
		return false;
	}

	void byzantineFailure(String messageID) throws IOException, InterruptedException {
		this.logger.log("entering byzantineFailure");
		JSONObject obj = new JSONObject();
		obj.put(MessageKeys.MESSAGE_ID, messageID);
		obj.put(MessageKeys.MESSAGE, "byzantineFailure");
		obj.put(MessageKeys.STATUS_CODE, Config.StatusCode.FAIL.toString());
		obj.put(MessageKeys.RM_PORT_NUMBER, Integer.toString(Config.Replica3.RM_PORT));
		this.logger.log("sending to frontend: " + obj.toJSONString());
		byte[] buffer = obj.toString().getBytes();

		DatagramSocket socket = new DatagramSocket();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(Config.IPAddresses.FRONT_END), Config.PortNumbers.RE_FE);
		socket.send(packet);
		socket.close();
	}

	void processCrashFailure() {
		this.logger.log("entering processCrashFailure");
		while (runThreadsAtomicBoolean.get());
	}

	@Override
	public void runServers(int errorType) {
		runThreadsAtomicBoolean.set(true);

		if (errorType == Config.Failure.NONE.ordinal()) {
			this.errorType = Config.Failure.NONE;
		} else if (errorType == Config.Failure.BYZANTINE.ordinal()) {
			this.errorType = Config.Failure.BYZANTINE;
		} else if (errorType == Config.Failure.PROCESS_CRASH.ordinal()) {
			this.errorType = Config.Failure.PROCESS_CRASH;
		}

		try {
			setupMulticastSocket();

			centerServerCA = new CenterServer("CA");
			centerServerUS = new CenterServer("US");
			centerServerUK = new CenterServer("UK");

			listenForPackets = new ListenForPacketsThread();
			processMessages = new ProcessMessagesThread();
			communicateWithRM = new CommunicateWithRMThread();
			
			listenForPackets.start();
			processMessages.start();
            communicateWithRM.start();
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
		multicastSocket.setSoTimeout(1000);
		multicastSocket.joinGroup(group);
	}

	public PriorityQueue<JSONObject> getDeliveryQueue() {
		return deliveryQueue;
	}

	@Override
	public void shutdownServers() {
		this.logger.log("shutting down servers...");

		runThreadsAtomicBoolean.set(false);
		listenForPackets.interrupt();
		processMessages.interrupt();

		try {
			listenForPackets.join();
            communicateWithRM.join();
			processMessages.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		centerServerCA.stopServer();
		centerServerUS.stopServer();
		centerServerUK.stopServer();

		this.logger.log("shutdown all components");
	}

	@Override
	public JSONArray getData() {
		JSONArray jsonArray = new JSONArray();

		List<CenterServer> centerServers = Arrays.asList(centerServerCA, centerServerUK, centerServerUS);

		for (CenterServer centerServer : centerServers) {
			String serverLocation = centerServer.getLocation();
			jsonArray.add(centerServer.getRecords().getJSONArray(serverLocation));
		}

		this.logger.log("got data from replica: " + jsonArray.toJSONString());
		return jsonArray;
	}

	@Override
	public void setData(JSONArray jsonArray) {
		this.logger.log("setting data in replica: " + jsonArray.toJSONString());
		
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
