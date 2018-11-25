package Test;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import DEMS.Config;
import DEMS.MessageKeys;

public class ReplicaTest {

	static MulticastSocket multicastSocket;
	static InetAddress group;
	static JSONParser jsonParser = new JSONParser();

	static class UdpServer implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					byte[] receiveData = new byte[1024];
					DatagramPacket datagramPacket = new DatagramPacket(receiveData, receiveData.length);
					multicastSocket.receive(datagramPacket);

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

	static String managerID;
	static int sequenceNumber = 1;
	static int messageID = 1;

	public static void main(String args[]) throws Exception {
		managerID = args[0];

		group = InetAddress.getByName("228.5.6.7");
		multicastSocket = new MulticastSocket(Config.PortNumbers.SEQ_RE);
		multicastSocket.joinGroup(group);

		Thread udpServerThread = new Thread(new UdpServer());
		udpServerThread.start();

		createEmployeeRecord();
		createEmployeeRecord();
		createManagerRecord();
		createManagerRecord();
		getRecordCount();
		editRecord();
		transferRecord();
		getRecordCount();

		// multicastSocket.close();
		// udpServerThread.interrupt();
	}

	public static JSONObject baseObject() {
		JSONObject message = new JSONObject();
		message.put(MessageKeys.SEQUENCE_NUMBER, Integer.toString(sequenceNumber++));
		message.put(MessageKeys.MESSAGE_ID, Integer.toString(messageID++));
		message.put(MessageKeys.MANAGER_ID, managerID);
		return message;
	}

	public static void getRecordCount() throws Exception {
		JSONObject message = baseObject();
		message.put(MessageKeys.COMMAND_TYPE, Config.GET_RECORD_COUNT);
		sendMessage(message);
	}

	public static void createEmployeeRecord() throws Exception {
		JSONObject message = baseObject();
		message.put(MessageKeys.COMMAND_TYPE, Config.CREATE_EMPLOYEE_RECORD);
		message.put(MessageKeys.FIRST_NAME, "firstName");
		message.put(MessageKeys.LAST_NAME, "lastName");
		message.put(MessageKeys.EMPLOYEE_ID, "123");
		message.put(MessageKeys.MAIL_ID, "mailID");
		message.put(MessageKeys.PROJECT_ID, "projectID");
		sendMessage(message);
	}

	public static void createManagerRecord() throws Exception {
		JSONObject message = baseObject();
		message.put(MessageKeys.COMMAND_TYPE, Config.CREATE_MANAGER_RECORD);
		message.put(MessageKeys.FIRST_NAME, "firstName");
		message.put(MessageKeys.LAST_NAME, "lastName");
		message.put(MessageKeys.EMPLOYEE_ID, "123");
		message.put(MessageKeys.MAIL_ID, "mailID");
		message.put(MessageKeys.LOCATION, "location");
		JSONObject project = new JSONObject();
		project.put(MessageKeys.PROJECT_ID, "456");
		project.put(MessageKeys.PROJECT_CLIENT, "clientName");
		project.put(MessageKeys.PROJECT_NAME, "projectName");
		JSONArray projects = new JSONArray();
		projects.add(project);
		message.put(MessageKeys.PROJECTS, projects);
		sendMessage(message);
	}

	public static void editRecord() throws Exception {
		JSONObject message = baseObject();
		message.put(MessageKeys.COMMAND_TYPE, Config.EDIT_RECORD);
		message.put(MessageKeys.RECORD_ID, "ER00000");
		message.put(MessageKeys.FIELD_NAME, MessageKeys.FIRST_NAME);
		message.put(MessageKeys.NEW_VALUE, "newFirstName");
		sendMessage(message);
	}

	public static void transferRecord() throws Exception {
		JSONObject message = baseObject();
		message.put(MessageKeys.COMMAND_TYPE, Config.TRANSFER_RECORD);
		message.put(MessageKeys.RECORD_ID, "ER00000");
		message.put(MessageKeys.REMOTE_SERVER_NAME, "UK");
		sendMessage(message);
	}

	public static void sendMessage(JSONObject message) throws Exception {
		byte[] sendData = new byte[1024];
		sendData = message.toString().getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, group, Config.PortNumbers.SEQ_RE);
		multicastSocket.send(sendPacket);
	}

}
