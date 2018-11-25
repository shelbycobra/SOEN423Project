package Replicas.Replica3;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ManagerRecord extends Record {
	private static final long serialVersionUID = -7309796150452080853L;

	private Projects projects;
	private String location;

	public ManagerRecord(String firstName, String lastName, int employeeID, String mailID, Projects projects, String location) {
		super("MR", firstName, lastName, employeeID, mailID);
		this.projects = projects;
		this.location = location;
	}

	public ManagerRecord(JSONObject jsonObject) {
		super(jsonObject);
		this.projects = new Projects((JSONArray) jsonObject.get(DEMS.MessageKeys.PROJECTS));
		this.location = (String) jsonObject.get(DEMS.MessageKeys.LOCATION);
	}

	public Projects getProjects() {
		return projects;
	}

	public String getLocation() {
		return location;
	}

	@Override
	public String toString() {
		return "ManagerRecord [recordID=" + getRecordID() + ", firstName=" + getFirstName() + ", lastName=" + getLastName()
		+ ", employeeID=" + getEmployeeID() + ", mailID=" + getMailID() + ", location=" + location + "]";
	}

	@Override
	public JSONObject getJSONObject() {
		JSONObject jsonObject = super.getJSONObject();
		jsonObject.put(DEMS.MessageKeys.PROJECTS, projects.getJSONArray());
		jsonObject.put(DEMS.MessageKeys.LOCATION, location);
		return jsonObject;
	}

}
