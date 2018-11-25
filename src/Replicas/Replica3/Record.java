package Replicas.Replica3;

import java.io.Serializable;

import org.json.simple.JSONObject;

public abstract class Record implements Serializable {
	private static final long serialVersionUID = -4371452764387799673L;

	private String recordID;
	private String recordIDPrefix;

	private String firstName;
	private String lastName;
	private int employeeID;
	private String mailID;

	public Record(String recordIDPrefix, String firstName, String lastName, int employeeID, String mailID) {
		this.recordIDPrefix = recordIDPrefix;

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

	public void setRecordID(String recordID) {
		this.recordID = recordID;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setEmployeeID(int employeeID) {
		this.employeeID = employeeID;
	}

	public void setMailID(String mailID) {
		this.mailID = mailID;
	}

	public void setRecordIDNumber(int recordIDNumber) {
		this.recordID = String.format("%s%05d", recordIDPrefix, recordIDNumber);
	}

	public JSONObject getJSONObject() {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put(DEMS.MessageKeys.RECORD_ID, recordID);
		jsonObject.put(DEMS.MessageKeys.FIRST_NAME, firstName);
		jsonObject.put(DEMS.MessageKeys.LAST_NAME, lastName);
		jsonObject.put(DEMS.MessageKeys.EMPLOYEE_ID, Integer.toString(employeeID));
		jsonObject.put(DEMS.MessageKeys.MAIL_ID, mailID);

		return jsonObject;
	}
}
