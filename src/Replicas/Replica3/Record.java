package Replicas.Replica3;

import java.io.Serializable;

import org.json.simple.JSONObject;

public abstract class Record implements Serializable {
	private static final long serialVersionUID = -4371452764387799673L;

	public String recordID;

	public String firstName;
	public String lastName;
	public int employeeID;
	public String mailID;

	Record(String recordID, String firstName, String lastName, int employeeID, String mailID) {
		this.recordID = recordID;

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

	public JSONObject getJSONObject() {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put(DEMS.MessageKeys.RECORD_ID, recordID);
		jsonObject.put(DEMS.MessageKeys.FIRST_NAME, firstName);
		jsonObject.put(DEMS.MessageKeys.LAST_NAME, lastName);
		jsonObject.put(DEMS.MessageKeys.EMPLOYEE_ID, employeeID);
		jsonObject.put(DEMS.MessageKeys.MAIL_ID, mailID);

		return jsonObject;
	}
}
