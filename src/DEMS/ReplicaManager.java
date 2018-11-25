package DEMS;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReplicaManager {

	private Replica replica;

	private final int replicaNumber;
	private final int replicaManagerPort;

	private final int maxCrashCount = 1;
	private final int maxByzantineCount = 3;

	private Thread udpServerThread;
	private Logger logger;

	private InetAddress otherReplicaHost1 = null;
	private int otherReplicaPort1 = 0;
	private InetAddress otherReplicaHost2 = null;
	private int otherReplicaPort2 = 0;

	class UdpServer extends Thread {

		private int replicaCrashCount = 0;
		private int replicaByzantineCount = 0;

		private DatagramSocket datagramSocket;
		private JSONParser jsonParser = new JSONParser();

		@Override
		public void run() {

			try {
				datagramSocket = new DatagramSocket(replicaManagerPort);
			} catch (SocketException e) {
				e.printStackTrace();
				return;
			}

			while (true) {
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

				JSONObject jsonObject;

				try {
					datagramSocket.receive(receivePacket);
					jsonObject = processRequest(receivePacket);
				} catch (IOException | ParseException e) {
					e.printStackTrace();
					continue;
				}

				String commandType = jsonObject.get(MessageKeys.COMMAND_TYPE).toString();

				if (commandType.equals(Config.GET_DATA)) {
					jsonObject.put(MessageKeys.MESSAGE, replica.getData());
					jsonObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
				} else if (commandType.equals(Config.REPORT_FAILURE)) {
					if ((Config.Failure) jsonObject.get(MessageKeys.FAILURE_TYPE) == Config.Failure.PROCESS_CRASH) {
						replicaCrashCount += 1;
					} else if ((Config.Failure) jsonObject.get(MessageKeys.FAILURE_TYPE) == Config.Failure.BYZANTINE) {
						replicaByzantineCount += 1;
					}

					if (replicaCrashCount >= maxCrashCount || replicaByzantineCount >= maxByzantineCount) {
						try {
							restartReplica();
						} catch (Exception e) {
							e.printStackTrace();
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
			replica.shutdownServers();
			replica.runServers(0);

			JSONObject jsonSendObject = new JSONObject();
			jsonSendObject.put(MessageKeys.COMMAND_TYPE, Config.GET_DATA);
			byte[] sendDate = jsonSendObject.toString().getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(sendDate, sendDate.length, otherReplicaHost1, otherReplicaPort1);

			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			datagramSocket.receive(receivePacket); // ignore echo packet
			datagramSocket.receive(receivePacket);
			JSONObject jsonReceiveObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());

			JSONArray recordData = (JSONArray) jsonReceiveObject.get(MessageKeys.MESSAGE);
			replica.setData(recordData);
		}

		private void notifyFrontEnd(JSONObject jsonObject) {
			try {
				InetAddress frontEndHost = InetAddress.getByName(Config.FRONT_END_HOST);
				int frontEndPort = Config.PortNumbers.SEQ_RE;
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
			this.otherReplicaHost1 = InetAddress.getByName(Config.IPAddresses.REPLICA2);
			this.otherReplicaPort1 = Config.Replica2.RM_PORT;
			this.otherReplicaHost2 = InetAddress.getByName(Config.IPAddresses.REPLICA3);
			this.otherReplicaPort2 = Config.Replica3.RM_PORT;
		} else if (replicaNumber == 2) {
			this.replicaManagerPort = DEMS.Config.Replica2.RM_PORT;
			this.otherReplicaHost1 = InetAddress.getByName(Config.IPAddresses.REPLICA1);
			this.otherReplicaPort1 = Config.Replica1.RM_PORT;
			this.otherReplicaHost2 = InetAddress.getByName(Config.IPAddresses.REPLICA3);
			this.otherReplicaPort2 = Config.Replica3.RM_PORT;
		} else if (replicaNumber == 3) {
			this.replicaManagerPort = DEMS.Config.Replica3.RM_PORT;
			this.otherReplicaHost2 = InetAddress.getByName(Config.IPAddresses.REPLICA1);
			this.otherReplicaPort2 = Config.Replica1.RM_PORT;
			this.otherReplicaHost1 = InetAddress.getByName(Config.IPAddresses.REPLICA2);
			this.otherReplicaPort1 = Config.Replica2.RM_PORT;
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
		udpServerThread.interrupt();
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
