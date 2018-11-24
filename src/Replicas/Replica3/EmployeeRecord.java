package Replicas.Replica3;

import org.json.simple.JSONObject;

public class EmployeeRecord extends Record {
	private static final long serialVersionUID = 4220125172871733314L;

	public int projectID;

	public EmployeeRecord(String firstName, String lastName, int employeeID, String mailID, int projectID) {
		super("ER" + employeeID, firstName, lastName, employeeID, mailID);
		this.projectID = projectID;
	}

	public EmployeeRecord(JSONObject jsonObject) {
		super(jsonObject);
		this.projectID = Integer.parseInt((String) jsonObject.get(DEMS.MessageKeys.PROJECT_ID));
	}

	@Override
	public String toString() {
		return "EmployeeRecord [recordID=" + recordID + ", firstName=" + firstName + ", lastName=" + lastName
				+ ", employeeID=" + employeeID + ", mailID=" + mailID + ", projectID=" + projectID + "]";
	}

	@Override
	public JSONObject getJSONObject() {
		JSONObject jsonObject = super.getJSONObject();

		jsonObject.put(DEMS.MessageKeys.PROJECT_ID, projectID);

		return jsonObject;
	}

}
