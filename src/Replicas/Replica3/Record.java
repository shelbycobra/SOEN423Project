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
