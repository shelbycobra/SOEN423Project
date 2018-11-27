package Replicas.Replica3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.xml.internal.ws.resources.SenderMessages;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import DEMS.Config;
import DEMS.MessageKeys;

public class CenterServer {

	private String location;
	private UdpServer udpServer;
	private Logger logger;
	private JSONParser jsonParser = new JSONParser();
	private String sendMessage;
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

	class InternalMessage {
		public static final String getRecordCountInternal = "getRecordCountInternal";
		public static final String transferRecordInternal = "transferRecordInternal";
		public static final String recordExistsInternal = "recordExistsInternal";
	}

	AtomicBoolean runThreadsAtomicBoolean = new AtomicBoolean(true);

	class UdpServer extends Thread {

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
				serverSocket.setSoTimeout(1000);

				byte[] receiveData = new byte[1024];
				byte[] sendData = new byte[1024];
				while (true) {
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					logger.log("udp waiting for connection on port: " + localPort);

					while (true) {
						try {
							serverSocket.receive(receivePacket);
							break;
						} catch(SocketTimeoutException e) {
							if (runThreadsAtomicBoolean.get()) {
								continue;
							} else {
								serverSocket.close();
								return;
							}
						}
					}

					JSONObject jsonReceiveObject;
					try {
						String str = new String(receivePacket.getData()).trim();
						System.out.println(str);
						jsonReceiveObject = (JSONObject) jsonParser.parse(str);
						receiveData = new byte[1024];
					} catch (ParseException e) {
						e.printStackTrace();
						continue;
					}

					JSONObject jsonSendObject = new JSONObject();
					jsonSendObject.put(MessageKeys.MESSAGE_ID, jsonReceiveObject.get(MessageKeys.MESSAGE_ID));
					jsonSendObject.put(MessageKeys.RM_PORT_NUMBER, Integer.toString(Config.Replica3.RM_PORT));
					//jsonSendObject.put(MessageKeys.REPLICA_NUMBER, "3");

					String commandType = jsonReceiveObject.get(MessageKeys.COMMAND_TYPE).toString();
					String managerID = (String) jsonReceiveObject.get(MessageKeys.MANAGER_ID);

					logger.log(String.format("udp message: managerID: %s, commandType: %s", managerID, commandType));
					boolean internalMessage = false;

					if (commandType.equals(InternalMessage.getRecordCountInternal)) {
						internalMessage = true;
						jsonSendObject.put(MessageKeys.MESSAGE, Integer.toString(records.getRecordCount()));
						jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
					} else if (commandType.equals(InternalMessage.transferRecordInternal)) {
						internalMessage = true;
						records.addRecord(jsonReceiveObject);
						jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
					} else if (commandType.equals(InternalMessage.recordExistsInternal)) {
						internalMessage = true;
						sendMessage += "Record Exists in "+location+" Database.\n";
						String recordID = (String) jsonReceiveObject.get(MessageKeys.RECORD_ID);
						jsonSendObject.put(MessageKeys.MESSAGE, Boolean.toString(records.recordExists(recordID)));
						jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
					} else if (commandType.equals(Config.GET_RECORD_COUNT)) {
						sendMessage += "Get Record Count: \n";
						String recordCount = getRecordCounts(managerID);
						jsonSendObject.put(MessageKeys.MESSAGE, recordCount);
						jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
					} else if (commandType.equals(Config.TRANSFER_RECORD)) {
						sendMessage += "Transfer Record: \n";
						String recordID = (String) jsonReceiveObject.get(MessageKeys.RECORD_ID);
						String remoteCenterServerName = (String) jsonReceiveObject.get(MessageKeys.REMOTE_SERVER_NAME);
						try {
							String recordString = records.getRecord(recordID).toString();
							String message = transferRecord(managerID, recordID, remoteCenterServerName);
							message = transferRecord(managerID, recordID, remoteCenterServerName);
							if (message.equals("ok")) {
								sendMessage += "Transfer Success \n";
								jsonSendObject.put(MessageKeys.MESSAGE, recordString);
								jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
							} else {
								sendMessage += "Transfer Failure \n";
								jsonSendObject.put(MessageKeys.MESSAGE, message);
								jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.FAIL.toString());
							}
						} catch (IllegalArgumentException e) {
							jsonSendObject.put(MessageKeys.MESSAGE, "could not find record ID "+recordID+" in "+location+" hashmap");
							jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
						}
					} else if (commandType.equals(Config.CREATE_MANAGER_RECORD)) {
						sendMessage += location + " Server Creating Manager Record: ";
						String firstName = (String) jsonReceiveObject.get(MessageKeys.FIRST_NAME);
						String lastName = (String) jsonReceiveObject.get(MessageKeys.LAST_NAME);
						int employeeID = Integer.parseInt((String) jsonReceiveObject.get(MessageKeys.EMPLOYEE_ID));
						String mailID = (String) jsonReceiveObject.get(MessageKeys.MAIL_ID);
						Projects projects = new Projects((JSONArray) jsonReceiveObject.get(DEMS.MessageKeys.PROJECTS));
						String location = (String) jsonReceiveObject.get(DEMS.MessageKeys.LOCATION);
						String recordID = createMRecord(managerID, firstName, lastName, employeeID, mailID, projects, location);
						jsonSendObject.put(MessageKeys.MESSAGE, records.getRecord(recordID).toString());
						jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
					} else if (commandType.equals(Config.CREATE_EMPLOYEE_RECORD)) {
						sendMessage += location + " Server Creating Employee Record: ";
						String firstName = (String) jsonReceiveObject.get(MessageKeys.FIRST_NAME);
						String lastName = (String) jsonReceiveObject.get(MessageKeys.LAST_NAME);
						int employeeID = Integer.parseInt((String) jsonReceiveObject.get(MessageKeys.EMPLOYEE_ID));
						String mailID = (String) jsonReceiveObject.get(MessageKeys.MAIL_ID);
						String projectID = (String) jsonReceiveObject.get(DEMS.MessageKeys.PROJECT_ID);
						String recordID = createERecord(managerID, firstName, lastName, employeeID, mailID, projectID);
						jsonSendObject.put(MessageKeys.MESSAGE, records.getRecord(recordID).toString());
						jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
					} else if (commandType.equals(Config.EDIT_RECORD)) {
						sendMessage += "Editing a record ";
						String recordID = (String) jsonReceiveObject.get(MessageKeys.RECORD_ID);
						String fieldName = (String) jsonReceiveObject.get(MessageKeys.FIELD_NAME);
						String newValue = (String) jsonReceiveObject.get(MessageKeys.NEW_VALUE);
						try {
							editRecord(managerID, recordID, fieldName, newValue);
							jsonSendObject.put(MessageKeys.MESSAGE, records.getRecord(recordID).toString());
							jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
						} catch (IllegalArgumentException e) {
							jsonSendObject.put(MessageKeys.MESSAGE, "could not find record ID "+recordID+" in "+location+" hashmap");
							jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.SUCCESS.toString());
						}
					} else {
						sendMessage += location + " Server -  Unknown Command Type ";
						jsonSendObject.put(MessageKeys.MESSAGE, "unknown command type");
						jsonSendObject.put(MessageKeys.STATUS_CODE, Config.StatusCode.FAIL.toString());
					}

