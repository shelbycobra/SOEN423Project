package Replicas.Replica3;

import org.json.simple.JSONObject;

public class EmployeeRecord extends Record {
	private static final long serialVersionUID = 4220125172871733314L;

	private String projectID;

	public EmployeeRecord(String firstName, String lastName, int employeeID, String mailID, String projectID) {
		super("ER", firstName, lastName, employeeID, mailID);
		this.projectID = projectID;
	}

	public EmployeeRecord(JSONObject jsonObject) {
		super(jsonObject);
		this.projectID = (String) jsonObject.get(DEMS.MessageKeys.PROJECT_ID);
	}

	public String getProjectID() {
		return projectID;
	}

	public void setProjectID(String projectID) {
		this.projectID = projectID;
	}

	@Override
	public String toString() {
		return "EmployeeRecord \n\trecordID=" + getRecordID() + "\n\tfirstName=" + getFirstName() + "\n\tlastName=" + getLastName()
		+ "\n\temployeeID=" + getEmployeeID() + "\n\tmailID=" + getMailID() + "\n\tprojectID=" + projectID;
	}

	@Override
	public JSONObject getJSONObject() {
		JSONObject jsonObject = super.getJSONObject();

		jsonObject.put(DEMS.MessageKeys.PROJECT_ID, projectID);

		return jsonObject;
	}

}
