package Replicas.Replica3;

import java.io.Serializable;
import java.util.Random;

import org.json.simple.JSONObject;

public abstract class Record implements Serializable {
	private static final long serialVersionUID = -4371452764387799673L;

	private String recordID;

	private String firstName;
	private String lastName;
	private int employeeID;
	private String mailID;

	public Record(String recordIDPrefix, String firstName, String lastName, int employeeID, String mailID) {
		this.recordID = recordIDPrefix + randomInt(10000, 99999);

		this.firstName = firstName;
		this.lastName = lastName;
		this.employeeID = employeeID;
		this.mailID = mailID;
	}

	public Record(JSONObject jsonObject) {
		this.recordID = (String) jsonObject.get(DEMS.MessageKeys.RECORD_ID);
		this.firstName = (String) jsonObject.get(DEMS.MessageKeys.FIRST_NAME);
		this.lastName = (String) jsonObject.get(DEMS.MessageKeys.LAST_NAME);
		this.employeeID = Integer.parseInt((String) jsonObject.get(DEMS.MessageKeys.EMPLOYEE_ID));
		this.mailID = (String) jsonObject.get(DEMS.MessageKeys.MAIL_ID);
	}

	public String getRecordID() {
		return recordID;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public int getEmployeeID() {
		return employeeID;
	}

	public String getMailID() {
		return mailID;
	}

	public JSONObject getJSONObject() {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put(DEMS.MessageKeys.RECORD_ID, recordID);
		jsonObject.put(DEMS.MessageKeys.FIRST_NAME, firstName);
		jsonObject.put(DEMS.MessageKeys.LAST_NAME, lastName);
		jsonObject.put(DEMS.MessageKeys.EMPLOYEE_ID, employeeID);
		jsonObject.put(DEMS.MessageKeys.MAIL_ID, mailID);

		return jsonObject;
	}

	public int randomInt(int min, int max) {
		Random random = new Random();
		return random.nextInt((max - min) + 1) + min;
	}
}
