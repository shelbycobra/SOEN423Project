import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import DEMS.Config;
import DEMS.MessageKeys;

public class ReplicaManagerTest {

	static DatagramSocket datagramSocket;
	static JSONParser jsonParser = new JSONParser();

	static int sequenceNumber = 1;
	static int messageID = 1;

	static class UdpServer implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					byte[] receiveData = new byte[1024];
					DatagramPacket datagramPacket = new DatagramPacket(receiveData, receiveData.length);
					datagramSocket.receive(datagramPacket);

					String resultString = new String(datagramPacket.getData()).trim();
					System.out.println(resultString);
					JSONObject result = (JSONObject) jsonParser.parse(resultString);

					System.out.println(result.toJSONString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		int replicaNumber = Integer.parseInt(args[0]);
		
		InetAddress replicaManagerHost = null;
		int replicaManagerPort = 0;
		
		if (replicaNumber == 1) {
			replicaManagerHost = InetAddress.getByName(Config.IPAddresses.REPLICA1);
			replicaManagerPort = Config.Replica1.RM_PORT;
		} else if (replicaNumber == 2) {
			replicaManagerHost = InetAddress.getByName(Config.IPAddresses.REPLICA2);			
			replicaManagerPort = Config.Replica2.RM_PORT;
		} else if (replicaNumber == 3) {
			replicaManagerHost = InetAddress.getByName(Config.IPAddresses.REPLICA3);
			replicaManagerPort = Config.Replica3.RM_PORT;
		}

		datagramSocket = new DatagramSocket(Config.PortNumbers.SEQ_RE);
		Thread udpServerThread = new Thread(new UdpServer());
		udpServerThread.start();

		JSONObject message = baseObject();
		message.put(MessageKeys.COMMAND_TYPE, Config.GET_DATA);

		byte[] sendData = new byte[1024];
		sendData = message.toString().getBytes();
		DatagramPacket datagramPacket = new DatagramPacket(sendData, sendData.length, replicaManagerHost, replicaManagerPort);
		datagramSocket.send(datagramPacket);		
	}

	public static JSONObject baseObject() {
		JSONObject message = new JSONObject();
		message.put(MessageKeys.SEQUENCE_NUMBER, Integer.toString(sequenceNumber++));
		message.put(MessageKeys.MESSAGE_ID, Integer.toString(messageID++));
		return message;
	}


}
