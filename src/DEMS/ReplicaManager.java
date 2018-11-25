package DEMS;

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
import java.util.HashMap;
import java.util.Map;

public class ReplicaManager {

	private final int replicaNumber;
	private final int replicaManagerPort;

	private final int maxCrashCount = 1;
	private final int maxByzantineCount = 3;

	private Thread udpServerThread;

	private Logger logger;

	class UdpServer extends Thread {

		private Map<Integer, Integer> replicaCrashCounts = new HashMap<>();
		private Map<Integer, Integer> replicaByzantineCounts = new HashMap<>();

		private DatagramSocket datagramSocket;
		private JSONParser jsonParser = new JSONParser();

		@Override
		public void run() {

			replicaCrashCounts.put(1, 0);
			replicaCrashCounts.put(2, 0);
			replicaCrashCounts.put(3, 0);

			replicaByzantineCounts.put(1, 0);
			replicaByzantineCounts.put(2, 0);
			replicaByzantineCounts.put(3, 0);

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

				int faildReplicaNumber = Integer.parseInt((String) jsonObject.get(MessageKeys.REPLICA_NUMBER));
				if (!((replicaNumber == 1 && (faildReplicaNumber == 2 || faildReplicaNumber == 3)) ||
						(replicaNumber == 2 && (faildReplicaNumber == 1 || faildReplicaNumber == 3)) ||
						(replicaNumber == 3 && (faildReplicaNumber == 1 || faildReplicaNumber == 2)))) {
					notifyFrontEnd(Config.BAD_REPLICA_NUMBER, faildReplicaNumber);
					continue;
				}

				int replicaCrashCount = replicaCrashCounts.get(faildReplicaNumber);
				int replicaByzantineCount = replicaByzantineCounts.get(faildReplicaNumber);

				if ((Config.Failure) jsonObject.get(MessageKeys.FAILURE_TYPE) == Config.Failure.PROCESS_CRASH) {
					replicaCrashCount += 1;
					replicaCrashCounts.put(faildReplicaNumber, replicaCrashCount);
				} else if ((Config.Failure) jsonObject.get(MessageKeys.FAILURE_TYPE) == Config.Failure.BYZANTINE) {
					replicaByzantineCount += 1;
					replicaByzantineCounts.put(faildReplicaNumber, replicaByzantineCount);
				}

				if (replicaCrashCount >= maxCrashCount || replicaByzantineCount >= maxByzantineCount) {
					try {
						restartReplica(faildReplicaNumber);
					} catch (IOException e) {
						notifyFrontEnd(Config.FAILED_REPLICA_RESTART_FAILED, faildReplicaNumber);
						e.printStackTrace();
						continue;
					}
					replicaCrashCounts.put(faildReplicaNumber, 0);
					replicaByzantineCounts.put(faildReplicaNumber, 0);
					notifyFrontEnd(Config.FAILED_REPLICA_RESTARTED, faildReplicaNumber);
				}

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

		private void restartReplica(int faildReplicaNumber) throws IOException {
			InetAddress group = InetAddress.getByName("228.5.6.7");
			MulticastSocket multicastSocket = new MulticastSocket(Config.PortNumbers.SEQ_RE);
			multicastSocket.joinGroup(group);

			JSONObject jsonObject = new JSONObject();
			jsonObject.put(MessageKeys.COMMAND_TYPE, Config.RESTART_REPLICA);
			jsonObject.put(MessageKeys.REPLICA_NUMBER, faildReplicaNumber);
			byte[] sendDate = jsonObject.toString().getBytes();

			DatagramPacket packet = new DatagramPacket(sendDate, sendDate.length, group, Config.PortNumbers.SEQ_RE);
			multicastSocket.send(packet);

			multicastSocket.close();
		}

		private void notifyFrontEnd(String commandType, int replicaNumber) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(MessageKeys.COMMAND_TYPE, commandType);
			jsonObject.put(MessageKeys.REPLICA_NUMBER, replicaNumber);
			byte[] sendDate = jsonObject.toString().getBytes();

			try {
				InetAddress IPAddress = InetAddress.getByName(Config.FRONT_END_HOST);
				byte[] sendData = jsonObject.toString().getBytes();
				DatagramPacket datagramPacket = new DatagramPacket(sendDate, sendDate.length, IPAddress, Config.PortNumbers.SEQ_RE);
				datagramSocket.send(datagramPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public ReplicaManager(int replicaNumber) {
		this.replicaNumber = replicaNumber;
		this.logger = new Logger(replicaNumber);

		if (replicaNumber == 1) {
			this.replicaManagerPort = DEMS.Config.Replica1.RM_PORT;
		} else if (replicaNumber == 2) {
			this.replicaManagerPort = DEMS.Config.Replica2.RM_PORT;
		} else if (replicaNumber == 3) {
			this.replicaManagerPort = DEMS.Config.Replica3.RM_PORT;
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

	public static void main(String[] args) throws InstantiationException, IllegalAccessException {
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
		ReplicaManager replicaManager = new ReplicaManager(replicaNumber);

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
