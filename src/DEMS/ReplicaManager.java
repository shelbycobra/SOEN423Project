package DEMS;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ReplicaManager {

	private final int replicaNumber;
	private final int replicaManagerPort;

	private final int maxFailedReplicaCount = 3;

	private Thread udpServerThread;

	class UdpServer extends Thread {

		private Map<Integer, Integer> replicaFailureCounts = new HashMap<>();
		private DatagramSocket datagramSocket;
		private JSONParser jsonParser = new JSONParser();

		@Override
		public void run() {

			replicaFailureCounts.put(1, 0);
			replicaFailureCounts.put(2, 0);
			replicaFailureCounts.put(3, 0);

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

				if (jsonObject.get(MessageKeys.COMMAND_TYPE).equals("REPORT_FAILED_REPLICA")) {
					int faildReplicaNumber = Integer.parseInt((String) jsonObject.get(MessageKeys.REPLICA_NUMBER));

					if (!((replicaNumber == 1 && (faildReplicaNumber == 2 || faildReplicaNumber == 3)) ||
							(replicaNumber == 2 && (faildReplicaNumber == 1 || faildReplicaNumber == 3)) ||
							(replicaNumber == 3 && (faildReplicaNumber == 1 || faildReplicaNumber == 2)))) {
						notifyFrontEnd("BAD_REPLICA_NUMBER", faildReplicaNumber);
						continue;
					}

					int newReplicaFailureCount = replicaFailureCounts.get(faildReplicaNumber) + 1;
					replicaFailureCounts.put(faildReplicaNumber, newReplicaFailureCount);

					if (newReplicaFailureCount >= maxFailedReplicaCount) {
						try {
							restartReplica(faildReplicaNumber);
						} catch (IOException e) {
							notifyFrontEnd("FAILED_REPLICA_RESTART_FAILED", faildReplicaNumber);
							e.printStackTrace();
							continue;
						}
						replicaFailureCounts.put(faildReplicaNumber, 0);
						notifyFrontEnd("FAILED_REPLICA_RESTARTED", faildReplicaNumber);
					}
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
			jsonObject.put(MessageKeys.MESSAGE_ID, "RESTART_REPLICA");
			jsonObject.put(MessageKeys.REPLICA_NUMBER, faildReplicaNumber);
			byte[] sendDate = jsonObject.toString().getBytes();

			DatagramPacket packet = new DatagramPacket(sendDate, sendDate.length, group, Config.PortNumbers.SEQ_RE);
			multicastSocket.send(packet);

			multicastSocket.close();
		}

		private void notifyFrontEnd(String messageID, int replicaNumber) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(MessageKeys.MESSAGE_ID, messageID);
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
		log("starting udpServerThread");
		udpServerThread.start();
	}

	public void stop() {
		log("interrupting udpServerThread");
		udpServerThread.interrupt();
	}

	public static void main(String[] args) {
		int replicaNumber = Integer.parseInt(args[0]);
		Replica replica = null;

		if (replicaNumber == 1) {
			replica = new Replicas.Replica1.CenterServer();
		} else if (replicaNumber == 2) {
			replica = new Replicas.Replica2.Server();
		} else if (replicaNumber == 3) {
			replica = new Replicas.Replica3.CenterServerController();
		} else {
			throw new IllegalArgumentException("Invalid replicaNumber: " + replicaNumber);
		}

		log("starting replica: " + replicaNumber);
		replica.runServers();

		ReplicaManager replicaManager = new ReplicaManager(replicaNumber);
		log("starting ReplicaManager for replica: " + replicaNumber);
		replicaManager.start();
	}

	private static void log(String message) {
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date dateTime = Calendar.getInstance().getTime();
		String timeStamp = dateFormat.format(dateTime);
		System.out.println(String.format("[ReplicaManager %s] %s", timeStamp, message));
	}

}
