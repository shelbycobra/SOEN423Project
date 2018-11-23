package Replicas.Replica3;

import DEMS.Config;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CenterServer implements Runnable {

	private String location;
	private Thread udpServerThread;
	private Logger logger;

	static HashMap<Character, List<Record>> records = new HashMap<Character, List<Record>>();

	public static final HashMap<String, Integer> UDPPortMap;
	static {
		UDPPortMap = new HashMap<String, Integer>();
		UDPPortMap.put("CA", Config.Replica3.CA_PORT);
		UDPPortMap.put("US", Config.Replica3.US_PORT);
		UDPPortMap.put("UK", Config.Replica3.UK_PORT);
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

					byte[] b = receivePacket.getData();
					ByteArrayInputStream bis = new ByteArrayInputStream(b);
					ObjectInput in = new ObjectInputStream(bis);
					MethodCallMessage methodCallMessage = (MethodCallMessage) in.readObject();

					logger.log("udp method call received: " + methodCallMessage.getMethodName());

					String response = "";
					if (methodCallMessage.getMethodName().equals("getRecordCounts")) {
						int count = 0;
						for (char key : records.keySet()) {
							count += records.get(key).size();
						}
						response = Integer.toString(count);
					} else if (methodCallMessage.getMethodName().startsWith("recordExists")) {
						response = "recordExists";
						boolean recordExists = false;
						for (char key : records.keySet()) {
							for (Record value : records.get(key)) {
								if (value.recordID.equals(methodCallMessage.getArguments()[0])) {
									recordExists = true;
									break;
								}
							}
						}
						response = Boolean.toString(recordExists);
					} else if (methodCallMessage.getMethodName().startsWith("transferRecord")) {
						Record newRecord = (Record)methodCallMessage.getArguments()[0];
						char letter = newRecord.lastName.toLowerCase().charAt(0);
						List<Record> recordList = records.computeIfAbsent(letter, k -> new ArrayList<Record>());
						recordList.add(newRecord);
						response = "ok";
					}

					logger.log("udp response: " + response);
					InetAddress IPAddress = receivePacket.getAddress();
					int port = receivePacket.getPort();
					sendData = response.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					serverSocket.send(sendPacket);
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public CenterServer(String location) {
		this.location = location;
		this.logger = new Logger(location);

		udpServerThread = new Thread(new UdpServer());
	}

	public synchronized int createMRecord(String managerID, String firstName, String lastName, int employeeID, String mailID, Project[] projects, String location) {
		this.logger.log(String.format("createMRecord(%s, %s, %s, %d, %s, %s, %s)", managerID, firstName, lastName, employeeID, mailID, projects.toString(), location));

		ArrayList<Project> projectsList = new ArrayList<Project>(Arrays.asList(projects));
		ManagerRecord record = new ManagerRecord(firstName, lastName, employeeID, mailID, projectsList, location);
		char letter = lastName.toLowerCase().charAt(0);
		List<Record> recordList = records.computeIfAbsent(letter, k -> new ArrayList<Record>());
		recordList.add(record);

		return 0;
	}

	public synchronized int createERecord(String managerID, String firstName, String lastName, int employeeID, String mailID, int projectID) {
		this.logger.log(String.format("createERecord(%s, %s, %s, %d, %s, %d)", managerID, firstName, lastName, employeeID, mailID, projectID));

		EmployeeRecord record = new EmployeeRecord(firstName, lastName, employeeID, mailID, projectID);
		char letter = lastName.toLowerCase().charAt(0);
		List<Record> recordList = records.computeIfAbsent(letter, k -> new ArrayList<Record>());
		recordList.add(record);

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

				MethodCallMessage methodCallMessage = new MethodCallMessage("getRecordCounts", null);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos);
				out.writeObject(methodCallMessage);
				sendData = bos.toByteArray();
				out.close();
				bos.close();

				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				clientSocket.send(sendPacket);
				clientSocket.setSoTimeout(2000);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				clientSocket.receive(receivePacket);
				response = (new String(receivePacket.getData())).trim();
				this.logger.log(String.format("record count from %s: %s", location, response));
				clientSocket.close();
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

		for (char key : records.keySet()) {
			for (Record value : records.get(key)) {
				if (value.recordID.equals(recordID)) {
					try {
						Class c = value.getClass();
						Field f = c.getField(fieldName);
						f.set(value, newValue);
						return 0;
					} catch (NoSuchFieldException | IllegalAccessException e) {
						e.printStackTrace();
						return -1;
					}
				}
			}
		}

		// no record was edited
		return -2;
	}

	public int printData(String managerID) {
		this.logger.log(String.format("printData(%s)", managerID));

		for (char key : records.keySet()) {
			this.logger.log(String.format("  %c:", key));
			for (Record value : records.get(key)) {
				this.logger.log("    " + value.toString());
				if (value instanceof ManagerRecord) {
					this.logger.log("      projects:");
					for (Project project : ((ManagerRecord) value).projects) {
						this.logger.log("        " + project.toString());
					}
				}
			}
		}

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

		for (char key : records.keySet()) {
			for (Record value : records.get(key)) {
				if (value.recordID.equals(recordID)) {
					recordToTransfer = value;
					break;
				}
			}
		}

		if (recordToTransfer == null) {
			return -1;
		}

		String newServerContainsRecordResponse;

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];

			Object[] arguments = {recordID};
			MethodCallMessage methodCallMessage = new MethodCallMessage("recordExists", arguments);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(methodCallMessage);
			sendData = bos.toByteArray();
			out.close();
			bos.close();

			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, remoteCenterServerPort);
			clientSocket.send(sendPacket);
			clientSocket.setSoTimeout(2000);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			newServerContainsRecordResponse = (new String(receivePacket.getData())).trim();
			this.logger.log(String.format("recordIDExists from %s: %s", remoteCenterServerName, newServerContainsRecordResponse));
			clientSocket.close();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return -2;
		} catch (IOException e) {
			e.printStackTrace();
			return -3;
		}

		if (newServerContainsRecordResponse.equals("true")) {
			return -4;
		} else if (newServerContainsRecordResponse.equals("false")) {
			// ok
		} else {
			return -5;
		}

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			byte[] sendData = new byte[1024];
			byte[] receiveData = new byte[1024];

			Object[] arguments = {recordToTransfer};
			MethodCallMessage methodCallMessage = new MethodCallMessage("transferRecord", arguments);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(methodCallMessage);
			sendData = bos.toByteArray();
			out.close();
			bos.close();

			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, remoteCenterServerPort);
			clientSocket.send(sendPacket);
			clientSocket.setSoTimeout(2000);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			newServerContainsRecordResponse = (new String(receivePacket.getData())).trim();
			this.logger.log(String.format("transferRecord from %s: %s", remoteCenterServerName, newServerContainsRecordResponse));
			clientSocket.close();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return -2;
		} catch (IOException e) {
			e.printStackTrace();
			return -3;
		}

		for (char key : records.keySet()) {
			for (Iterator<Record> iter = records.get(key).listIterator(); iter.hasNext();) {
				Record record = iter.next();
				if (record.recordID.equals(recordID)) {
					iter.remove();
				}
			}
		}

		return 0;
	}

	@Override
	public void run() {
		udpServerThread.start();
	}

}
