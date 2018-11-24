package Replicas.Replica3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import DEMS.Config;
import DEMS.MessageKeys;

public class CenterServer extends Thread {

	private String location;
	private Thread udpServerThread;
	private Logger logger;
	private JSONParser jsonParser = new JSONParser();

	private Records records = new Records();

	public Records getRecords() {
		return records;
	}

	public void setRecords(Records records) {
		this.records = records;
	}

	public String getLocation() {
		return location;
	}

	class UdpServer implements Runnable {

		@Override
		public void run() {
			try {
				int localPort = 0;
				if (location.equals("CA")) {
					localPort = Config.Replica3.CA_PORT;
				} else if (location.equals("UK")) {
					localPort = Config.Replica3.UK_PORT;
				} else if (location.equals("US")) {
					localPort = Config.Replica3.US_PORT;
				}

				DatagramSocket serverSocket = new DatagramSocket(localPort);
				byte[] receiveData = new byte[1024];
				byte[] sendData = new byte[1024];
				while (true) {
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					logger.log("udp waiting for connection");
					serverSocket.receive(receivePacket);

					JSONObject jsonReceiveObject;
					try {
						jsonReceiveObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());
					} catch (ParseException e) {
						e.printStackTrace();
						continue;
					}

					JSONObject jsonSendObject = new JSONObject();

					int commandType = Integer.parseInt((String) jsonReceiveObject.get(MessageKeys.COMMAND_TYPE));
					logger.log("udp command received: " + commandType);

					if (commandType == Config.GET_RECORD_COUNT) {
						jsonSendObject.put(MessageKeys.RECORD_COUNT, Integer.toString(records.getRecordCount()));
					} else if (commandType == Config.RECORD_EXISTS) {
						String recordID = (String) jsonReceiveObject.get(MessageKeys.RECORD_ID);
						jsonSendObject.put(MessageKeys.RECORD_EXISTS, Boolean.toString(records.recordExists(recordID)));
					} else if (commandType == Config.TRANSFER_RECORD) {
						records.addRecord(jsonReceiveObject);
						jsonSendObject.put(MessageKeys.MESSAGE, "ok");
					}

					logger.log("udp command response: " + jsonSendObject);
					InetAddress IPAddress = receivePacket.getAddress();
					int port = receivePacket.getPort();
					sendData = jsonSendObject.toString().getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					serverSocket.send(sendPacket);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public CenterServer(String location) {
		this.location = location;
		this.logger = new Logger(location);

		udpServerThread = new Thread(new UdpServer());
	}

	public synchronized int createMRecord(String managerID, String firstName, String lastName, int employeeID, String mailID, Projects projects, String location) {
		this.logger.log(String.format("createMRecord(%s, %s, %s, %d, %s, %s, %s)", managerID, firstName, lastName, employeeID, mailID, projects.toString(), location));

		ManagerRecord record = new ManagerRecord(firstName, lastName, employeeID, mailID, projects, location);
		records.addRecord(record);

		return 0;
	}

	public synchronized int createERecord(String managerID, String firstName, String lastName, int employeeID, String mailID, int projectID) {
		this.logger.log(String.format("createERecord(%s, %s, %s, %d, %s, %d)", managerID, firstName, lastName, employeeID, mailID, projectID));

		EmployeeRecord record = new EmployeeRecord(firstName, lastName, employeeID, mailID, projectID);
		records.addRecord(record);

		return 0;
	}

	public synchronized String getRecordCounts(String managerID) {
		this.logger.log(String.format("getRecordCounts(%s)", managerID));

		String result = "";
		for (String location : new String[]{"CA", "UK", "US"}) {
			int port = 0;
			if (location.equals("CA")) {
				port = Config.Replica3.CA_PORT;
			} else if (location.equals("UK")) {
				port = Config.Replica3.UK_PORT;
			} else if (location.equals("US")) {
				port = Config.Replica3.US_PORT;
			}

			String response;

			try {
				DatagramSocket clientSocket = new DatagramSocket();
				InetAddress IPAddress = InetAddress.getByName("localhost");
				byte[] sendData = new byte[1024];
				byte[] receiveData = new byte[1024];

				JSONObject jsonSendObject = new JSONObject();
				jsonSendObject.put(MessageKeys.COMMAND_TYPE, Config.GET_RECORD_COUNT);
				sendData = jsonSendObject.toString().getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				clientSocket.send(sendPacket);
				clientSocket.setSoTimeout(2000);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				clientSocket.receive(receivePacket);
				JSONObject jsonReceiveObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());
				response = (String) jsonReceiveObject.get(MessageKeys.RECORD_COUNT);
				this.logger.log(String.format("record count from %s: %s", location, response));
				clientSocket.close();
			} catch (ParseException e) {
				e.printStackTrace();
				response = "ParseException";
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				response = "SocketTimeoutException";
			} catch (IOException e) {
				e.printStackTrace();
				response = "IOException";
			}

			result += String.format("%s: %s, ", location, response);
		}

		// remove comma at end of string
		result = result.substring(0, result.length() - 2);

		return result;
	}

	public synchronized int editRecord(String managerID, String recordID, String fieldName, String newValue) {
		this.logger.log(String.format("editRecord(%s, %s, %s, %s)", managerID, recordID, fieldName, newValue));

		try {
			records.editRecord(recordID, fieldName, newValue);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return -2;
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
			return -1;
		}

		return 0;
	}

	public int printData(String managerID) {
		this.logger.log(String.format("printData(%s)", managerID));

		records.printRecords(this.logger);

		return 0;
	}

	public synchronized int transferRecord(String managerID, String recordID, String remoteCenterServerName) {
		this.logger.log(String.format("transferRecord(%s, %s, %s)", managerID, recordID, remoteCenterServerName));

		int remoteCenterServerPort = 0;
		if (remoteCenterServerName.equals("CA")) {
			remoteCenterServerPort = Config.Replica3.CA_PORT;
		} else if (remoteCenterServerName.equals("UK")) {
			remoteCenterServerPort = Config.Replica3.UK_PORT;
		} else if (remoteCenterServerName.equals("US")) {
			remoteCenterServerPort = Config.Replica3.US_PORT;
		}

		Record recordToTransfer = null;
		try {
			recordToTransfer = records.getRecord(recordID);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return -1;
		}

		boolean newServerContainsRecordResponse;

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];

			Object[] arguments = {recordID};

			JSONObject jsonSendObject = new JSONObject();
			jsonSendObject.put(MessageKeys.COMMAND_TYPE, Config.RECORD_EXISTS);
			jsonSendObject.put(MessageKeys.RECORD_ID, recordID);
			sendData = jsonSendObject.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, remoteCenterServerPort);
			clientSocket.send(sendPacket);
			clientSocket.setSoTimeout(2000);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			JSONObject jsonReceiveObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());
			newServerContainsRecordResponse = Boolean.parseBoolean((String) jsonReceiveObject.get(MessageKeys.RECORD_EXISTS));
			this.logger.log(String.format("recordIDExists from %s: %s", remoteCenterServerName, newServerContainsRecordResponse));
			clientSocket.close();
		} catch (ParseException e) {
			e.printStackTrace();
			return -6;
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return -2;
		} catch (IOException e) {
			e.printStackTrace();
			return -3;
		}

		if (newServerContainsRecordResponse == true) {
			return -4;
		} else if (newServerContainsRecordResponse == false) {
			// ok
		} else {
			return -5;
		}

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];

			JSONObject jsonSendObject = recordToTransfer.getJSONObject();
			sendData = jsonSendObject.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, remoteCenterServerPort);
			clientSocket.send(sendPacket);
			clientSocket.setSoTimeout(2000);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			JSONObject jsonReceiveObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());
			newServerContainsRecordResponse = Boolean.parseBoolean((String) jsonReceiveObject.get(MessageKeys.RECORD_EXISTS));
			this.logger.log(String.format("transferRecord from %s: %s", remoteCenterServerName, newServerContainsRecordResponse));
			clientSocket.close();
		} catch (ParseException e) {
			e.printStackTrace();
			return -6;
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return -2;
		} catch (IOException e) {
			e.printStackTrace();
			return -3;
		}

		records.removeRecord(recordID);

		return 0;
	}

	@Override
	public void run() {
		udpServerThread.start();
	}

}