					logger.log("udp command response: " + jsonSendObject);
					InetAddress IPAddress = receivePacket.getAddress();
					int port = receivePacket.getPort();

					// jsonSendObject.put(MessageKeys.MESSAGE, sendMessage);
					sendData = jsonSendObject.toString().getBytes();

					if (internalMessage) {
						DatagramPacket sendPacket1 = new DatagramPacket(sendData, sendData.length, IPAddress, port);
						serverSocket.send(sendPacket1);
					} else {
						InetAddress frontEndHost = InetAddress.getByName(Config.IPAddresses.FRONT_END);
						DatagramPacket sendPacket2 = new DatagramPacket(sendData, sendData.length, frontEndHost, Config.PortNumbers.RE_FE);
						serverSocket.send(sendPacket2);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public CenterServer(String location) {
		this.location = location.toUpperCase();
		this.logger = new Logger(location);

		udpServer = new UdpServer();
		udpServer.start();
	}

	public void stopServer() {
		runThreadsAtomicBoolean.set(false);
		try {
			udpServer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized String createMRecord(String managerID, String firstName, String lastName, int employeeID, String mailID, Projects projects, String location) {
		this.logger.log(String.format("createMRecord(%s, %s, %s, %d, %s, %s, %s)", managerID, firstName, lastName, employeeID, mailID, projects.toString(), location));

		ManagerRecord record = new ManagerRecord(firstName, lastName, employeeID, mailID, projects, location);
		records.addRecord(record);
		sendMessage += "Record created: " + record.getRecordID();
		sendMessage += "\n\tFull Name: " + firstName + " " + lastName + "\n\tEmployee ID: " + employeeID + "\n\tMail ID: " + mailID + "\n\tProjects:";

		List<Project> projs = projects.getProjects();
		for (Project p : projs) {
			sendMessage += "\n\t\tProject ID: " + p.getID();
			sendMessage += "\n\t\tProject Client: " + p.getClientName();
			sendMessage += "\n\t\tProject Name: " + p.getProjectName();
		}

		return record.getRecordID();
	}

	public synchronized String createERecord(String managerID, String firstName, String lastName, int employeeID, String mailID, String projectID) {
		this.logger.log(String.format("createERecord(%s, %s, %s, %d, %s, %s)", managerID, firstName, lastName, employeeID, mailID, projectID));

		EmployeeRecord record = new EmployeeRecord(firstName, lastName, employeeID, mailID, projectID);
		records.addRecord(record);
		sendMessage += "Record created: ";
		sendMessage += record.toString();
		return record.getRecordID();
	}

	public synchronized String getRecordCounts(String managerID) {
		this.logger.log(String.format("getRecordCounts(%s)", managerID));

		String result = String.format("%s: %d, ", this.location, records.getRecordCount());
		String otherLocations[] = new String[0];
		if (this.location.equals("CA")) {
			otherLocations = new String[]{"UK", "US"};
		} else if (this.location.equals("UK")) {
			otherLocations = new String[]{"CA", "US"};
		} else if (this.location.equals("US")) {
			otherLocations = new String[]{"CA", "UK"};
		}

		for (String location : otherLocations) {
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
				jsonSendObject.put(MessageKeys.COMMAND_TYPE, InternalMessage.getRecordCountInternal);
				sendData = jsonSendObject.toString().getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				clientSocket.send(sendPacket);
				clientSocket.setSoTimeout(2000);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				clientSocket.receive(receivePacket);
				JSONObject jsonReceiveObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());
				response = (String) jsonReceiveObject.get(MessageKeys.MESSAGE);
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

		sendMessage += result;
		return result;
	}

	public synchronized int editRecord(String managerID, String recordID, String fieldName, String newValue) {
		this.logger.log(String.format("editRecord(%s, %s, %s, %s)", managerID, recordID, fieldName, newValue));
		Record record;
		try {
			record = records.editRecord(recordID, fieldName, newValue);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return -2;
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
			return -1;
		}

		sendMessage += record.toString();

		if (record == null)
			return -1;

		return 0;
	}

	public int printData(String managerID) {
		this.logger.log(String.format("printData(%s)", managerID));

		records.printRecords(this.logger);

		return 0;
	}

	public synchronized String transferRecord(String managerID, String recordID, String remoteCenterServerName) throws IllegalArgumentException {
		this.logger.log(String.format("transferRecord(%s, %s, %s)", managerID, recordID, remoteCenterServerName));

		int remoteCenterServerPort = 0;
		if (remoteCenterServerName.equals("CA")) {
			remoteCenterServerPort = Config.Replica3.CA_PORT;
		} else if (remoteCenterServerName.equals("UK")) {
			remoteCenterServerPort = Config.Replica3.UK_PORT;
		} else if (remoteCenterServerName.equals("US")) {
			remoteCenterServerPort = Config.Replica3.US_PORT;
		}

		Record recordToTransfer = records.getRecord(recordID);

		boolean newServerContainsRecordResponse;

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];

			Object[] arguments = {recordID};

			JSONObject jsonSendObject = new JSONObject();
			jsonSendObject.put(MessageKeys.COMMAND_TYPE, InternalMessage.recordExistsInternal);
			jsonSendObject.put(MessageKeys.RECORD_ID, recordID);
			sendData = jsonSendObject.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, remoteCenterServerPort);
			clientSocket.send(sendPacket);
			clientSocket.setSoTimeout(2000);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			JSONObject jsonReceiveObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());
			newServerContainsRecordResponse = Boolean.parseBoolean((String) jsonReceiveObject.get(MessageKeys.MESSAGE));
			this.logger.log(String.format("recordIDExists from %s: %s", remoteCenterServerName, newServerContainsRecordResponse));
			clientSocket.close();
		} catch (ParseException e) {
			e.printStackTrace();
			return "ParseException";
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return "SocketTimeoutException";
		} catch (IOException e) {
			e.printStackTrace();
			return "IOException";
		}

		if (newServerContainsRecordResponse == true) {
			return "new server already contains record";
		} else if (newServerContainsRecordResponse == false) {
			// ok
		} else {
			return "unknown message from new server";
		}

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];

			JSONObject jsonSendObject = recordToTransfer.getJSONObject();
			jsonSendObject.put(MessageKeys.COMMAND_TYPE, InternalMessage.transferRecordInternal);
			sendData = jsonSendObject.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, remoteCenterServerPort);
			clientSocket.send(sendPacket);
			clientSocket.setSoTimeout(2000);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			JSONObject jsonReceiveObject = (JSONObject) jsonParser.parse(new String(receivePacket.getData()).trim());
			this.logger.log(String.format("transferRecord from %s: %s", remoteCenterServerName, newServerContainsRecordResponse));
			clientSocket.close();
		} catch (ParseException e) {
			e.printStackTrace();
			return "ParseException";
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return "SocketTimeoutException";
		} catch (IOException e) {
			e.printStackTrace();
			return "SocketTimeoutException";
		}

		records.removeRecord(recordID);

		return "ok";
	}

}
