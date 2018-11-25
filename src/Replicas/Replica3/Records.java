package Replicas.Replica3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import DEMS.MessageKeys;

public class Records {

	private HashMap<Character, List<Record>> records;
	private int recordCounter = 0;

	public Records() {
		this.records = new HashMap<Character, List<Record>>();
	}

	public Records(JSONArray jsonArray) {
		for (int i = 0; i < jsonArray.size(); i++){
			JSONObject jsonObject = (JSONObject) jsonArray.get(i);
			String recordID = (String) jsonObject.get(MessageKeys.RECORD_ID);
			String recordType = recordID.substring(0, 2).toLowerCase();

			Record record = null;
			if (recordType.equals("er")) {
				record = new EmployeeRecord(jsonObject);
			} else if (recordType.equals("mr")) {
				record = new ManagerRecord(jsonObject);
			}

			addRecord(record);
		}
	}

	public JSONArray getJSONArray(String serverLocation) {
		JSONArray jsonArray = new JSONArray();

		for (char key : records.keySet()) {
			for (Record record : records.get(key)) {
				JSONObject jsonObject = record.getJSONObject();
				jsonObject.put(MessageKeys.SERVER_LOCATION, serverLocation);
				jsonObject.put(MessageKeys.RECORD_ID, record.getRecordID());
				jsonArray.add(jsonObject);
			}
		}

		return jsonArray;
	}

	public int getRecordCount() {
		int count = 0;
		for (char key : records.keySet()) {
			count += records.get(key).size();
		}
		return count;
	}

	public boolean recordExists(String recordID) {
		boolean recordExists = false;
		for (char key : records.keySet()) {
			for (Record value : records.get(key)) {
				if (value.getRecordID().equals(recordID)) {
					recordExists = true;
					break;
				}
			}
		}
		return recordExists;
	}

	public void addRecord(Record record) {
		record.setRecordIDNumber(recordCounter++);
		char letter = record.getLastName().toLowerCase().charAt(0);
		List<Record> recordList = new ArrayList<Record>();
		recordList = records.computeIfAbsent(letter, k -> new ArrayList<Record>());
		recordList.add(record);
	}

	public void addRecord(JSONObject jsonObject) {
		String recordID = (String) jsonObject.get(MessageKeys.RECORD_ID);
		String firstName = (String) jsonObject.get(MessageKeys.FIRST_NAME);
		String lastName = (String) jsonObject.get(MessageKeys.LAST_NAME);
		int employeeID = Integer.parseInt((String) jsonObject.get(MessageKeys.EMPLOYEE_ID));
		String mailID = (String) jsonObject.get(MessageKeys.MAIL_ID);

		Record record = null;
		if (recordID.substring(0, 2).toLowerCase().equals("er")) {
			String projectID = (String) jsonObject.get(MessageKeys.PROJECT_ID);
			record = new EmployeeRecord(firstName, lastName, employeeID, mailID, projectID);
		} else if (recordID.substring(0, 2).toLowerCase().equals("mr")) {
			String location = (String) jsonObject.get(MessageKeys.LOCATION);
			Projects projects = new Projects((JSONArray) jsonObject.get(MessageKeys.PROJECTS));
			record = new ManagerRecord(firstName, lastName, employeeID, mailID, projects, location);
		}

		addRecord(record);
	}

	public void editRecord(String recordID, String fieldName, String newValue) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		Record record = getRecord(recordID);

		if (fieldName.equals(MessageKeys.FIRST_NAME)) {
			record.setFirstName(newValue);
		} else if (fieldName.equals(MessageKeys.LAST_NAME)) {
			record.setLastName(newValue);
		} else if (fieldName.equals(MessageKeys.EMPLOYEE_ID)) {
			record.setEmployeeID(Integer.parseInt(newValue));
		} else if (fieldName.equals(MessageKeys.MAIL_ID)) {
			record.setMailID(newValue);
		}

		if (recordID.substring(0, 2).toLowerCase().equals("er")) {
			EmployeeRecord recordER = (EmployeeRecord) getRecord(recordID);
			if (fieldName.equals(MessageKeys.PROJECT_ID)) {
				recordER.setProjectID(newValue);
			}
		} else if (recordID.substring(0, 2).toLowerCase().equals("mr")) {
			ManagerRecord recordMR = (ManagerRecord) getRecord(recordID);
			if (fieldName.equals(MessageKeys.LOCATION)) {
				recordMR.setLocation(newValue);
			} else if (fieldName.equals(MessageKeys.PROJECTS)) {
				// recordMR.setProjects(newValue);
			}
		}

		throw new IllegalArgumentException("recordID not found: " + recordID);
	}

	public void printRecords(Logger logger) {
		for (char key : records.keySet()) {
			logger.log(String.format("  %c:", key));
			for (Record value : records.get(key)) {
				logger.log("    " + value.toString());
				if (value instanceof ManagerRecord) {
					logger.log("      projects:");
					for (Project project : ((ManagerRecord) value).getProjects()) {
						logger.log("        " + project.toString());
					}
				}
			}
		}
	}

	public Record getRecord(String recordID) throws IllegalArgumentException {
		for (char key : records.keySet()) {
			for (Record value : records.get(key)) {
				if (value.getRecordID().equals(recordID)) {
					return value;
				}
			}
		}
		throw new IllegalArgumentException("recordID not found: " + recordID);
	}

	public void removeRecord(String recordID) {
		for (char key : records.keySet()) {
			for (Iterator<Record> iter = records.get(key).listIterator(); iter.hasNext();) {
				Record record = iter.next();
				if (record.getRecordID().equals(recordID)) {
					iter.remove();
				}
			}
		}
	}

}
