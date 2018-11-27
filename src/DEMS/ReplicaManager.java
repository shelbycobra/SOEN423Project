package DEMS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ReplicaManager {

	private Replica replica;

	private final int replicaNumber;
	private final int replicaManagerPort;

	private final int maxCrashCount = 1;
	private final int maxByzantineCount = 3;

	private Thread udpServerThread;
	private Logger logger;

	private InetAddress thisReplicaHost = null;
	private int thisReplicaPort = 0;
	private InetAddress otherReplicaHost1 = null;
	private int otherReplicaPort1 = 0;
	private InetAddress otherReplicaHost2 = null;
	private int otherReplicaPort2 = 0;

	AtomicBoolean runThreadsAtomicBoolean = new AtomicBoolean(true);

	class UdpServer extends Thread {

		private int replicaCrashCount = 0;
		private int replicaByzantineCount = 0;

		private DatagramSocket datagramSocket;
		private JSONParser jsonParser = new JSONParser();

		@Override
		public void run() {

			try {
				datagramSocket = new DatagramSocket(replicaManagerPort);
				datagramSocket.setSoTimeout(1000);
				logger.log("listening on port: " + replicaManagerPort);
			} catch (SocketException e) {
				e.printStackTrace();
				return;
			}

			while (true) {
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

				JSONObject jsonObject;
				
				try {
					logger.log("waiting for request");

					while (true) {
						try {
							datagramSocket.receive(receivePacket);
							break;
						} catch(SocketTimeoutException e) {
							if (runThreadsAtomicBoolean.get()) {
								continue;
							} else {
								logger.log("exiting UdpServer");
								datagramSocket.close();
								return;
							}
						}
					}

					jsonObject = processRequest(receivePacket);
					logger.log("received object: " + jsonObject.toJSONString());
				} catch (IOException | ParseException e) {
					e.printStackTrace();
					continue;
				}

				String commandType = jsonObject.get(MessageKeys.COMMAND_TYPE).toString();
				
				int replicaPortNumber = 0;
				try {
					replicaPortNumber = Integer.parseInt(jsonObject.get(MessageKeys.RM_PORT_NUMBER).toString());
				} catch (NullPointerException e) {
					
				}

				jsonObject.put(MessageKeys.COMMAND_TYPE, Config.ACK);

				if (commandType.equals(Config.GET_DATA)) {
					logger.log("processing get_data request");
					try {
						JSONArray jsonArray = replicaGetData();
						logger.log("got data from local replica");
						jsonObject.put(MessageKeys.MESSAGE, jsonArray);
						jsonObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());

						InetAddress IPAddress = receivePacket.getAddress();
						int port = receivePacket.getPort();
						byte[] sendDate = jsonObject.toString().getBytes();
						DatagramPacket datagramPacket = new DatagramPacket(sendDate, sendDate.length, IPAddress, port);
						try {
							datagramSocket.send(datagramPacket);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (Exception e) {
						e.printStackTrace();
						logger.log("unable to get data from local replica");
						jsonObject.put(MessageKeys.MESSAGE, "unable to get data for this replica");
						jsonObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.FAIL.toString());
					}
					continue;
				} else if (replicaPortNumber == replicaManagerPort && commandType.equals(Config.REPORT_FAILURE)) {
					logger.log("processing report_failure request");
					String failureType = jsonObject.get(MessageKeys.FAILURE_TYPE).toString();
					if (failureType.equals(Config.Failure.PROCESS_CRASH.toString())) {
						replicaCrashCount += 1;
						logger.log("incrementing replicaCrashCount to: " + replicaCrashCount);
					} else if (failureType.equals(Config.Failure.BYZANTINE.toString())) {
						replicaByzantineCount += 1;
						logger.log("incrementing replicaByzantineCount to: " + replicaByzantineCount);
					}

					if (replicaCrashCount >= maxCrashCount || replicaByzantineCount >= maxByzantineCount) {
						try {
							logger.log("trying to restart local replica...");
							restartReplica();
							logger.log("restarting local replica successful");
						} catch (Exception e) {
							e.printStackTrace();
							logger.log("restarting local replica failed");
							jsonObject.put(MessageKeys.MESSAGE, Config.FAILED_REPLICA_RESTART_FAILED);
							jsonObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.FAIL.toString());
							continue;
						}
						replicaCrashCount = 0;
						replicaByzantineCount = 0;
						jsonObject.put(MessageKeys.MESSAGE, Config.REPLICA_RESTARTED);
						jsonObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
					} else {
						jsonObject.put(MessageKeys.MESSAGE, Config.FAILURE_COUNTS_INCREMENTED);
						jsonObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
					}
				}

				notifyFrontEnd(jsonObject);
			}

		}

		private JSONArray replicaGetData() throws Exception {
			logger.log("getting data from local replica");
			DatagramSocket datagramSocket = new DatagramSocket();

			JSONObject jsonSendObject = new JSONObject();
			jsonSendObject.put(MessageKeys.COMMAND_TYPE, Config.GET_DATA);
			byte[] sendDate = jsonSendObject.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendDate, sendDate.length, thisReplicaHost, thisReplicaPort);
			datagramSocket.send(sendPacket);

			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			datagramSocket.receive(receivePacket);
			JSONArray jsonArray = (JSONArray) jsonParser.parse(new String(receivePacket.getData()).trim());
			logger.log("got data from local replica: " + jsonArray.toJSONString());

			return jsonArray;
		}

		private void replicaSetData(JSONArray jsonArray) throws IOException {
			logger.log("setting data in local replica: " + jsonArray.toJSONString());
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(MessageKeys.COMMAND_TYPE, Config.SET_DATA);
			jsonObject.put(MessageKeys.MESSAGE, jsonArray);
			byte[] sendDate = jsonArray.toString().getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(sendDate, sendDate.length, thisReplicaHost, thisReplicaPort);
			datagramSocket.send(datagramPacket);
		}

		private JSONObject processRequest(DatagramPacket receivePacket) throws ParseException {
			JSONObject jsonObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());

			// send echo to sending client
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
			byte[] sendDate = jsonObject.toString().getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(sendDate, sendDate.length, IPAddress, port);

			try {
				datagramSocket.send(datagramPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}

			return jsonObject;
		}

		private void restartReplica() throws Exception {
			logger.log("restarting local replica");
			replica.shutdownServers();
			replica.runServers(0);

			JSONObject jsonSendObject = new JSONObject();
			jsonSendObject.put(MessageKeys.COMMAND_TYPE, Config.GET_DATA);
			byte[] sendDate = jsonSendObject.toString().getBytes();

			DatagramSocket datagramSocket = new DatagramSocket();
			DatagramPacket datagramPacket = new DatagramPacket(sendDate, sendDate.length, otherReplicaHost1, otherReplicaPort1);
			logger.log("getting data from otherReplica1: " + jsonSendObject.toJSONString());
			// datagramSocket.setSoTimeout(2000);
			datagramSocket.send(datagramPacket);
			
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			datagramSocket.receive(receivePacket); // ignore echo
			datagramSocket.receive(receivePacket);
			String receiveStringData = new String(receivePacket.getData()).trim();
			JSONObject jsonReceiveObject = (JSONObject) jsonParser.parse(receiveStringData);
			logger.log("got jsonReceiveObject otherReplica1: " + jsonReceiveObject.toJSONString());
			JSONArray jsonReceiveData = (JSONArray) jsonReceiveObject.get(MessageKeys.MESSAGE);

			replicaSetData(jsonReceiveData);
		}

		private void notifyFrontEnd(JSONObject jsonObject) {
			logger.log("notifying frontend: " + jsonObject.toJSONString());
			try {
				InetAddress frontEndHost = InetAddress.getByName(Config.IPAddresses.FRONT_END);
				int frontEndPort = Config.PortNumbers.RE_FE;
				byte[] sendData = jsonObject.toString().getBytes();
				DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, frontEndHost, frontEndPort);
				datagramSocket.send(datagramPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public ReplicaManager(Replica replica, int replicaNumber) throws UnknownHostException {
		this.replica = replica;
		this.replicaNumber = replicaNumber;
		this.logger = new Logger(replicaNumber);

		if (replicaNumber == 1) {
			this.replicaManagerPort = DEMS.Config.Replica1.RM_PORT;
			this.thisReplicaHost = InetAddress.getByName(Config.IPAddresses.REPLICA1);
			this.thisReplicaPort = Config.Replica1.RE_PORT;
			this.otherReplicaHost1 = InetAddress.getByName(Config.IPAddresses.REPLICA2);
			this.otherReplicaPort1 = Config.Replica2.RM_PORT;
			this.otherReplicaHost2 = InetAddress.getByName(Config.IPAddresses.REPLICA3);
			this.otherReplicaPort2 = Config.Replica3.RM_PORT;
		} else if (replicaNumber == 2) {
			this.replicaManagerPort = DEMS.Config.Replica2.RM_PORT;
			this.thisReplicaHost = InetAddress.getByName(Config.IPAddresses.REPLICA2);
			this.thisReplicaPort = Config.Replica2.RE_PORT;
			this.otherReplicaHost1 = InetAddress.getByName(Config.IPAddresses.REPLICA1);
			this.otherReplicaPort1 = Config.Replica1.RM_PORT;
			this.otherReplicaHost2 = InetAddress.getByName(Config.IPAddresses.REPLICA3);
			this.otherReplicaPort2 = Config.Replica3.RM_PORT;
		} else if (replicaNumber == 3) {
			this.replicaManagerPort = DEMS.Config.Replica3.RM_PORT;
			this.thisReplicaHost = InetAddress.getByName(Config.IPAddresses.REPLICA3);
			this.thisReplicaPort = Config.Replica3.RE_PORT;
			this.otherReplicaHost1 = InetAddress.getByName(Config.IPAddresses.REPLICA1);
			this.otherReplicaPort1 = Config.Replica1.RM_PORT;
			this.otherReplicaHost2 = InetAddress.getByName(Config.IPAddresses.REPLICA2);
			this.otherReplicaPort2 = Config.Replica2.RM_PORT;
		} else {
			throw new IllegalArgumentException("Invalid replicaNumber: " + replicaNumber);
		}

		udpServerThread = new Thread(new UdpServer());
	}

	public void start() {
		this.logger.log("starting udpServerThread");
		udpServerThread.start();
	}

	public void stop() {
		this.logger.log("interrupting udpServerThread");
		runThreadsAtomicBoolean.set(false);
		try {
			udpServerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.logger.close();
	}

	public static void main(String[] args) throws Exception {
		int replicaNumber = Integer.parseInt(args[0]);
		int errorType =  Integer.parseInt(args[1]);
		Class replicaClass = null;

		if (replicaNumber == 1) {
			replicaClass = Replicas.Replica1.CenterServer.class;
		} else if (replicaNumber == 2) {
			replicaClass = Replicas.Replica2.Server.class;
		} else if (replicaNumber == 3) {
			replicaClass = Replicas.Replica3.CenterServerController.class;
		} else {
			throw new IllegalArgumentException("Invalid replicaNumber: " + replicaNumber);
		}

		Replica replica = (Replica) replicaClass.newInstance();
		ReplicaManager replicaManager = new ReplicaManager(replica, replicaNumber);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				replicaManager.logger.log("running shutdown hook");
				replica.shutdownServers();
				replicaManager.stop();
			}
		});

		replicaManager.logger.log("starting replica: " + replicaNumber);
		replica.runServers(errorType);

		replicaManager.logger.log("starting ReplicaManager for replica: " + replicaNumber);
		replicaManager.start();
	}

}

class Logger {

	public static Logger logger;

	private PrintWriter logFile;
	private String description;

	public Logger(int replicaNumber) {
		this.description = "ReplicaManager" + replicaNumber;
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
		String logFileName = String.format("Logs/Replica%d/%s-%s.log", replicaNumber, description, timeStamp);
		try {
			logFile = new PrintWriter(logFileName, "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void log(String message) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
		String logMessage = String.format("%s | %s | %s", description, timeStamp, message);
		logFile.println(logMessage);
		System.out.println(logMessage);
	}

	public void close() {
		logFile.close();
	}
}
